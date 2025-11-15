package application.domain.core.emprunt;

import application.domain.core.document.Document;
import application.domain.core.personne.Adherent;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * Entité domaine Emprunt.
 * - Calcule la pénalité courante (Money)
 * - Peut générer une Penalite persistable si retard confirmé
 */
public final class Emprunt {

    private Long id;

    private final Document document;
    private final Adherent emprunteur;

    private final LocalDate dateEmprunt;
    private final LocalDate dateRetourPrevue;
    private LocalDate dateRetourEffective; // null => en cours

    // Association optionnelle 0..1 : une pénalité peut être générée pour cet emprunt
    private Penalite penalite;

    // Politiques par défaut
    public static final int DUREE_JOURS_DEFAUT = 21;
    public static final Money PENALITE_PAR_JOUR_DEFAUT = Money.euros(0.50);

    // ---------- Factories ----------
    public static Emprunt creer(Document doc, Adherent adherent, LocalDate dateEmprunt) {
        Objects.requireNonNull(doc);
        Objects.requireNonNull(adherent);
        Objects.requireNonNull(dateEmprunt);
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

    public static Emprunt creer(Document doc, Adherent adherent, LocalDate dateEmprunt, int dureeJours) {
        Objects.requireNonNull(doc);
        Objects.requireNonNull(adherent);
        Objects.requireNonNull(dateEmprunt);
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
     * Factory utilisée par le service quand la date de retour prévue est déjà connue.
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

    // ---------- Constructeur ----------
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
        this.penalite = penalite; // peut être null (non générée)
    }

    // ---------- Métier ----------
    public boolean estEnCours() {
        return dateRetourEffective == null;
    }

    public boolean estEnRetard(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        LocalDate reference = estEnCours() ? asOf : dateRetourEffective;
        return reference.isAfter(dateRetourPrevue);
    }

    public long joursDeRetard(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        LocalDate reference = estEnCours() ? asOf : dateRetourEffective;
        long diff = ChronoUnit.DAYS.between(dateRetourPrevue, reference);
        return Math.max(0L, diff);
    }

    /**
     * Calcule la pénalité courante avec un tarif par jour fourni.
     * Ne crée pas d'entité Penalite ; pur calcul.
     */
    public Money penaliteCourante(LocalDate asOf, Money tarifParJour) {
        Objects.requireNonNull(asOf, "asOf");
        Objects.requireNonNull(tarifParJour, "tarifParJour");
        long jours = joursDeRetard(asOf);
        return tarifParJour.multiply(jours);
    }

    /**
     * Calcule la pénalité courante selon la politique par défaut (0,50 € / jour).
     */
    public Money penaliteCourante(LocalDate asOf) {
        return penaliteCourante(asOf, PENALITE_PAR_JOUR_DEFAUT);
    }

    /**
     * Clôture l'emprunt (enregistre la date de retour).
     * Idempotent si la même date est fournie.
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
     * Méthode utilisée par le service pour enregistrer un retour.
     * Délègue à la logique de 'cloturerRetour'.
     */
    public void enregistrerRetour(LocalDate dateRetour) {
        cloturerRetour(dateRetour);
    }

    /**
     * Statut métier calculé à la date 'asOf'.
     */
    public EmpruntStatus statut(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        if (!estEnCours()) {
            // Rendu
            return dateRetourEffective.isAfter(dateRetourPrevue)
                    ? EmpruntStatus.RETOURNE_EN_RETARD
                    : EmpruntStatus.RENDU;
        }
        // Toujours en cours
        return estEnRetard(asOf) ? EmpruntStatus.EN_RETARD : EmpruntStatus.EN_COURS;
    }

    /**
     * Génère et attache une Penalite si l'emprunt est en retard à 'asOf'
     * ET qu'aucune pénalité n'a déjà été générée.
     * Retourne la pénalité créée ou vide sinon.
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

    public Optional<Penalite> getPenalite() {
        return Optional.ofNullable(penalite);
    }

    // ---------- Getters ----------
    public Long getId() { return id; }
    public Document getDocument() { return document; }
    public Adherent getEmprunteur() { return emprunteur; }
    public LocalDate getDateEmprunt() { return dateEmprunt; }
    public LocalDate getDateRetourPrevue() { return dateRetourPrevue; }
    public LocalDate getDateRetourEffective() { return dateRetourEffective; }

    /**
     * Définit l'identifiant technique (généré par la persistance).
     */
    public void setId(Long id) {
        if (id == null) return; // toléré si on veut ignorer un null
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

    /** Délégation pratique pour les appels avec primitif. */
    public void setId(long id) {
        setId(Long.valueOf(id));
    }
}