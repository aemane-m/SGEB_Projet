package application.domain.core.document;

/**
 * Classe abstraite représentant un document générique dans le système SGEB.
 *
 * <p>Elle sert de classe parent (super-classe) pour les différents types
 * de documents gérés dans la bibliothèque :</p>
 *
 * <ul>
 *     <li>{@link application.domain.core.document.Livre Livre}</li>
 *     <li>{@link application.domain.core.document.Magazine Magazine}</li>
 *     <li>{@link application.domain.core.document.CD CD}</li>
 * </ul>
 *
 * <p>Cette classe regroupe tous les attributs communs aux documents :
 * titre, année, éditeur, genre et informations liées à la couverture (image).</p>
 *
 * <h3>Gestion des couvertures</h3>
 * <p>
 * Chaque document possède une référence de couverture (coverRef) qui correspond
 * au nom du fichier image stocké dans <code>/data/covers/</code>.
 * Les métadonnées coverMime et coverUpdatedAt permettent de savoir
 * le type de l'image et sa date de dernière mise à jour.
 * </p>
 *
 * <p>Cette classe est abstraite : on ne crée jamais directement un Document,
 * mais uniquement une sous-classe (Livre, Magazine, CD).</p>
 */
public abstract class Document {

    // ======================================================================
    // ========================== CHAMPS PRINCIPAUX ==========================
    // ======================================================================

    /** Identifiant unique dans la base de données (clé primaire). */
    private Long id;

    /** Titre du document (ex : "Le Petit Prince"). */
    private String titre;

    /** Année de publication (ex : 1943). */
    private Integer anneePublication;

    /** Nom de l'éditeur (ex : "Gallimard"). */
    private String editeur;

    /** Genre littéraire ou catégorie (ex : "Roman", "Science", "Fantaisie"). */
    private String genre;

    /**
     * Nom du type concret de document.
     * <p>
     * Exemple : "Livre", "Magazine", "CD".
     * Utile pour la persistance (pattern Table-per-Hierarchy).
     * </p>
     */
    private String dtype;

    // ======================================================================
    // ====================== MÉTADONNÉES DE COUVERTURE =====================
    // ======================================================================

    /** Nom du fichier image associé au document (ex : "12345.jpg"). */
    private String coverRef;

    /** Type MIME de l'image (ex : "image/jpeg", "image/png"). */
    private String coverMime;

    /** Timestamp (millisecondes) indiquant la dernière mise à jour de la couverture. */
    private Long coverUpdatedAt;

    // ======================================================================
    // =========================== CONSTRUCTEURS ============================
    // ======================================================================

    /**
     * Constructeur par défaut nécessaire pour certains frameworks
     * (ex : instanciation via réflexion).
     */
    public Document() {}

    /**
     * Constructeur complet pour initialiser toutes les propriétés partagées
     * entre les documents.
     *
     * @param id identifiant unique
     * @param titre titre du document
     * @param anneePublication année de publication
     * @param editeur nom de l'éditeur
     * @param genre genre du document
     * @param dtype type concret (Livre, Magazine, CD)
     * @param coverRef nom du fichier de couverture
     * @param coverMime type MIME de la couverture
     * @param coverUpdatedAt timestamp de dernière mise à jour
     */
    public Document(Long id,
                    String titre,
                    Integer anneePublication,
                    String editeur,
                    String genre,
                    String dtype,
                    String coverRef,
                    String coverMime,
                    Long coverUpdatedAt) {

        this.id = id;
        this.titre = titre;
        this.anneePublication = anneePublication;
        this.editeur = editeur;
        this.genre = genre;
        this.dtype = dtype;
        this.coverRef = coverRef;
        this.coverMime = coverMime;
        this.coverUpdatedAt = coverUpdatedAt;
    }

    // ======================================================================
    // ============================ GETTERS/SETTERS ==========================
    // ======================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnneePublication() { return anneePublication; }
    public void setAnneePublication(Integer anneePublication) { this.anneePublication = anneePublication; }

    public String getEditeur() { return editeur; }
    public void setEditeur(String editeur) { this.editeur = editeur; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getDtype() { return dtype; }
    public void setDtype(String dtype) { this.dtype = dtype; }

    public String getCoverRef() { return coverRef; }
    public void setCoverRef(String coverRef) { this.coverRef = coverRef; }

    public String getCoverMime() { return coverMime; }
    public void setCoverMime(String coverMime) { this.coverMime = coverMime; }

    public Long getCoverUpdatedAt() { return coverUpdatedAt; }
    public void setCoverUpdatedAt(Long coverUpdatedAt) { this.coverUpdatedAt = coverUpdatedAt; }

    // ======================================================================
    // ========================== MÉTHODES UTILITAIRES =======================
    // ======================================================================

    /**
     * Retourne une représentation textuelle simple du document.
     *
     * @return chaîne contenant : type + titre + année + éditeur
     */
    @Override
    public String toString() {
        return String.format("[%s] %s (%d) - %s",
                dtype != null ? dtype : getClass().getSimpleName(),
                titre,
                anneePublication != null ? anneePublication : 0,
                editeur != null ? editeur : "Éditeur inconnu");
    }

    /**
     * Méthode fournie pour compatibilité avec les classes qui utilisent l'ISBN,
     * mais non implémentée ici car tous les documents n'ont pas d'ISBN.
     *
     * <p>
     * Les sous-classes comme {@link Livre} peuvent surcharger cette méthode
     * et retourner la vraie valeur.
     * </p>
     *
     * @return null par défaut
     */
    public String getIsbn() {
        return null;
    }
}