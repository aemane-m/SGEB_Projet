package application.domain.core.personne;

/**
 * Représente une personne générique dans le système SGEB.
 *
 * <p>
 * Cette classe abstraite sert de base pour toutes les entités humaines du domaine,
 * telles que :
 * </p>
 *
 * <ul>
 *     <li>{@link Adherent} (lecteur / utilisateur final)</li>
 *     <li>Employé (ex. futur Administrateur, Bibliothécaire)</li>
 * </ul>
 *
 * <h3>Rôle dans l’architecture</h3>
 * <p>
 * Personne définit les attributs communs (nom, prénom, email, téléphone) et impose
 * les invariants minimaux du domaine humain.
 * Les classes concrètes ajoutent ensuite leurs règles métier spécifiques.
 * </p>
 *
 * <h3>Invariants métier minimaux</h3>
 * <ul>
 *     <li>Le nom et le prénom doivent être non vides.</li>
 *     <li>L’email peut être null mais, s’il est présent, doit contenir '@'.</li>
 *     <li>L’identifiant technique (id) est immuable une fois défini.</li>
 * </ul>
 */
public abstract class Personne {

    // ======================================================================
    // =============================== ATTRIBUTS =============================
    // ======================================================================

    /** Identifiant technique généré par la base de données. */
    private Long id;

    /** Nom de famille. */
    private String nom;

    /** Prénom de la personne. */
    private String prenom;

    /** Adresse email (optionnelle mais fortement recommandée). */
    private String email;

    /** Numéro de téléphone (optionnel). */
    private String telephone;

    // ======================================================================
    // ============================= CONSTRUCTEURS ===========================
    // ======================================================================

    /**
     * Constructeur vide requis pour la désérialisation ou l'ORM.
     *
     * <p>
     * Attention : l'objet peut être dans un état intermédiaire invalide
     * tant que tous les champs n'ont pas été initialisés.
     * </p>
     */
    public Personne() {
        super();
    }

    /**
     * Constructeur complet pour initialiser une personne dans un état valide.
     *
     * @param id identifiant technique (peut être null pour création)
     * @param nom nom de famille (obligatoire, non vide)
     * @param prenom prénom (obligatoire, non vide)
     * @param email email (optionnel, mais doit contenir '@' s'il est non null)
     * @param telephone numéro de téléphone (optionnel)
     */
    public Personne(Long id, String nom, String prenom, String email, String telephone) {
        // On passe par les setters pour réutiliser les validations unitaires.
        setId(id);
        setNom(nom);
        setPrenom(prenom);
        setEmail(email);
        setTelephone(telephone);

        // Vérification globale des invariants (par sécurité / lisibilité métier).
        validateInvariants();
    }

    // ======================================================================
    // ================================ MÉTIER ===============================
    // ======================================================================

    /**
     * Valide les invariants minimaux de la classe Personne.
     *
     * <p>
     * Cette méthode suppose que les champs ont déjà été initialisés via les setters.
     * Elle est surtout utile pour les constructeurs ou pour des vérifications
     * globales éventuelles côté domaine.
     * </p>
     *
     * @throws IllegalArgumentException si un invariant n’est pas respecté.
     */
    protected void validateInvariants() {
        // Ces vérifications doublonnent celles des setters, mais donnent
        // une vue "globale" de ce qu'on considère comme valide.
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom est obligatoire.");
        }
        if (prenom == null || prenom.isBlank()) {
            throw new IllegalArgumentException("Le prénom est obligatoire.");
        }
        if (email != null && !email.contains("@")) {
            throw new IllegalArgumentException("L'email doit contenir un '@'.");
        }
    }

    // ======================================================================
    // ============================ GETTERS/SETTERS ==========================
    // ======================================================================

    public Long getId() {
        return id;
    }

    /**
     * Définit l'identifiant technique généré par la persistance.
     *
     * <p>Règles :</p>
     * <ul>
     *     <li>Si {@code id} est null → ignoré (on laisse l'objet "non persisté").</li>
     *     <li>Si l'id est déjà défini → impossible de le modifier.</li>
     *     <li>Un id doit être strictement positif.</li>
     * </ul>
     */
    public void setId(Long id) {
        if (id == null) {
            return; // on ne force pas l'id si null
        }
        if (id <= 0) {
            throw new IllegalArgumentException("L'identifiant doit être > 0.");
        }
        if (this.id != null && !this.id.equals(id)) {
            throw new IllegalStateException("L'identifiant est déjà défini pour cette personne.");
        }
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    /**
     * Définit le nom de la personne.
     *
     * @param nom nom non null et non vide
     */
    public void setNom(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom est obligatoire.");
        }
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    /**
     * Définit le prénom de la personne.
     *
     * @param prenom prénom non null et non vide
     */
    public void setPrenom(String prenom) {
        if (prenom == null || prenom.isBlank()) {
            throw new IllegalArgumentException("Le prénom est obligatoire.");
        }
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Définit l'email de la personne.
     *
     * <p>
     * L'email peut être null (non renseigné). S'il est non null, il doit contenir '@'.
     * </p>
     *
     * @param email email ou null
     */
    public void setEmail(String email) {
        if (email != null && !email.contains("@")) {
            throw new IllegalArgumentException("L'email doit contenir un '@'.");
        }
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    /**
     * Définit le téléphone de la personne.
     *
     * <p>
     * Aucun invariant n'est imposé ici au niveau du domaine (validation
     * éventuelle côté UI ou service).
     * </p>
     */
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    // ======================================================================
    // ================================ UTILITAIRE ===========================
    // ======================================================================

    /**
     * Représentation textuelle simple sous la forme :
     * "Prénom Nom (email)".
     */
    @Override
    public String toString() {
        String base = (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
        return base.trim() + (email != null ? " (" + email + ")" : "");
    }
}