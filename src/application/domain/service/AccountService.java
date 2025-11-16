package application.domain.service;

import application.domain.core.security.Role;
import application.domain.core.security.Capability;

import java.util.List;
import java.util.Set;

/**
 * Service domaine pour la gestion des comptes (Admin + Adhérent).
 * Ne dépend d’aucune technologie (JavaFX, SQLite, UI).
 *
 * Le domaine fournit :
 * - création de comptes (setup, admin, adhérent)
 * - login authentifié
 * - informations de session (role + capabilities)
 * - état du compte : ACTIVE, PENDING, SUSPENDED
 * - flag passwordMustChange
 */
public interface AccountService {

    // =============================
    // ====== Création comptes =====
    // =============================

    /** Création du tout premier Administrateur. Utilisé par le Setup Wizard. */
    void createFirstAdmin(String email, String rawPassword, String nom, String prenom);

    /** Création d’un administrateur par un admin existant. */
    void createAdmin(String email, String rawPassword, String nom, String prenom);

    /** Création d’un adhérent par inscription publique. */
    void createAdherentPublic(String email, String rawPassword, String nom, String prenom);

    /** Retourne le nombre total de comptes en base. */
    long countUsers();

    // =============================
    // ========== Login ============
    // =============================

    /**
     * Authentifie un utilisateur (admin ou adhérent).
     *
     * @return LoginResult contenant :
     *         - succès/échec
     *         - message éventuel
     *         - UserSession si succès
     */
    LoginResult login(String email, String rawPassword);

    // =============================
    // ====== Mot de passe =========
    // =============================

    /**
     * Change le mot de passe de l'utilisateur et met le flag
     * "passwordMustChange" à false (utilisé après login avec mot de passe
     * temporaire).
     */
    void changePasswordAndClearFlag(long userId, String rawPassword);

    // =============================
    // ======== Gestion comptes ====
    // =============================

    /**
     * Résumé d’un compte (admin + adhérent) pour la gestion
     * dans l’interface d’administration.
     */
    record UserAccountSummary(
            long userId,
            String email,
            String roleName,          // "ADMIN" / "ADHERENT"
            String status,            // "ACTIVE" / "PENDING" / "SUSPENDED" ...
            boolean actif,            // vrai si le compte est activé
            boolean passwordMustChange,
            Long adherentId           // null pour un admin, id pour un adhérent
    ) { }

    /**
     * Liste tous les comptes (admin + adhérents).
     */
    List<UserAccountSummary> listAllAccounts();

    /**
     * Change le statut d’un compte (par ex. ACTIVE / SUSPENDED),
     * avec vérification métier :
     * - un utilisateur ne peut pas modifier son propre statut.
     *
     * @param targetUserId  id du compte dont on change le statut
     * @param newStatus     nouveau statut logique ("ACTIVE", "SUSPENDED", ...)
     * @param actingUserId  id de l'utilisateur qui effectue l'action
     */
    void changeAccountStatus(long targetUserId, String newStatus, long actingUserId);

    /**
     * Supprime un compte (admin ou adhérent) avec règles métier :
     * - un utilisateur ne peut pas supprimer son propre compte.
     *
     * @param targetUserId  id du compte à supprimer
     * @param actingUserId  id de l'utilisateur qui effectue l'action
     */
    void deleteAccount(long targetUserId, long actingUserId);

    // =============================
    // ===== Structures retour =====
    // =============================

    /**
     * Session utilisateur.
     *
     * @param userId             id technique du compte
     * @param email              email de connexion
     * @param role               rôle principal (ADMIN / ADHERENT / …)
     * @param capabilities       droits détaillés
     * @param passwordMustChange le mot de passe doit-il être changé à la prochaine connexion ?
     * @param adherentId         id de l'adhérent si le compte est lié à un adhérent, sinon null
     */
    record UserSession(
            long userId,
            String email,
            Role role,
            Set<Capability> capabilities,
            boolean passwordMustChange,
            Long adherentId
    ) { }

    /**
     * Résultat d'authentification.
     * success=true => session non nulle.
     * success=false => session=null + message d'erreur.
     */
    record LoginResult(boolean success, String message, UserSession session) {

        /** Échec avec message. */
        public static LoginResult failure(String message) {
            String msg = (message == null || message.isBlank())
                    ? "Authentification échouée."
                    : message;
            return new LoginResult(false, msg, null);
        }

        /** Succès avec session valide. */
        public static LoginResult success(UserSession session) {
            return new LoginResult(true, null, session);
        }
    }

    // =====================================================================
    // ========= Méthode de confort pour le Setup Wizard ===================
    // =====================================================================

    /**
     * Surcharge pratique pour le Setup Wizard qui ne demande qu'un "nom complet".
     * On découpe en (prenom, nom) de façon simple :
     * - tout sauf le dernier mot = prénom
     * - dernier mot = nom
     */
    default void createFirstAdmin(String fullName, String email, String rawPassword) {
        if (fullName == null) {
            fullName = "";
        }
        fullName = fullName.trim();

        if (fullName.isEmpty()) {
            // fallback : on considère tout comme nom
            createFirstAdmin(email, rawPassword, fullName, "");
            return;
        }

        int idx = fullName.lastIndexOf(' ');
        String nom;
        String prenom;

        if (idx > 0) {
            prenom = fullName.substring(0, idx).trim();
            nom = fullName.substring(idx + 1).trim();
        } else {
            // un seul mot => nom uniquement
            prenom = "";
            nom = fullName;
        }

        createFirstAdmin(email, rawPassword, nom, prenom);
    }
}