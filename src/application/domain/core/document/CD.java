package application.domain.core.document;

/**
 * Représente un CD (album audio) dans le système SGEB.
 *
 * <p>Cette classe est une spécialisation de la classe abstraite {@link Document}.
 * Elle ajoute des attributs propres aux supports audio :</p>
 *
 * <ul>
 *     <li><strong>dureeMinutes</strong> : durée totale du CD en minutes</li>
 *     <li><strong>artistePrincipal</strong> : nom de l'artiste principal ou du groupe</li>
 *     <li><strong>label</strong> : maison de disque ou producteur</li>
 * </ul>
 *
 * <h3>Héritage et persistance</h3>
 * <p>
 * Le SGEB utilise un modèle d’héritage <strong>Table-Per-Hierarchy (TPH)</strong>
 * pour stocker tous les types de documents dans une seule table SQLite.
 * Une colonne <code>dtype</code> permet d'identifier le type concret.
 * Pour un CD, <code>dtype = "CD"</code>.
 * </p>
 *
 * <h3>Invariants</h3>
 * <ul>
 *     <li>Un CD doit avoir un titre non vide.</li>
 *     <li>Sa durée, si renseignée, doit être strictement positive.</li>
 * </ul>
 */
public class CD extends Document {

    // ======================================================================
    // ========================== CHAMPS SPÉCIFIQUES =========================
    // ======================================================================

    /** Durée totale du CD en minutes (ex : 42). */
    private Integer dureeMinutes;

    /** Nom de l'artiste principal ou du groupe. */
    private String artistePrincipal;

    /** Maison de disque, label ou producteur. */
    private String label;

    // ======================================================================
    // =============================== CONSTRUCTEURS =========================
    // ======================================================================

    /**
     * Constructeur vide requis pour la désérialisation ou la création
     * via réflexion (ex : frameworks, SQLite, parsers).
     * <p>Le champ {@code dtype} est automatiquement fixé à "CD".</p>
     */
    public CD() {
        super();
        setDtype("CD");
    }

    /**
     * Constructeur minimal permettant de créer un CD avec ses informations
     * spécifiques (sans métadonnées de document).
     *
     * @param dureeMinutes durée totale en minutes
     * @param artistePrincipal nom de l'artiste ou du groupe
     * @param label maison de disque
     */
    public CD(Integer dureeMinutes, String artistePrincipal, String label) {
        super();
        setDtype("CD");
        this.dureeMinutes = dureeMinutes;
        this.artistePrincipal = artistePrincipal;
        this.label = label;
    }

    /**
     * Constructeur complet aligné avec le modèle {@link Document}.
     * <p>
     * Utilisé principalement par les repositories pour reconstruire un CD
     * à partir des données SQLite.
     * </p>
     *
     * @param id identifiant du document
     * @param titre titre du CD
     * @param anneePublication année de publication
     * @param editeur éditeur / producteur
     * @param genre genre musical (ex : "Rock", "Classique")
     * @param coverRef fichier de couverture
     * @param coverMime type MIME de la couverture
     * @param coverUpdatedAt timestamp mise à jour couverture
     * @param dureeMinutes durée totale
     * @param artistePrincipal artiste principal ou groupe
     * @param label maison de disque
     */
    public CD(Long id,
              String titre,
              Integer anneePublication,
              String editeur,
              String genre,
              String coverRef,
              String coverMime,
              Long coverUpdatedAt,
              Integer dureeMinutes,
              String artistePrincipal,
              String label) {

        // dtype forcé à "CD"
        super(id, titre, anneePublication, editeur, genre, "CD",
              coverRef, coverMime, coverUpdatedAt);

        this.dureeMinutes = dureeMinutes;
        this.artistePrincipal = artistePrincipal;
        this.label = label;

        validateInvariants();
    }

    // ======================================================================
    // ============================ GETTERS / SETTERS ========================
    // ======================================================================

    public Integer getDureeMinutes() { return dureeMinutes; }
    public void setDureeMinutes(Integer dureeMinutes) { this.dureeMinutes = dureeMinutes; }

    public String getArtistePrincipal() { return artistePrincipal; }
    public void setArtistePrincipal(String artistePrincipal) { this.artistePrincipal = artistePrincipal; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    // ======================================================================
    // ========================== VALIDATION INVARIANTS ======================
    // ======================================================================

    /**
     * Vérifie les règles minimales de cohérence propres à un CD.
     *
     * @throws IllegalArgumentException si les données sont incohérentes
     */
    public void validateInvariants() throws IllegalArgumentException {
        if (getTitre() == null || getTitre().isBlank()) {
            throw new IllegalArgumentException("Le titre du CD est obligatoire.");
        }
        if (dureeMinutes != null && dureeMinutes <= 0) {
            throw new IllegalArgumentException("La durée du CD doit être positive.");
        }
    }

    // ======================================================================
    // =============================== UTILITAIRE =============================
    // ======================================================================

    /**
     * Retourne une représentation textuelle complète du CD, incluant :
     * <ul>
     *     <li>type + titre + année + éditeur (hérité de Document)</li>
     *     <li>artiste</li>
     *     <li>label</li>
     *     <li>durée totale</li>
     * </ul>
     *
     * @return chaîne formatée représentant le CD
     */
    @Override
    public String toString() {
        String base = super.toString();
        return base + String.format(
                " | %s (%s) - %s min",
                artistePrincipal != null ? artistePrincipal : "Artiste inconnu",
                label != null ? label : "Label inconnu",
                dureeMinutes != null ? dureeMinutes : "Durée inconnue"
        );
    }
}