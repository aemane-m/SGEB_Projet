package application.domain.service;

import application.domain.core.security.Capability;
import application.domain.core.security.Role;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service de domaine pour la gestion des comptes utilisateur (Administrateur + Adhérent).
 *
 * <p>Ce service ne dépend d'aucune technologie (pas de JavaFX, pas de SQLite, pas d'API HTTP).
 * Il définit seulement la logique métier attendue pour :</p>
 *
 * <ul>
 *     <li>Créer des comptes (premier admin, autres admins, adhérents)</li>
 *     <li>Authentifier un utilisateur (login)</li>
 *     <li>Gérer les mots de passe (réinitialisation, obligation de changement)</li>
 *     <li>Consulter et modifier l’état des comptes (actif, suspendu, etc.)</li>
 * </ul>
 */
public interface AccountService {

    // ======================================================================
    // ======================== CRÉATION DE COMPTES ==========================
    // ======================================================================

    /**
     * Crée le tout premier administrateur du système.
     *
     * <p>Utilisé par l'assistant de configuration initiale (Setup Wizard).
     * Si ce compte n’existe pas encore, l’application ne peut pas être administrée.</p>
     *
     * @param email      adresse e-mail de connexion
     * @param rawPassword mot de passe en clair (sera hashé en implémentation)
     * @param nom        nom de famille
     * @param prenom     prénom
     */
    void createFirstAdmin(String email, String rawPassword, String nom, String prenom);

    /**
     * Crée un administrateur supplémentaire.
     *
     * <p>Cette méthode est appelée par un administrateur déjà connecté.</p>
     *
     * @param email      adresse e-mail de connexion
     * @param rawPassword mot de passe en clair
     * @param nom        nom de famille
     * @param prenom     prénom
     */
    void createAdmin(String email, String rawPassword, String nom, String prenom);

    /**
     * Crée un compte adhérent via une inscription publique
     * (ex. depuis un formulaire "Créer mon compte").
     *
     * @param email      adresse e-mail de connexion
     * @param rawPassword mot de passe en clair
     * @param nom        nom de famille
     * @param prenom     prénom
     */
    void createAdherentPublic(String email, String rawPassword, String nom, String prenom);

    /**
     * @return nombre total de comptes (admins + adhérents).
     */
    long countUsers();

    /**
     * Indique si au moins un compte administrateur existe déjà.
     *
     * @return {@code true} s'il existe au moins un admin
     */
    boolean hasAnyAdmin();

    // ======================================================================
    // ============================ AUTHENTIFICATION =========================
    // ======================================================================

    /**
     * Authentifie un utilisateur (admin ou adhérent) à partir de son e-mail
     * et de son mot de passe.
     *
     * @param email      e-mail saisi
     * @param rawPassword mot de passe saisi (en clair)
     * @return un {@link LoginResult} contenant :
     *         <ul>
     *             <li>success = true/false</li>
     *             <li>un message d'erreur éventuel</li>
     *             <li>une {@link UserSession} si succès</li>
     *         </ul>
     */
    LoginResult login(String email, String rawPassword);

    // ======================================================================
    // =========================== MOTS DE PASSE =============================
    // ======================================================================

    /**
     * Change le mot de passe d’un utilisateur et remet le flag
     * {@code passwordMustChange} à {@code false}.
     *
     * <p>Appelée après un premier login avec un mot de passe temporaire.</p>
     *
     * @param userId      identifiant du compte
     * @param rawPassword nouveau mot de passe (en clair)
     */
    void changePasswordAndClearFlag(long userId, String rawPassword);

    /**
     * Réinitialise le mot de passe d’un utilisateur (action d’un administrateur).
     *
     * @param targetUserId        identifiant du compte à modifier
     * @param rawPassword         nouveau mot de passe (en clair)
     * @param forcePasswordChange si {@code true}, l’utilisateur devra changer
     *                            ce mot de passe à sa prochaine connexion
     */
    void adminResetPassword(long targetUserId,
                            String rawPassword,
                            boolean forcePasswordChange);

    /**
     * Récupère le compte associé à un adhérent donné.
     *
     *
     * @param adherentId identifiant de l’adhérent
     * @return un résumé de compte si trouvé, vide sinon
     */
    Optional<UserAccountSummary> findAccountByAdherentId(long adherentId);
    
    
    /**
     * Recherche un utilisateur par email et retourne une UserSession
     * minimale pour le reset de mot de passe.
     */
    Optional<UserSession> findSessionForPasswordReset(String email);

