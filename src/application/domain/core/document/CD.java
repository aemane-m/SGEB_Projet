package application.domain.core.document;

/**
 * Représente un CD dans le SGEB.
 * Spécialise la classe abstraite {@link Document}.
 *
 * Champs spécifiques :
 * - dureeMinutes : durée totale du CD en minutes
 * - artistePrincipal : nom de l'artiste principal ou du groupe
 * - label : maison de disque ou producteur
 *
 * Remarque persistance :
 * - Héritage TPH (Table-Per-Hierarchy) avec colonne "dtype" = "CD".
 */
public class CD extends Document {

    // === Champs spécifiques ===
    private Integer dureeMinutes;
    private String artistePrincipal;
    private String label;

    // === Constructeurs ===

    /** Constructeur vide requis pour ORM/désérialisation */
    public CD() {
        super();
        setDtype("CD");
    }

    /**
     * Constructeur minimal
     */
    public CD(Integer dureeMinutes, String artistePrincipal, String label) {
        super();
        setDtype("CD");
        this.dureeMinutes = dureeMinutes;
        this.artistePrincipal = artistePrincipal;
        this.label = label;
    }

    /**
     * Constructeur complet aligné avec Document
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

    // === Getters / Setters ===
    public Integer getDureeMinutes() { return dureeMinutes; }
    public void setDureeMinutes(Integer dureeMinutes) { this.dureeMinutes = dureeMinutes; }

    public String getArtistePrincipal() { return artistePrincipal; }
    public void setArtistePrincipal(String artistePrincipal) { this.artistePrincipal = artistePrincipal; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    // === Validation de cohérence minimale ===
    public void validateInvariants() throws IllegalArgumentException {
        if (getTitre() == null || getTitre().isBlank()) {
            throw new IllegalArgumentException("Le titre du CD est obligatoire.");
        }
        if (dureeMinutes != null && dureeMinutes <= 0) {
            throw new IllegalArgumentException("La durée du CD doit être positive.");
        }
    }

    // === Utilitaire ===
    @Override
    public String toString() {
        String base = super.toString();
        return base + String.format(" | %s (%s) - %s min",
                artistePrincipal != null ? artistePrincipal : "Artiste inconnu",
                label != null ? label : "Label inconnu",
                dureeMinutes != null ? dureeMinutes : "Durée inconnue");
    }
}
