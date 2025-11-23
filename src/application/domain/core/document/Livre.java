package application.domain.core.document;

/**
 * Représente un Livre dans le système SGEB.
 *
 * <p>Cette classe est une spécialisation de {@link Document}.
 * Un livre possède des attributs supplémentaires :
 * </p>
 *
 * <ul>
 *     <li><strong>auteur</strong> : auteur principal</li>
 *     <li><strong>isbn</strong> : identifiant ISBN-10 ou ISBN-13</li>
 *     <li><strong>nbPages</strong> : nombre de pages</li>
 * </ul>
 *
 * <h3>Héritage et persistance</h3>
 * <p>
 * Le SGEB utilise une stratégie d’héritage <strong>TPH (Table-Per-Hierarchy)</strong>.
 * Tous les documents (Livre, Magazine, CD) sont enregistrés dans une seule table SQLite.
 * Une colonne <code>dtype</code> indique le type concret.
 * Pour un livre : <code>dtype = "Livre"</code>.
 * </p>
 *
 * <h3>Invariants métier</h3>
 * <ul>
 *     <li>Un livre doit avoir un titre non vide.</li>
 *     <li>Le nombre de pages, s'il existe, doit être strictement positif.</li>
 *     <li>L’auteur et l’ISBN sont optionnels (saisie progressive).</li>
 * </ul>
 */
public class Livre extends Document {

    // ======================================================================
    // ========================== CHAMPS SPÉCIFIQUES =========================
    // ======================================================================

    /** Auteur principal du livre (optionnel). */
    private String auteur;

    /** Numéro ISBN-10 ou ISBN-13 (optionnel). */
    private String isbn;

    /** Nombre total de pages du livre (>= 1 si renseigné). */
    private Integer nbPages;

    // ======================================================================
    // =============================== CONSTRUCTEURS =========================
    // ======================================================================

    /**
     * Constructeur vide requis pour la désérialisation et certains frameworks.
     * <p>Le type concret (<code>dtype</code>) est automatiquement fixé à "Livre".</p>
     */
    public Livre() {
        super();
        setDtype("Livre");
    }

    /**
     * Constructeur minimal permettant d'initialiser uniquement
     * les champs propres au Livre.
     * <p>
     * Les attributs communs du document (titre, éditeur, année…) peuvent
     * être définis ensuite via les setters hérités.
     * </p>
     *
     * @param auteur auteur principal
     * @param isbn identifiant ISBN
     * @param nbPages nombre de pages
     */
    public Livre(String auteur, String isbn, Integer nbPages) {
        super();
        setDtype("Livre");
        this.auteur = auteur;
        this.isbn = isbn;
        this.nbPages = nbPages;
    }

    /**
     * Constructeur complet utilisé principalement par les repositories pour
     * reconstruire un Livre à partir des données SQLite.
     *
     * @param id identifiant du document
     * @param titre titre du livre
     * @param anneePublication année de publication
     * @param editeur nom de l'éditeur
     * @param genre genre littéraire
     * @param coverRef fichier de couverture associé
     * @param coverMime type MIME de la couverture (ex : "image/jpeg")
     * @param coverUpdatedAt timestamp de mise à jour de la couverture
     * @param auteur auteur principal
     * @param isbn identifiant ISBN-10 ou ISBN-13
     * @param nbPages nombre de pages du livre
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

        // dtype forcé à "Livre"
        super(id, titre, anneePublication, editeur, genre, "Livre",
              coverRef, coverMime, coverUpdatedAt);

        this.auteur = auteur;
        this.isbn = isbn;
        this.nbPages = nbPages;

        validateInvariants();
    }

    // ======================================================================
    // ============================ GETTERS / SETTERS ========================
    // ======================================================================

    public String getAuteur() { return auteur; }
    public void setAuteur(String auteur) { this.auteur = auteur; }

    @Override
    public String getIsbn() { return isbn; } // surcharge de Document
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public Integer getNbPages() { return nbPages; }
    public void setNbPages(Integer nbPages) { this.nbPages = nbPages; }

    // ======================================================================
    // ========================== VALIDATION INVARIANTS ======================
    // ======================================================================

    /**
     * Valide les contraintes minimales d’un livre.
     * <p>
     * Cette validation reste volontairement simple : les règles
     * plus complexes (ISBN valide, formats, politiques) pourront
     * être gérées dans un service métier ou un Validator.
     * </p>
     *
     * @throws IllegalArgumentException en cas de données invalides
     */
    public void validateInvariants() throws IllegalArgumentException {
        if (getTitre() == null || getTitre().isBlank()) {
            throw new IllegalArgumentException("Le titre du livre est obligatoire.");
        }
        if (nbPages != null && nbPages <= 0) {
            throw new IllegalArgumentException("Le nombre de pages doit être positif.");
        }
    }

    // ======================================================================
    // =============================== UTILITAIRE =============================
    // ======================================================================

    /**
     * Retourne une chaîne descriptive contenant les informations principales du livre,
     * y compris celles de {@link Document}.
     *
     * @return représentation textuelle du livre
     */
    @Override
    public String toString() {
        String base = super.toString(); // inclut dtype, titre, année, éditeur
        return base + String.format(
                " | Auteur: %s | ISBN: %s | Pages: %s",
                auteur != null ? auteur : "N/A",
                isbn != null ? isbn : "N/A",
                nbPages != null ? nbPages : "N/A"
        );
    }
}