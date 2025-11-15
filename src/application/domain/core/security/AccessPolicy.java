package application.domain.core.security;

import java.util.Objects;

/**
 * <p><strong>AccessPolicy</strong> centralise les règles d'autorisation
 * basées sur un {@link Role} et ses {@link Capability}.</p>
 *
 * <p>Répond à la question :
 * <em>"l'utilisateur a-t-il le droit d'effectuer cette action ?"</em></p>
 */
public final class AccessPolicy {

    private AccessPolicy() {
        // utilitaire
    }

    // ===========================
    // === Méthodes génériques ===
    // ===========================

    /**
     * Vérifie si un rôle possède la capability donnée.
     */
    public static boolean isAllowed(Role role, Capability capability) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(capability, "capability");
        return role.hasCapability(capability);
    }

    /**
     * Variante qui lève une exception en cas d'accès interdit.
     */
    public static void require(Role role, Capability capability) {
        if (!isAllowed(role, capability)) {
            throw new AccessDeniedException(
                "Accès refusé : capability requise = " + capability.name() +
                ", role = " + role.getNom()
            );
        }
    }

    // ===============================
    // === Raccourcis sémantiques ===
    // ===============================

    /** Peut gérer les adhérents ? */
    public static boolean canManageAdherents(Role role) {
        return isAllowed(role, Capability.MANAGE_ADHERENTS);
    }

    /** Peut gérer les documents (catalogue) ? */
    public static boolean canManageDocuments(Role role) {
        // On autorise l'un ou l'autre pour être souple
        return isAllowed(role, Capability.MANAGE_DOCUMENTS)
                || isAllowed(role, Capability.MANAGE_CATALOGUE);
    }

    /** Peut gérer/superviser les emprunts ? */
    public static boolean canManageEmprunts(Role role) {
        return isAllowed(role, Capability.MANAGE_EMPRUNTS);
    }

    /** Peut gérer/superviser les pénalités ? */
    public static boolean canManagePenalites(Role role) {
        return isAllowed(role, Capability.MANAGE_PENALITES);
    }

    /** Peut consulter les statistiques globales ? */
    public static boolean canViewStats(Role role) {
        return isAllowed(role, Capability.VIEW_STATS);
    }

    /** Peut consulter le catalogue (vue adhérent/admin) ? */
    public static boolean canConsultCatalog(Role role) {
        return isAllowed(role, Capability.CONSULT_CATALOG);
    }

    /** Peut emprunter un document (côté autorisation) ? */
    public static boolean canBorrowDocument(Role role) {
        return isAllowed(role, Capability.BORROW_DOCUMENT);
    }

    /** Peut retourner un document (côté autorisation) ? */
    public static boolean canReturnDocument(Role role) {
        return isAllowed(role, Capability.RETURN_DOCUMENT);
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

    // ===========================
    // === Exception dédiée   ====
    // ===========================

    /**
     * Exception lancée lorsqu'une autorisation est manquante.
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