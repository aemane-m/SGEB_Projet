package application.domain.core.security;

/**
 * <p><strong>Capability</strong> définit les permissions atomiques
 * (unités d’action) disponibles dans le système SGEB.</p>
 *
 * <p>Chaque {@link application.domain.core.security.Role} agrège un
 * ensemble de capabilities pour définir les actions autorisées
 * d’un utilisateur donné (ex. Adhérent, Bibliothécaire, Administrateur).</p>
 *
 * <p>Exemples :
 * <ul>
 *   <li>{@link #BORROW_DOCUMENT} : emprunter un document</li>
 *   <li>{@link #MANAGE_DOCUMENTS} : gérer le catalogue</li>
 *   <li>{@link #VIEW_STATS} : visualiser les statistiques</li>
 * </ul>
 * </p>
 *
 * @author SGEB
 * @version 1.0
 */
public enum Capability {

    /** Gérer les adhérents : création, modification, suppression, recherche */
    MANAGE_ADHERENTS("Gérer les adhérents"),

    /** Gérer les documents du catalogue (livres, magazines, CD, etc.) */
    MANAGE_DOCUMENTS("Gérer le catalogue de documents"),

    /** Superviser les emprunts et retours de documents */
    MANAGE_EMPRUNTS("Superviser les emprunts et retours"),

    /** Gérer les pénalités liées aux retards d’emprunts */
    MANAGE_PENALITES("Gérer les pénalités d’emprunt"),

    /** Emprunter un document (capacité d’un adhérent) */
    BORROW_DOCUMENT("Emprunter un document"),

    /** Retourner un document emprunté */
    RETURN_DOCUMENT("Retourner un document"),

    /** Consulter le catalogue et les disponibilités */
    CONSULT_CATALOG("Consulter le catalogue"),

    /** Visualiser les statistiques globales du système */
    VIEW_STATS("Visualiser les statistiques globales"),

    /** Consulter l’historique complet des emprunts */
    VIEW_HISTORY("Consulter l’historique des emprunts"),

    /** Gérer les utilisateurs et leurs rôles */
    MANAGE_USERS("Gérer les comptes utilisateurs et rôles"),

    /** Configurer les paramètres système (durées, tarifs, etc.) */
    CONFIGURE_SYSTEM("Configurer le système");

    // === Champ descriptif ===
    private final String description;

    Capability(String description) {
        this.description = description;
    }

    /**
     * Retourne la description textuelle de la capacité.
     *
     * @return description lisible de la capacité
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name() + " – " + description;
    }
}
