package application.domain.core.document;

/**
 * Représente un magazine dans le système SGEB.
 *
 * <p>Cette classe est une spécialisation de {@link Document}.
 * Les magazines ajoutent des caractéristiques propres aux supports périodiques :</p>
 *
 * <ul>
 *     <li><strong>numero</strong> : numéro d’édition (ex : 158)</li>
 *     <li><strong>moisParution</strong> : mois ou période de publication (ex : "Mars 2025")</li>
 *     <li><strong>periodicite</strong> : fréquence de parution (ex : "Mensuel", "Hebdomadaire")</li>
 * </ul>
 *
 * <h3>Héritage et persistance</h3>
 * <p>
 * Le SGEB utilise la stratégie <strong>Table-Per-Hierarchy</strong> (TPH) pour stocker
 * les documents dans une seule table SQLite.
 * Une colonne <code>dtype</code> identifie le type concret.
 * Pour un magazine, <code>dtype = "Magazine"</code>.
 * </p>
 *
 * <h3>Invariants métier</h3>
 * <ul>
 *     <li>Un magazine doit obligatoirement avoir un titre non vide.</li>
 *     <li>Le numéro d’édition, s'il est renseigné, doit être strictement positif.</li>
 *     <li>La périodicité et le mois de parution peuvent être laissés vides (saisie flexible).</li>
 * </ul>
 */
public class Magazine extends Document {

    // ======================================================================
    // ========================== CHAMPS SPÉCIFIQUES =========================
    // ======================================================================

    /** Numéro d’édition du magazine (ex : 152). */
    private Integer numero;

    /** Mois ou période de parution (ex : "Mars 2025", "Hiver 2024"). */
    private String moisParution;

    /** Fréquence de publication (ex : "Mensuel", "Hebdomadaire"). */
    private String periodicite;

    // ======================================================================
    // =============================== CONSTRUCTEURS =========================
    // ======================================================================

    /**
     * Constructeur vide requis pour la désérialisation ou les frameworks ORM.
     * <p>
     * Le type concret (<code>dtype</code>) est automatiquement fixé à "Magazine".
     * </p>
     */
    public Magazine() {
        super();
        setDtype("Magazine");
    }

    /**
     * Constructeur minimal pour créer un magazine uniquement avec
     * ses champs spécifiques.
     * <p>
     * Les attributs communs (titre, éditeur, année...) doivent être définis ensuite
     * via les setters hérités.
     * </p>
     *
     * @param numero numéro d’édition
     * @param moisParution mois ou période de publication
     * @param periodicite fréquence de parution
     */
    public Magazine(Integer numero, String moisParution, String periodicite) {
        super();
        setDtype("Magazine");
        this.numero = numero;
        this.moisParution = moisParution;
        this.periodicite = periodicite;
    }

    /**
     * Constructeur complet utilisé principalement par les repositories
     * pour reconstruire un Magazine depuis la base de données SQLite.
     *
     * @param id identifiant du document
     * @param titre titre du magazine
     * @param anneePublication année de publication
     * @param editeur éditeur du magazine
     * @param genre genre / catégorie (ex : "Actualités", "Sciences")
     * @param coverRef fichier de couverture
     * @param coverMime type MIME de la couverture
     * @param coverUpdatedAt timestamp mise à jour couverture
     * @param numero numéro d’édition
     * @param moisParution mois ou période de parution
     * @param periodicite fréquence de parution
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

        // dtype forcé à "Magazine"
        super(id, titre, anneePublication, editeur, genre, "Magazine",
              coverRef, coverMime, coverUpdatedAt);

        this.numero = numero;
        this.moisParution = moisParution;
        this.periodicite = periodicite;

        validateInvariants();
    }

    // ======================================================================
    // ============================ GETTERS / SETTERS ========================
    // ======================================================================

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public String getMoisParution() { return moisParution; }
    public void setMoisParution(String moisParution) { this.moisParution = moisParution; }

    public String getPeriodicite() { return periodicite; }
    public void setPeriodicite(String periodicite) { this.periodicite = periodicite; }

    // ======================================================================
    // ========================== VALIDATION INVARIANTS ======================
    // ======================================================================

    /**
     * Valide les invariants métier minimaux d’un magazine.
     *
     * @throws IllegalArgumentException si des données obligatoires sont incorrectes
     */
    public void validateInvariants() throws IllegalArgumentException {
        if (getTitre() == null || getTitre().isBlank()) {
            throw new IllegalArgumentException("Le titre du magazine est obligatoire.");
        }
        if (numero != null && numero <= 0) {
            throw new IllegalArgumentException("Le numéro du magazine doit être strictement positif.");
        }
    }

    // ======================================================================
    // =============================== UTILITAIRE =============================
    // ======================================================================

    /**
     * Retourne une chaîne descriptive du magazine, incluant :
     * <ul>
     *     <li>les informations héritées de {@link Document}</li>
     *     <li>le numéro d’édition</li>
     *     <li>la période de publication</li>
     *     <li>la périodicité</li>
     * </ul>
     *
     * @return représentation textuelle du magazine
     */
    @Override
    public String toString() {
        String base = super.toString();
        return base + String.format(
                " | N°%s (%s) [%s]",
                numero != null ? numero : "N/A",
                moisParution != null ? moisParution : "inconnu",
                periodicite != null ? periodicite : "N/A"
        );
    }
}