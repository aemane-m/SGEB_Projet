package application.domain.core.security;

import java.util.Objects;

/**
 * <p><strong>AccessPolicy</strong> centralise les règles d'autorisation
 * basées sur un {@link Role} et ses {@link Capability}.</p>
 *
 * <p>Cette classe répond à la question : <em>"l'utilisateur a-t-il le droit
 * d'effectuer cette action ?"</em> et ne traite pas les règles métier
 * supplémentaires (ex. limite d'emprunts, retards), qui restent dans
 * les entités/services du domaine (ex. {@code Adherent.peutEmprunter(asOf)}).</p>
 *
 * <h3>Exemples d'utilisation</h3>
 * <pre>{@code
 * Role admin = Role.of("ADMIN");
 * admin.addCapability(Capability.MANAGE_DOCUMENTS);
 *
 * if (AccessPolicy.canManageDocuments(admin)) {
 *     // Exécuter la commande de gestion de documents
 * } else {
 *     throw new AccessPolicy.AccessDeniedException("Droit MANAGE_DOCUMENTS requis.");
 * }
 * }</pre>
 *
 * <h3>Conception</h3>
 * <ul>
 *   <li>Classe utilitaire finale avec méthodes statiques</li>
 *   <li>Ne dépend que du domaine (Role/Capability)</li>
 *   <li>Exception interne dédiée : {@link AccessDeniedException}</li>
 * </ul>
 *
 * @author SGEB
 * @version 1.0
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
     *
     * @param role rôle à tester (non nul)
     * @param capability capability requise (non nulle)
     * @return {@code true} si autorisé, sinon {@code false}
     */
    public static boolean isAllowed(Role role, Capability capability) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(capability, "capability");
        return role.hasCapability(capability);
    }

    /**
     * Variante qui lève une exception en cas d'accès interdit.
     *
     * @param role rôle à vérifier
     * @param capability capability requise
     * @throws AccessDeniedException si le rôle ne possède pas la capability
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
        return isAllowed(role, Capability.MANAGE_DOCUMENTS);
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

    /** Peut consulter le catalogue ? */
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

    // ===========================
    // === Exception dédiée   ====
    // ===========================

    /**
     * Exception lancée lorsqu'une autorisation est manquante.
     * C'est une unchecked exception (RuntimeException) volontairement
     * légère pour l'usage côté service.
     */
    public static class AccessDeniedException extends RuntimeException {

        /** Identifiant de version de sérialisation */
        private static final long serialVersionUID = 1L;

        public AccessDeniedException(String message) {
            super(message);
        }

        public AccessDeniedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
