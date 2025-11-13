package application.domain.core.document;

/**
 * Classe abstraite représentant un document générique dans le système SGEB.
 * 
 * Elle sert de classe de base pour les entités spécialisées :
 * - Livre
 * - Magazine
 * - CD
 * 
 * Chaque document possède une référence de couverture (coverRef)
 * liée à une image stockée localement dans /data/covers/.
 */
public abstract class Document {

    // === Champs principaux ===
    private Long id;
    private String titre;
    private Integer anneePublication;
    private String editeur;
    private String genre;
    private String dtype; // Type concret (Livre, Magazine, CD)

    // === Métadonnées de couverture ===
    private String coverRef;       // nom ou identifiant du fichier
    private String coverMime;      // ex: "image/jpeg"
    private Long coverUpdatedAt;   // timestamp de mise à jour

    // === Constructeurs ===
    public Document() {
        // Constructeur par défaut requis pour l'instanciation dynamique
    }

    public Document(Long id, String titre, Integer anneePublication, String editeur, String genre, String dtype,
                    String coverRef, String coverMime, Long coverUpdatedAt) {
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

    // === Getters / Setters ===
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

    // === Méthodes utilitaires ===
    @Override
    public String toString() {
        return String.format("[%s] %s (%d) - %s", 
            dtype != null ? dtype : getClass().getSimpleName(),
            titre, 
            anneePublication != null ? anneePublication : 0,
            editeur != null ? editeur : "Éditeur inconnu");
    }

	public String getIsbn() {
		// TODO Auto-generated method stub
		return null;
	}
}

