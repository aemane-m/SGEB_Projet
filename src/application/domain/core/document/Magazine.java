package application.domain.core.document;

/**
 * Représente un magazine dans le SGEB.
 * Spécialise la classe abstraite {@link Document}.
 *
 * Champs spécifiques :
 * - numero : numéro du magazine (édition)
 * - moisParution : mois ou période de parution (ex: "Mars 2025")
 * - periodicite : fréquence de parution (ex: "Mensuel", "Hebdomadaire")
 *
 * Remarque persistance :
 * - Héritage TPH (Table-Per-Hierarchy) avec colonne "dtype" = "Magazine".
 */
public class Magazine extends Document {

    // === Champs spécifiques ===
    private Integer numero;
    private String moisParution;
    private String periodicite;

    // === Constructeurs ===

    /** Constructeur vide requis pour ORM ou (dé)sérialisation */
    public Magazine() {
        super();
        setDtype("Magazine");
    }

    /**
     * Constructeur de base (minimal)
     */
    public Magazine(Integer numero, String moisParution, String periodicite) {
        super();
        setDtype("Magazine");
        this.numero = numero;
        this.moisParution = moisParution;
        this.periodicite = periodicite;
    }

    /**
     * Constructeur complet aligné avec Document ET les champs spécifiques du Magazine.
     * C'est le constructeur principal utilisé par le Repository.
     */
    public Magazine(Long id,
                    String titre,
                    Integer anneePublication,
                    String editeur,
                    String genre,
                    String coverRef,
                    String coverMime,
                    Long coverUpdatedAt,
                    Integer numero,
                    String moisParution,
                    String periodicite) {

        // 1. Appel au constructeur parent (Document)
        //    (dtype forcé à "Magazine")
        super(id, titre, anneePublication, editeur, genre, "Magazine",
              coverRef, coverMime, coverUpdatedAt);

        // 2. Initialisation des champs spécifiques (Magazine)
        this.numero = numero;
        this.moisParution = moisParution;
        this.periodicite = periodicite;
        
        // 3. Validation (optionnelle)
        validateInvariants();
    }

    // === Getters / Setters ===

    public Integer getNumero() {
        return numero;
    }
    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public String getMoisParution() {
        return moisParution;
    }
    public void setMoisParution(String moisParution) {
        this.moisParution = moisParution;
    }

    public String getPeriodicite() {
        return periodicite;
    }
    public void setPeriodicite(String periodicite) {
        this.periodicite = periodicite;
    }

    // === Validation de cohérence minimale ===
    
    public void validateInvariants() throws IllegalArgumentException {
        if (getTitre() == null || getTitre().isBlank()) {
            throw new IllegalArgumentException("Le titre du magazine est obligatoire.");
        }
        if (numero != null && numero <= 0) {
            throw new IllegalArgumentException("Le numéro du magazine doit être positif.");
        }
    }

    // === Utilitaire ===
    
    @Override
    public String toString() {
        String base = super.toString();
        return base + String.format(" | N°%s (%s) [%s]",
                numero != null ? numero : "N/A",
                moisParution != null ? moisParution : "inconnu",
                periodicite != null ? periodicite : "N/A");
    }
}