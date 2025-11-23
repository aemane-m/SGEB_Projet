package application.domain.core.security;

/**
 * <p>
 * <strong>Capability</strong> définit les permissions atomiques
 * (unités d’action) disponibles dans le système SGEB.
 * </p>
 *
 * <p>
 * Chaque {@link Role} agrège un ensemble de capabilities pour définir
 * le périmètre d’action d’un utilisateur (ex. Adhérent, Administrateur).
 * </p>
 *
 * @see Role
 */
public enum Capability {

    // ======================================================================
    // ===================== CAPACITÉS ADMIN / GESTION =======================
    // ======================================================================

    /** Afficher le tableau de bord administrateur (Dashboard). */
    VIEW_ADMIN_DASHBOARD("Afficher le tableau de bord administrateur"),

    /** Gérer les utilisateurs (comptes, rôles, activation, etc.). */
    MANAGE_USERS("Gérer les comptes utilisateurs et rôles"),

    /** Gérer les adhérents : création, modification, suppression, recherche. */
    MANAGE_ADHERENTS("Gérer les adhérents"),

    /** Gérer le catalogue de documents (livres, magazines, CD, etc.). */
    MANAGE_DOCUMENTS("Gérer le catalogue de documents"),

    /**
     * Alias plus « métier » pour {@link #MANAGE_DOCUMENTS}.
     * <p>
     * Utile côté UI lorsque l’on parle de « Catalogue » plutôt que de « Documents ».
     * </p>
     */
    MANAGE_CATALOGUE("Gérer le catalogue (alias de MANAGE_DOCUMENTS)"),

    /** Superviser les emprunts et retours de documents. */
    MANAGE_EMPRUNTS("Superviser les emprunts et retours"),

    /** Gérer les pénalités liées aux retards d’emprunts. */
    MANAGE_PENALITES("Gérer les pénalités d’emprunt"),

    /** Visualiser les statistiques globales du système. */
    VIEW_STATS("Visualiser les statistiques globales"),

    /** Consulter l’historique complet des emprunts (vue admin). */
    VIEW_HISTORY("Consulter l’historique des emprunts"),

    /** Configurer les paramètres système (durées, tarifs, etc.). */
    CONFIGURE_SYSTEM("Configurer le système"),

    // ======================================================================
    // ===================== CAPACITÉS ADHÉRENT / SELF-SERVICE ==============
    // ======================================================================

    /** Consulter le catalogue et les disponibilités. */
    CONSULT_CATALOG("Consulter le catalogue"),

    /** Emprunter un document (capacité d’un adhérent). */
    BORROW_DOCUMENT("Emprunter un document"),

    /** Retourner un document emprunté. */
    RETURN_DOCUMENT("Retourner un document"),

    /** Consulter ses propres emprunts (section « Mes emprunts »). */
    VIEW_SELF_LOANS("Consulter ses propres emprunts"),

    /** Consulter ses propres pénalités (section « Mes pénalités »). */
    VIEW_SELF_PENALTIES("Consulter ses propres pénalités");

    // ======================================================================
    // ============================== CHAMPS =================================
    // ======================================================================

    /** Description textuelle lisible de la capacité. */
    private final String description;

    Capability(String description) {
        this.description = description;
    }

    /**
     * @return description lisible de la capability (pour logs / UI).
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name() + " – " + description;
    }
}