    // ======================================================================
    // ========================= GESTION DES COMPTES =========================
    // ======================================================================

    /**
     * Résumé d’un compte (admin ou adhérent) utilisé dans les écrans
     * de gestion (interface d’administration).
     */
    record UserAccountSummary(
            long userId,
            String email,
            String roleName,          // "ADMIN" / "ADHERENT"
            String status,            // "ACTIVE" / "SUSPENDED" ...
            boolean actif,            // true si le compte est activé
            boolean passwordMustChange,
            Long adherentId           // null pour un admin, non null pour un adhérent
    ) { }

    /**
     * Liste tous les comptes existants (administrateurs et adhérents).
     *
     * @return liste des résumés de comptes
     */
    List<UserAccountSummary> listAllAccounts();

    /**
     * Modifie le statut d’un compte utilisateur (ex. ACTIVE → SUSPENDED).
     *
     * <p>Règle métier : un utilisateur ne peut pas modifier
     * son <em>propre</em> statut.</p>
     *
     * @param targetUserId identifiant du compte à modifier
     * @param newStatus    nouveau statut logique (ex. "ACTIVE", "SUSPENDED")
     * @param actingUserId identifiant de l’utilisateur qui effectue l’action
     */
    void changeAccountStatus(long targetUserId, String newStatus, long actingUserId);

    /**
     * Supprime un compte (admin ou adhérent).
     *
     * <p>Règle métier : un utilisateur ne peut pas supprimer
     * son <em>propre</em> compte.</p>
     *
     * @param targetUserId identifiant du compte à supprimer
     * @param actingUserId identifiant de l’utilisateur qui effectue l’action
     */
    void deleteAccount(long targetUserId, long actingUserId);

    // ======================================================================
    // ========================== STRUCTURES DE RETOUR =======================
    // ======================================================================

    /**
     * Représente une session utilisateur après un login réussi.
     *
     * <ul>
     *     <li>{@code userId} : identifiant du compte</li>
     *     <li>{@code email} : e-mail de connexion</li>
     *     <li>{@code role} : rôle principal (ADMIN, ADHERENT…)</li>
     *     <li>{@code capabilities} : liste des droits détaillés</li>
     *     <li>{@code passwordMustChange} : le mot de passe doit-il être changé ?</li>
     *     <li>{@code adherentId} : id de l’adhérent si le compte y est lié</li>
     * </ul>
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
     * Résultat d’une tentative de connexion (login).
     *
     * <p>Cas possibles :</p>
     * <ul>
     *     <li>succès : {@code success == true} et {@code session != null}</li>
     *     <li>échec : {@code success == false}, {@code session == null} et message renseigné</li>
     * </ul>
     */
    record LoginResult(boolean success, String message, UserSession session) {

        /**
         * Crée un résultat d’échec avec un message.
         *
         * @param message message d’erreur à afficher
         * @return un {@code LoginResult} en échec
         */
        public static LoginResult failure(String message) {
            String msg = (message == null || message.isBlank())
                    ? "Authentification échouée."
                    : message;
            return new LoginResult(false, msg, null);
        }

        /**
         * Crée un résultat de succès avec une session valide.
         *
         * @param session session utilisateur créée après login
         * @return un {@code LoginResult} en succès
         */
        public static LoginResult success(UserSession session) {
            return new LoginResult(true, null, session);
        }
    }

    // ======================================================================
    // ============ MÉTHODE DE CONFORT POUR LE PREMIER ADMIN =================
    // ======================================================================

    /**
     * Méthode utilitaire pour l’assistant de configuration (Setup Wizard) :
     * création du premier admin à partir d’un "nom complet".
     *
     * <p>Règle simple :</p>
     * <ul>
     *     <li>tout sauf le dernier mot = prénom</li>
     *     <li>dernier mot = nom</li>
     * </ul>
     *
     * <p>Exemple : "Marie Dupont" → prénom = "Marie", nom = "Dupont".</p>
     *
     * @param fullName    nom complet saisi (ex. "Marie Dupont")
     * @param email       e-mail de connexion
     * @param rawPassword mot de passe en clair
     */
    default void createFirstAdmin(String fullName, String email, String rawPassword) {
        if (fullName == null) {
            fullName = "";
        }
        fullName = fullName.trim();

        if (fullName.isEmpty()) {
            // Si rien n’est saisi, on passe un nom vide et un prénom vide.
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
            // Un seul mot → on le considère comme nom de famille.
            prenom = "";
            nom = fullName;
        }

        createFirstAdmin(email, rawPassword, nom, prenom);
    }
}