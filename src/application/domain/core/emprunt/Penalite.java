package application.domain.core.emprunt;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entité de domaine représentant une pénalité monétaire associée à un {@link Emprunt}.
 *
 * <p>Une pénalité correspond à un montant dû par un adhérent à cause d’un
 * retard et est liée à :</p>
 *
 * <ul>
 *     <li>un emprunt ({@code empruntId})</li>
 *     <li>un adhérent ({@code adherentId})</li>
 * </ul>
 *
 * <p>
 * Cette classe est conçue comme une entité métier :
 * elle possède un identifiant technique {@link #id} et un petit cycle de vie
 * (due, demandée, payée, annulée) piloté par {@link PenaliteStatus}.
 * </p>
 *
 * <h3>Invariants</h3>
 * <ul>
 *     <li>{@code montant} doit être strictement positif ({@link Money#isPositive()}).</li>
 *     <li>{@code dateGeneration} est toujours non nulle.</li>
 *     <li>{@code datePaiement == null} ⇔ pénalité non payée.</li>
 *     <li>Si {@code datePaiement} est définie, elle ne peut pas être avant {@code dateGeneration}.</li>
 * </ul>
 *
 * <h3>Statut métier</h3>
 * <ul>
 *     <li>{@link PenaliteStatus#DUE} : due (non payée).</li>
 *     <li>{@link PenaliteStatus#PAYMENT_REQUESTED} : paiement demandé.</li>
 *     <li>{@link PenaliteStatus#PAID} : payée.</li>
 *     <li>{@link PenaliteStatus#CANCELLED} : annulée.</li>
 * </ul>
 */
public final class Penalite {

    // ======================================================================
    // ================================= CHAMPS ==============================
    // ======================================================================

    /** Identifiant technique (généré par la base de données). */
    private Long id;

    /** Montant de la pénalité (toujours strictement positif). */
    private final Money montant;

    /** Date à laquelle la pénalité a été générée. */
    private final LocalDate dateGeneration;

    /**
     * Date de paiement effectif.
     * <p>Si null, la pénalité est considérée comme non payée.</p>
     */
    private LocalDate datePaiement;

    // ----------------------------------------------------------------------
    // Relations (découplées pour limiter les problèmes de N+1)
    // ----------------------------------------------------------------------

    /** Identifiant de l'emprunt concerné. */
    private final Long empruntId;

    /** Identifiant de l'adhérent débiteur. */
    private final Long adherentId;

    /**
     * Référence transiente vers l'emprunt complet (non persistée).
     * <p>
     * Utilisée éventuellement par l'UI ou certains services lorsque
     * l'objet complet est disponible.
     * </p>
     */
    private transient Emprunt emprunt;

    /** Statut métier de la pénalité. */
    private PenaliteStatus status;

    // ======================================================================
    // ============================ CONSTRUCTEUR =============================
    // ======================================================================

    /**
     * Constructeur principal utilisé pour le chargement depuis la base
     * de données (persistance).
     *
     * @param id identifiant technique (peut être null pour une nouvelle pénalité)
     * @param empruntId identifiant de l'emprunt lié
     * @param adherentId identifiant de l'adhérent concerné
     * @param montant montant de la pénalité (doit être > 0)
     * @param dateGeneration date de génération de la pénalité
     * @param datePaiement date de paiement (ou null si non payée)
     * @throws IllegalArgumentException si les invariants ne sont pas respectés
     */
    public Penalite(Long id,
                    Long empruntId,
                    Long adherentId,
                    Money montant,
                    LocalDate dateGeneration,
                    LocalDate datePaiement) {

        this.id = id;
        this.empruntId = Objects.requireNonNull(empruntId, "empruntId");
        this.adherentId = Objects.requireNonNull(adherentId, "adherentId");
        this.montant = Objects.requireNonNull(montant, "montant");

        // Invariant : montant > 0
        if (!montant.isPositive()) {
            throw new IllegalArgumentException("Le montant de la pénalité doit être > 0");
        }

        this.dateGeneration = Objects.requireNonNull(dateGeneration, "dateGeneration");

        if (datePaiement != null && datePaiement.isBefore(dateGeneration)) {
            throw new IllegalArgumentException("La date de paiement ne peut pas être avant la génération");
        }
        this.datePaiement = datePaiement;

        // Statut cohérent avec la date de paiement
        if (datePaiement == null) {
            this.status = PenaliteStatus.DUE;
        } else {
            this.status = PenaliteStatus.PAID;
        }
    }

    // ======================================================================
    // =============================== FACTORY ===============================
    // ======================================================================

    /**
     * Factory métier : crée une pénalité non payée à la date donnée
     * pour un {@link Emprunt} donné.
     *
     * <p>
     * Les identifiants d'emprunt et d'adhérent sont extraits de l'entité
     * {@link Emprunt} pour éviter de charger des graphes d'objets trop lourds
     * (pattern "ID reference").
     * </p>
     *
     * @param emprunt emprunt en retard
     * @param montant montant de la pénalité (strictement positif)
     * @param dateGeneration date de génération de la pénalité
     * @return une nouvelle instance de {@link Penalite} en statut {@link PenaliteStatus#DUE}
     */
    public static Penalite creerPour(Emprunt emprunt, Money montant, LocalDate dateGeneration) {
        Objects.requireNonNull(emprunt, "emprunt");
        Objects.requireNonNull(emprunt.getId(), "emprunt.id");
        Objects.requireNonNull(emprunt.getEmprunteur(), "emprunt.emprunteur");
        Objects.requireNonNull(emprunt.getEmprunteur().getId(), "emprunt.emprunteur.id");
        Objects.requireNonNull(montant, "montant");
        Objects.requireNonNull(dateGeneration, "dateGeneration");

        Penalite p = new Penalite(
                null,
                emprunt.getId(),
                emprunt.getEmprunteur().getId(),
                montant,
                dateGeneration,
                null
        );

        // Statut par défaut pour une nouvelle pénalité : due
        p.status = PenaliteStatus.DUE;

        // Stocke l'objet source (transient) pour un accès immédiat éventuel
        p.setEmprunt(emprunt);
        return p;
    }

    // ======================================================================
    // =============================== MÉTIER ================================
    // ======================================================================

    /**
     * @return {@code true} si la pénalité est payée (date de paiement définie
     *         et statut {@link PenaliteStatus#PAID}).
     */
    public boolean estPayee() {
        return datePaiement != null && status == PenaliteStatus.PAID;
    }

    /**
     * Marque la pénalité comme "paiement demandé" (par l'adhérent par exemple).
     *
     * <p>
     * Cette méthode ne fixe pas de date de paiement : elle change uniquement
     * le {@link #status}. Si la pénalité est déjà payée ou annulée, un appel
     * déclenche une exception.
     * </p>
     *
     * <p>La méthode est idempotente : si le statut est déjà
     * {@link PenaliteStatus#PAYMENT_REQUESTED}, elle ne fait rien.</p>
     *
     * @throws IllegalStateException si la pénalité est déjà payée ou annulée
     */
    public void demanderPaiement() {
        if (status == PenaliteStatus.PAID) {
            throw new IllegalStateException("Impossible de demander un paiement pour une pénalité déjà payée.");
        }
        if (status == PenaliteStatus.CANCELLED) {
            throw new IllegalStateException("Impossible de demander un paiement pour une pénalité annulée.");
        }
        // Idempotence : si déjà demandée, on ne fait rien
        if (status == PenaliteStatus.PAYMENT_REQUESTED) {
            return;
        }
        this.status = PenaliteStatus.PAYMENT_REQUESTED;
    }

    /**
     * Marque la pénalité comme payée à la date fournie.
     *
     * @param date date de paiement
     * @throws IllegalArgumentException si la date est avant la date de génération
     * @throws IllegalStateException si la pénalité est annulée
     */
    public void marquerCommePayee(LocalDate date) {
        Objects.requireNonNull(date, "date");
        if (date.isBefore(dateGeneration)) {
            throw new IllegalArgumentException("La date de paiement ne peut pas être avant la génération");
        }
        if (status == PenaliteStatus.CANCELLED) {
            throw new IllegalStateException("Impossible de payer une pénalité annulée.");
        }
        this.datePaiement = date;
        this.status = PenaliteStatus.PAID;
    }

    /**
     * Annule la pénalité, quel que soit son état précédent.
     * <p>
     * La date de paiement est effacée, et le statut passe à
     * {@link PenaliteStatus#CANCELLED}.
     * </p>
     */
    public void annuler() {
        this.datePaiement = null;
        this.status = PenaliteStatus.CANCELLED;
    }

    /**
     * Permet de synchroniser le statut métier depuis la persistance, tout en
     * évitant de casser les invariants.
     *
     * <p>Exemple de règle : si la pénalité possède une date de paiement, on
     * refuse de la repasser à {@link PenaliteStatus#DUE}.</p>
     *
     * @param newStatus nouveau statut lu en base
     */
    public void overrideStatusFromPersistence(PenaliteStatus newStatus) {
        if (newStatus == null) {
            return;
        }
        // On ne casse pas les invariants : une pénalité payée en base reste payée
        if (this.datePaiement != null && newStatus == PenaliteStatus.DUE) {
            // Incohérent : on ignore ce cas
            return;
        }
        this.status = newStatus;
    }

    // ======================================================================
    // ============================== GETTERS ================================
    // ======================================================================

    public Long getId() { return id; }
    public Money getMontant() { return montant; }
    public LocalDate getDateGeneration() { return dateGeneration; }
    public LocalDate getDatePaiement() { return datePaiement; }

    public Long getEmpruntId() { return empruntId; }
    public Long getAdherentId() { return adherentId; }

    public Emprunt getEmprunt() { return emprunt; }
    public void setEmprunt(Emprunt emprunt) { this.emprunt = emprunt; }

    public PenaliteStatus getStatus() { return status; }

    /**
     * Renvoie la date de fin de la pénalité, c’est-à-dire la date de paiement.
     *
     * @return la date de paiement, ou {@code null} si la pénalité n'est pas payée
     */
    public LocalDate getDateFin() {
        return this.datePaiement;
    }

    // ======================================================================
    // ============================== IDENTIFIANT ============================
    // ======================================================================

    /**
     * Définit l'identifiant technique (généré par la persistance).
     *
     * <p>Règles :</p>
     * <ul>
     *     <li>Si {@code id} est null, l’appel est ignoré.</li>
     *     <li>L'identifiant doit être strictement positif.</li>
     *     <li>Une fois positionné, il ne peut plus être modifié.</li>
     * </ul>
     *
     * @param id identifiant à affecter
     */
    public void setId(Long id) {
        if (id == null) {
            return;
        }
        if (id <= 0) {
            throw new IllegalArgumentException("L'identifiant de la pénalité doit être > 0.");
        }
        if (this.id != null && !this.id.equals(id)) {
            throw new IllegalStateException(
                    "L'identifiant de la pénalité est déjà défini (" + this.id + "), modification interdite."
            );
        }
        this.id = id;
    }
}