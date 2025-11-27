package application.domain.core.emprunt;

import application.domain.core.document.Document;
import application.domain.core.personne.Adherent;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * Entité de domaine représentant un emprunt d'un {@link Document}
 * par un {@link Adherent} dans le système SGEB.
 *
 * <p>Cette classe encapsule la logique métier d'un emprunt :</p>
 * <ul>
 *     <li>Dates clés : emprunt, retour prévu, retour effectif</li>
 *     <li>Statut calculé (en cours, en retard, rendu... via {@link EmpruntStatus})</li>
 *     <li>Calcul de la pénalité courante (en {@link Money})</li>
 *     <li>Génération d'une {@link Penalite} si l'emprunt est en retard</li>
 * </ul>
 *
 * <h3>Règles métier principales</h3>
 * <ul>
 *     <li>Durée par défaut d'un emprunt : {@link #DUREE_JOURS_DEFAUT} jours (21).</li>
 *     <li>Pénalité par jour de retard : {@link #PENALITE_PAR_JOUR_DEFAUT} (0,50 €).</li>
 *     <li>Un emprunt est "en cours" tant qu'il n'a pas de date de retour effective.</li>
 *     <li>Un emprunt est "en retard" si la date de référence &gt; dateRetourPrevue.</li>
 * </ul>
 *
 * <p>La classe est <strong>final</strong> pour éviter son extension et garder les règles
 * métier contrôlées (pattern "Rich Domain Model").</p>
 */
public final class Emprunt {

    // ======================================================================
    // ============================== CONSTANTES =============================
    // ======================================================================

    /** Durée par défaut d'un emprunt en jours (21 jours). */
    public static final int DUREE_JOURS_DEFAUT = 21;

    /** Pénalité par jour de retard par défaut (0,50 €). */
    public static final Money PENALITE_PAR_JOUR_DEFAUT = Money.euros(0.50);

    // ======================================================================
    // ================================ CHAMPS ===============================
    // ======================================================================

    /** Identifiant technique (généré par la couche de persistance). */
    private Long id;

    /** Document emprunté. Ne peut pas être null. */
    private final Document document;

    /** Adhérent qui emprunte le document. Ne peut pas être null. */
    private final Adherent emprunteur;

    /** Date à laquelle l'emprunt a été effectué. */
    private final LocalDate dateEmprunt;

    /** Date prévue de retour (calculée ou fournie). */
    private final LocalDate dateRetourPrevue;

    /**
     * Date réelle de retour du document.
     * <p>
     * Si null, l'emprunt est considéré comme toujours "en cours".
     * </p>
     */
    private LocalDate dateRetourEffective;

    /**
     * Pénalité associée à cet emprunt, si un retard a été constaté et matérialisé.
     * <p>
     * Association optionnelle : il peut ne pas y avoir de pénalité
     * (pas de retard ou pas encore générée).
     * </p>
     */
    private Penalite penalite;

    // ======================================================================
    // =============================== FACTORIES =============================
    // ======================================================================

    /**
     * Factory standard pour créer un emprunt avec la durée par défaut
     * ({@link #DUREE_JOURS_DEFAUT} jours).
     *
     * @param doc document emprunté
     * @param adherent adhérent emprunteur
     * @param dateEmprunt date de l'emprunt
     * @return une nouvelle instance d'Emprunt
     */
    public static Emprunt creer(Document doc, Adherent adherent, LocalDate dateEmprunt) {
        Objects.requireNonNull(doc, "doc");
        Objects.requireNonNull(adherent, "adherent");
        Objects.requireNonNull(dateEmprunt, "dateEmprunt");

        return new Emprunt(
                null,
                doc,
                adherent,
                dateEmprunt,
                dateEmprunt.plusDays(DUREE_JOURS_DEFAUT),
                null,
                null
        );
    }

    /**
     * Factory permettant de créer un emprunt avec une durée personnalisée.
     *
     * @param doc document emprunté
     * @param adherent adhérent emprunteur
     * @param dateEmprunt date de l'emprunt
     * @param dureeJours durée souhaitée en jours (doit être >= 1)
     * @return une nouvelle instance d'Emprunt
     * @throws IllegalArgumentException si la durée est &lt; 1 jour
     */
    public static Emprunt creer(Document doc, Adherent adherent, LocalDate dateEmprunt, int dureeJours) {
        Objects.requireNonNull(doc, "doc");
        Objects.requireNonNull(adherent, "adherent");
        Objects.requireNonNull(dateEmprunt, "dateEmprunt");

        if (dureeJours < 1) {
            throw new IllegalArgumentException("La durée doit être >= 1 jour");
        }

        return new Emprunt(
                null,
                doc,
                adherent,
                dateEmprunt,
                dateEmprunt.plusDays(dureeJours),
                null,
                null
        );
    }

    /**
     * Factory utilisée lorsqu'on connaît déjà la date de retour prévue.
     * <p>
     * Généralement appelée par un service métier ou un repository.
     * </p>
     *
     * @param document document emprunté
     * @param adherent adhérent emprunteur
     * @param dateEmprunt date d'emprunt
     * @param dateRetourPrevue date prévue de retour
     * @return une nouvelle instance d'Emprunt
     */
    public static Emprunt creer(Document document,
                                Adherent adherent,
                                LocalDate dateEmprunt,
                                LocalDate dateRetourPrevue) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(adherent, "adherent");
        Objects.requireNonNull(dateEmprunt, "dateEmprunt");
        Objects.requireNonNull(dateRetourPrevue, "dateRetourPrevue");

        return new Emprunt(null, document, adherent, dateEmprunt, dateRetourPrevue, null, null);
    }

    // ======================================================================
    // ============================== CONSTRUCTEUR ===========================
    // ======================================================================

    /**
     * Constructeur principal de l'entité Emprunt.
     * <p>
     * Généralement appelé par les factories statiques ou par la couche repository
     * lors du rechargement depuis la base.
     * </p>
     *
     * @param id identifiant technique (peut être null pour un emprunt nouveau)
     * @param document document emprunté
     * @param emprunteur adhérent qui emprunte
     * @param dateEmprunt date à laquelle l'emprunt a été réalisé
     * @param dateRetourPrevue date prévue de retour
     * @param dateRetourEffective date réelle de retour (null si en cours)
     * @param penalite pénalité déjà associée (peut être null)
     * @throws IllegalArgumentException si les dates sont incohérentes
     */
    public Emprunt(Long id,
                   Document document,
                   Adherent emprunteur,
                   LocalDate dateEmprunt,
                   LocalDate dateRetourPrevue,
                   LocalDate dateRetourEffective,
                   Penalite penalite) {

        this.id = id;
        this.document = Objects.requireNonNull(document, "document");
        this.emprunteur = Objects.requireNonNull(emprunteur, "emprunteur");
        this.dateEmprunt = Objects.requireNonNull(dateEmprunt, "dateEmprunt");
        this.dateRetourPrevue = Objects.requireNonNull(dateRetourPrevue, "dateRetourPrevue");

        if (dateRetourPrevue.isBefore(dateEmprunt)) {
            throw new IllegalArgumentException("dateRetourPrevue doit être >= dateEmprunt");
        }
        if (dateRetourEffective != null && dateRetourEffective.isBefore(dateEmprunt)) {
            throw new IllegalArgumentException("dateRetourEffective ne peut pas être avant dateEmprunt");
        }

        this.dateRetourEffective = dateRetourEffective;
        this.penalite = penalite; // peut être null (pas encore générée)
    }

    // ======================================================================
    // ================================ MÉTIER ===============================
    // ======================================================================

    /**
     * Indique si l'emprunt est toujours en cours.
     *
     * @return {@code true} si aucune date de retour effective n'est enregistrée
     */
    public boolean estEnCours() {
        return dateRetourEffective == null;
    }

    /**
     * Indique si l'emprunt est en retard à une date donnée.
     *
     * <p>La date de référence utilisée est :</p>
     * <ul>
     *     <li>la date passée en paramètre si l'emprunt est encore en cours</li>
     *     <li>la date de retour effective si l'emprunt est déjà rendu</li>
     * </ul>
     *
     * @param asOf date de référence (généralement "aujourd'hui")
     * @return {@code true} si la date de référence est postérieure à la date de retour prévue
     */
    public boolean estEnRetard(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        LocalDate reference = estEnCours() ? asOf : dateRetourEffective;
        return reference.isAfter(dateRetourPrevue);
    }

    /**
     * Calcule le nombre de jours de retard à une date donnée.
     *
     * @param asOf date de référence
     * @return nombre de jours de retard (0 si pas de retard)
     */
    public long joursDeRetard(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        LocalDate reference = estEnCours() ? asOf : dateRetourEffective;
        long diff = ChronoUnit.DAYS.between(dateRetourPrevue, reference);
        return Math.max(0L, diff);
    }

    /**
     * Calcule la pénalité courante en fonction d'un tarif par jour donné.
     * <p>
     * Cette méthode ne crée pas d'entité {@link Penalite}, elle effectue uniquement un calcul.
     * </p>
     *
     * @param asOf date de référence (ex : date du jour)
     * @param tarifParJour montant à appliquer par jour de retard
     * @return montant de la pénalité courante
     */
    public Money penaliteCourante(LocalDate asOf, Money tarifParJour) {
        Objects.requireNonNull(asOf, "asOf");
        Objects.requireNonNull(tarifParJour, "tarifParJour");
        long jours = joursDeRetard(asOf);
        return tarifParJour.multiply(jours);
    }

    /**
     * Calcule la pénalité courante à partir de la politique par défaut
     * ({@link #PENALITE_PAR_JOUR_DEFAUT}).
     *
     * @param asOf date de référence
     * @return montant de la pénalité courante
     */
    public Money penaliteCourante(LocalDate asOf) {
        return penaliteCourante(asOf, PENALITE_PAR_JOUR_DEFAUT);
    }

    /**
     * Clôture l'emprunt en enregistrant la date de retour.
     *
     * <p>Règles :</p>
     * <ul>
     *     <li>La date de retour ne peut pas être antérieure à la date d'emprunt.</li>
     *     <li>Si une date de retour existe déjà et qu'elle est différente,
     *         une exception est levée (immutable une fois fixée).</li>
     * </ul>
     *
     * @param dateRetour date réelle de retour
     */
    public void cloturerRetour(LocalDate dateRetour) {
        Objects.requireNonNull(dateRetour, "dateRetour");
        if (dateRetour.isBefore(dateEmprunt)) {
            throw new IllegalArgumentException("La date de retour ne peut pas être avant la date d'emprunt");
        }
        if (this.dateRetourEffective != null && !this.dateRetourEffective.equals(dateRetour)) {
            throw new IllegalStateException("L'emprunt est déjà clôturé avec une autre date de retour");
        }
        this.dateRetourEffective = dateRetour;
    }

    /**
     * Méthode de façade utilisée par les services métiers pour enregistrer
     * un retour de document. Elle délègue simplement à {@link #cloturerRetour(LocalDate)}.
     *
     * @param dateRetour date réelle de retour
     */
    public void enregistrerRetour(LocalDate dateRetour) {
        cloturerRetour(dateRetour);
    }

    /**
     * Calcule le statut métier de l'emprunt à une date donnée.
     *
     * @param asOf date de référence
     * @return le statut calculé ({@link EmpruntStatus})
     */
    public EmpruntStatus statut(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        if (!estEnCours()) {
            // Document rendu
            return dateRetourEffective.isAfter(dateRetourPrevue)
                    ? EmpruntStatus.RETOURNE_EN_RETARD
                    : EmpruntStatus.RENDU;
        }
        return estEnRetard(asOf) ? EmpruntStatus.EN_RETARD : EmpruntStatus.EN_COURS;
    }

    /**
     * Génère et associe une {@link Penalite} si l'emprunt est en retard
     * à la date donnée et qu'aucune pénalité n'existe encore.
     *
     * @param asOf date de référence
     * @param tarifParJour tarif par jour de retard
     * @return {@link Optional} contenant la pénalité créée (ou existante),
     *         ou vide si aucune pénalité n'est nécessaire
     */
    public Optional<Penalite> genererPenaliteSiNecessaire(LocalDate asOf, Money tarifParJour) {
        Objects.requireNonNull(asOf, "asOf");
        Objects.requireNonNull(tarifParJour, "tarifParJour");

        if (!estEnRetard(asOf)) {
            return Optional.empty();
        }
        if (this.penalite != null) {
            return Optional.of(this.penalite);
        }

        Money montant = penaliteCourante(asOf, tarifParJour);
        this.penalite = Penalite.creerPour(this, montant, asOf);
        return Optional.of(this.penalite);
    }

    /**
     * Retourne la pénalité associée à cet emprunt, le cas échéant.
     *
     * @return un Optional pouvant contenir une {@link Penalite}
     */
    public Optional<Penalite> getPenalite() {
        return Optional.ofNullable(penalite);
    }

    // ======================================================================
    // ================================ GETTERS ===============================
    // ======================================================================

    public Long getId() { return id; }
    public Document getDocument() { return document; }
    public Adherent getEmprunteur() { return emprunteur; }
    public LocalDate getDateEmprunt() { return dateEmprunt; }
    public LocalDate getDateRetourPrevue() { return dateRetourPrevue; }
    public LocalDate getDateRetourEffective() { return dateRetourEffective; }

    // ======================================================================
    // ============================== IDENTIFIANT =============================
    // ======================================================================

    /**
     * Définit l'identifiant technique de l'emprunt (généré par la base de données).
     * <p>
     * Règles :
     * </p>
     * <ul>
     *     <li>Les valeurs null sont ignorées (n'ont aucun effet).</li>
     *     <li>L'identifiant doit être strictement positif.</li>
     *     <li>Une fois défini, il ne peut plus être modifié.</li>
     * </ul>
     *
     * @param id identifiant à affecter
     */
    public void setId(Long id) {
        if (id == null) {
            return; // On tolère le null pour ne pas écraser un id existant.
        }
        if (id <= 0) {
            throw new IllegalArgumentException("L'identifiant d'un Emprunt doit être > 0.");
        }
        if (this.id != null && !this.id.equals(id)) {
            throw new IllegalStateException(
                    "L'identifiant de l'Emprunt est déjà défini (" + this.id + "), modification interdite."
            );
        }
        this.id = id;
    }

    /**
     * Surcharge pratique pour accepter un identifiant primitif.
     *
     * @param id identifiant à affecter
     */
    public void setId(long id) {
        setId(Long.valueOf(id));
    }
}