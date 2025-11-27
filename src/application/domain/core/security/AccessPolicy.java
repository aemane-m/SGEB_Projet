package application.domain.core.security;

import java.util.Objects;

/**
 * <p>
 * <strong>AccessPolicy</strong> centralise les règles d'autorisation
 * basées sur un {@link Role} et ses {@link Capability}.
 * </p>
 *
 * <p>
 * Elle répond à la question :
 * <em>« Cet utilisateur a-t-il le droit d’effectuer cette action ? »</em>
 * </p>
 *
 * <p>
 * Classe utilitaire purement métier : aucun accès BD / réseau.
 * </p>
 */
public final class AccessPolicy {

    /** Classe utilitaire : pas d'instanciation. */
    private AccessPolicy() {
    }

    // ======================================================================
    // =========================== MÉTHODES GÉNÉRIQUES =======================
    // ======================================================================

    /**
     * Vérifie si un rôle possède la capability donnée.
     *
     * @param role       rôle à vérifier (non null)
     * @param capability capability requise (non null)
     * @return {@code true} si le rôle possède cette capability
     */
    public static boolean isAllowed(Role role, Capability capability) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(capability, "capability");
        return role.hasCapability(capability);
    }

    /**
     * Vérifie qu’un rôle possède la capability donnée, sinon lève une exception.
     *
     * @param role       rôle à vérifier (non null)
     * @param capability capability requise (non null)
     * @throws AccessDeniedException si la capability est absente
     */
    public static void require(Role role, Capability capability) {
        if (!isAllowed(role, capability)) {
            throw new AccessDeniedException(
                    "Accès refusé : capability requise = " + capability.name() +
                    ", rôle = " + role.getName()
            );
        }
    }

    // ======================================================================
    // ======================== RACCOURCIS SÉMANTIQUES ======================
    // ======================================================================

    /**
     * Peut gérer les adhérents ?
     *
     * @param role rôle à tester
     */
    public static boolean canManageAdherents(Role role) {
        return isAllowed(role, Capability.MANAGE_ADHERENTS);
    }

    /**
     * Peut gérer le catalogue de documents ?
     *
     * <p>
     * On autorise l'une ou l'autre capability pour être souple côté
     * libellés UI :
     * {@link Capability#MANAGE_DOCUMENTS} ou {@link Capability#MANAGE_CATALOGUE}.
     * </p>
     */
    public static boolean canManageDocuments(Role role) {
        return isAllowed(role, Capability.MANAGE_DOCUMENTS)
                || isAllowed(role, Capability.MANAGE_CATALOGUE);
    }

    /** Peut gérer / superviser les emprunts ? */
    public static boolean canManageEmprunts(Role role) {
        return isAllowed(role, Capability.MANAGE_EMPRUNTS);
    }

    /** Peut gérer / superviser les pénalités ? */
    public static boolean canManagePenalites(Role role) {
        return isAllowed(role, Capability.MANAGE_PENALITES);
    }

    /** Peut consulter les statistiques globales ? */
    public static boolean canViewStats(Role role) {
        return isAllowed(role, Capability.VIEW_STATS);
    }

    /** Peut consulter le catalogue (vue adhérent ou admin) ? */
    public static boolean canConsultCatalog(Role role) {
        return isAllowed(role, Capability.CONSULT_CATALOG);
    }

    /** Peut emprunter un document (côté autorisation) ? */
    public static boolean canBorrowDocument(Role role) {
        return isAllowed(role, Capability.BORROW_DOCUMENT);
    }


    /** Peut afficher le tableau de bord administrateur ? */
    public static boolean canViewAdminDashboard(Role role) {
        return isAllowed(role, Capability.VIEW_ADMIN_DASHBOARD);
    }

    /** Peut consulter ses propres emprunts (espace adhérent) ? */
    public static boolean canViewSelfLoans(Role role) {
        return isAllowed(role, Capability.VIEW_SELF_LOANS);
    }

    /** Peut consulter ses propres pénalités (espace adhérent) ? */
    public static boolean canViewSelfPenalties(Role role) {
        return isAllowed(role, Capability.VIEW_SELF_PENALTIES);
    }

    // ======================================================================
    // ======================= EXCEPTION DÉDIÉE =============================
    // ======================================================================

    /**
     * Exception lancée lorsqu’une autorisation est manquante.
     */
    public static class AccessDeniedException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public AccessDeniedException(String message) {
            super(message);
        }

        public AccessDeniedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}