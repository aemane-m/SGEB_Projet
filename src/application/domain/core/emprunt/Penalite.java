package application.domain.core.emprunt;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entité domaine représentant une pénalité monétaire liée à un Emprunt.
 * Stocke les ID des relations pour éviter les N+1.
 *
 * Invariants :
 * <ul>
 * <li>montant > 0</li>
 * <li>{@code datePaiement == null} &harr; non payée</li>
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
    
    // Gardez une référence 'transient' si le service a besoin de la charger
    private transient Emprunt emprunt; 

    /**
     * Constructeur principal pour la persistance (chargement DB).
     */
    public Penalite(Long id, Long empruntId, Long adherentId, Money montant, LocalDate dateGeneration, LocalDate datePaiement) {
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
    }

    /** Factory : crée une pénalité non payée à la date donnée (Logique métier). */
    public static Penalite creerPour(Emprunt emprunt, Money montant, LocalDate dateGeneration) {
        Objects.requireNonNull(emprunt, "emprunt");
        Objects.requireNonNull(emprunt.getId(), "emprunt.id");
        Objects.requireNonNull(emprunt.getEmprunteur(), "emprunt.emprunteur");
        Objects.requireNonNull(emprunt.getEmprunteur().getId(), "emprunt.emprunteur.id");
        
        Penalite p = new Penalite(
            null,
            emprunt.getId(),
            emprunt.getEmprunteur().getId(),
            montant,
            dateGeneration,
            null
        );
        
        // Stocke l'objet source (transient) pour un accès immédiat
        p.setEmprunt(emprunt); 
        return p;
    }

    /** @return {@code true} si la pénalité est payée. */
    public boolean estPayee() {
        return datePaiement != null;
    }

    /** Marque la pénalité comme payée à la date fournie. */
    public void marquerCommePayee(LocalDate date) {
        Objects.requireNonNull(date, "date");
        if (date.isBefore(dateGeneration)) {
            throw new IllegalArgumentException("La date de paiement ne peut pas être avant la génération");
        }
        this.datePaiement = date;
    }

    /** (Optionnel) Annule l'état payé. */
    public void annulerPaiement() {
        this.datePaiement = null;
    }

    // --- Getters ---
    public Long getId() { return id; }
    public Money getMontant() { return montant; }
    public LocalDate getDateGeneration() { return dateGeneration; }
    public LocalDate getDatePaiement() { return datePaiement; }
    
    // --- Getters des relations ---
    public Long getEmpruntId() { return empruntId; }
    public Long getAdherentId() { return adherentId; }

    /** (Optionnel) Retourne l'objet Emprunt s'il a été chargé. */
    public Emprunt getEmprunt() { return emprunt; }
    
    /** (Optionnel) Permet à un service de "charger" l'emprunt. */
    public void setEmprunt(Emprunt emprunt) { this.emprunt = emprunt; }

    /** Définit l'identifiant technique (généré par la persistance). */
    public void setId(Long id) {
        if (id == null) return;
        if (id <= 0) {
            throw new IllegalArgumentException("L'identifiant de la pénalité doit être > 0.");
        }
        if (this.id != null && !this.id.equals(id)) {
            throw new IllegalStateException("L'identifiant de la pénalité est déjà défini (" + this.id + "), modification interdite.");
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
}
