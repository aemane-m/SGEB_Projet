package application.domain.service;

/**
 * Service de gestion des comptes (création, authentification, navigation UI).
 * Couche Domaine — Gère uniquement les comptes Administrateurs.
 */
public interface AccountService {

    /** @return nombre total d'utilisateurs (admins) persistés. */
    long countUsers();

    /** * Crée un compte administrateur.
     * CORRIGÉ : Ajout de nom et prenom pour correspondre aux contraintes de la DB.
     */
    void createAdmin(String username, String rawPassword, String nom, String prenom);

    /**
     * Tente d'authentifier un utilisateur (admin).
     * @return un {@link LoginResult} indiquant succès/échec, message éventuel et identifiant.
     */
    LoginResult login(String username, String rawPassword);

    /**
     * Point d'extension pour déclencher la navigation UI après un login réussi.
     * (Implémentation côté adapter/UI, le domaine se contente de l’annoncer.)
     */
    void navigateToMainAfterLogin(LoginResult result);

    /**
     * Résultat d'authentification.
     * - success : indicateur de succès
     * - message : message d'erreur (échec) ou null (succès)
     * - username : identifiant de l'administrateur connecté (succès) sinon null
     */
    record LoginResult(boolean success, String message, String username) {

        /** Fabrique un résultat d'échec avec message explicite (jamais null). */
        public static LoginResult failure(String message) {
            String msg = (message == null || message.isBlank()) ? "Authentification échouée." : message;
            return new LoginResult(false, msg, null);
        }

        /** Fabrique un résultat de succès pour l'administrateur. */
        public static LoginResult success(String username) {
            return new LoginResult(true, null, username);
        }
    }
}