package application.domain.core.document;

/**
 * Représente un Livre dans le SGEB.
 * Spécialise la classe abstraite {@link Document}.
 *
 * Champs spécifiques :
 * - auteur  : l'auteur principal du livre
 * - isbn    : identifiant du livre (ISBN-10 ou ISBN-13)
 * - nbPages : nombre de pages (>= 1)
 *
 * Remarque persistance :
 * - Héritage TPH (Table-Per-Hierarchy) avec colonne "dtype" = "Livre".
 */
public class Livre extends Document {

    // === Champs spécifiques ===
    private String auteur;
    private String isbn;
    private Integer nbPages;

    // === Constructeurs ===

    /** Constructeur vide requis pour la (dé)sérialisation et l'ORM. */
    public Livre() {
        super();
        // Optionnel : on peut poser une valeur par défaut pour dtype
        setDtype("Livre");
    }

    /**
     * Constructeur « minimal » pour créer un Livre avec ses champs dédiés.
     * Les champs communs (titre, éditeur, etc.) peuvent être posés ensuite via setters.
     */
    public Livre(String auteur, String isbn, Integer nbPages) {
        super();
        setDtype("Livre");
        this.auteur = auteur;
        this.isbn = isbn;
        this.nbPages = nbPages;
    }

    /**
     * Constructeur complet aligné avec Document ET les champs spécifiques du Livre.
     * C'est le constructeur principal utilisé par le Repository.
     */
    public Livre(Long id,
                 String titre,
                 Integer anneePublication,
                 String editeur,
                 String genre,
                 String coverRef,
                 String coverMime,
                 Long coverUpdatedAt,
                 String auteur,
                 String isbn,
                 Integer nbPages) {

        // 1. Appel au constructeur parent (Document)
        //    (dtype forcé à "Livre")
        super(id, titre, anneePublication, editeur, genre, "Livre",
              coverRef, coverMime, coverUpdatedAt);

        // 2. Initialisation des champs spécifiques (Livre)
        this.auteur = auteur;
        this.isbn = isbn;
        this.nbPages = nbPages;
        
        // 3. Validation (optionnelle)
        validateInvariants();
    }

    // === Getters / Setters ===

    public String getAuteur() {
        return auteur;
    }
    public void setAuteur(String auteur) {
        this.auteur = auteur;
    }

    public String getIsbn() {
        return isbn;
    }
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Integer getNbPages() {
        return nbPages;
    }
    public void setNbPages(Integer nbPages) {
        this.nbPages = nbPages;
    }

    // === Règles simples / invariants domaine ===
    
    /**
     * Valide quelques invariants métier basiques côté domaine.
     * (On garde volontairement simple – les validations complètes
     * seront gérées dans les services/policies si besoin.)
     */
    public void validateInvariants() throws IllegalArgumentException {
        if (getTitre() == null || getTitre().isBlank()) {
            throw new IllegalArgumentException("Le titre du livre est obligatoire.");
        }
        if (nbPages != null && nbPages <= 0) {
            throw new IllegalArgumentException("Le nombre de pages doit être positif.");
        }
        // Auteur et ISBN peuvent être null (saisie progressive)
    }

    // === Utilitaires ===
    
    @Override
    public String toString() {
        String base = super.toString(); // inclut dtype/titre/année/éditeur
        return base + String.format(" | Auteur: %s | ISBN: %s | Pages: %s",
                auteur != null ? auteur : "N/A",
                isbn != null ? isbn : "N/A",
                nbPages != null ? nbPages : "N/A");
    }
}