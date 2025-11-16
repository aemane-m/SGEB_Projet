package application.domain.core.emprunt;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entité domaine représentant une pénalité monétaire liée à un Emprunt.
 * Stocke les ID des relations pour éviter les N+1.
 *
 * Invariants :
 * <ul>
 *   <li>montant &gt; 0</li>
 *   <li>{@code datePaiement == null} &harr; non payée</li>
 * </ul>
 */
public final class Penalite {

    private Long id;
    private final Money montant;
    private final LocalDate dateGeneration;
    private LocalDate datePaiement; // null = non payée

    // --- Relations découplées ---
    private final Long empruntId;
    private final Long adherentId;

    // Référence transiente vers l'emprunt (facultatif, pour l'UI ou certains services)
    private transient Emprunt emprunt;

    // Statut métier de la pénalité
    private PenaliteStatus status;

    /**
     * Constructeur principal pour la persistance (chargement DB).
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

        // Déduit un statut minimal cohérent avec la date de paiement
        if (datePaiement == null) {
            this.status = PenaliteStatus.DUE;
        } else {
            this.status = PenaliteStatus.PAID;
        }
    }

    /**
     * Factory : crée une pénalité non payée à la date donnée (logique métier).
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

        // Stocke l'objet source (transient) pour un accès immédiat
        p.setEmprunt(emprunt);
        return p;
    }

    /** @return {@code true} si la pénalité est payée. */
    public boolean estPayee() {
        return datePaiement != null && status == PenaliteStatus.PAID;
    }

    /**
     * Marque la pénalité comme "paiement demandé" (par l'adhérent).
     * Ne définit pas de date de paiement, uniquement un changement de statut.
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

    /** Marque la pénalité comme payée à la date fournie. */
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

    /** Annule la pénalité (quel que soit son état précédent). */
    public void annuler() {
        this.datePaiement = null;
        this.status = PenaliteStatus.CANCELLED;
    }

    // --- Getters / Setters techniques ---

    public Long getId() { return id; }
    public Money getMontant() { return montant; }
    public LocalDate getDateGeneration() { return dateGeneration; }
    public LocalDate getDatePaiement() { return datePaiement; }

    public Long getEmpruntId() { return empruntId; }
    public Long getAdherentId() { return adherentId; }

    public Emprunt getEmprunt() { return emprunt; }
    public void setEmprunt(Emprunt emprunt) { this.emprunt = emprunt; }

    public PenaliteStatus getStatus() { return status; }

    /** Définit l'identifiant technique (généré par la persistance). */
    public void setId(Long id) {
        if (id == null) return;
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

    /**
     * Renvoie la date de fin de la pénalité (date de paiement).
     * @return La date de paiement ou {@code null} si non payée.
     */
    public LocalDate getDateFin() {
        return this.datePaiement;
    }

 // Dans Penalite
    public void overrideStatusFromPersistence(PenaliteStatus newStatus) {
        if (newStatus == null) return;
        // On ne casse pas les invariants : une pénalité payée en base reste payée
        if (this.datePaiement != null && newStatus == PenaliteStatus.DUE) {
            // Incohérent : on ignore ce cas
            return;
        }
        this.status = newStatus;
    }
}