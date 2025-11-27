package application.ui.navigation;

import application.domain.service.AccountService;

/**
 * Abstraction de la navigation UI.
 *
 * L’implémentation (ex : {@code FxNavigator}) se charge de :
 *  - afficher le Shell principal,
 *  - charger les vues FXML,
 *  - injecter les ViewModels,
 *  - transporter le contexte utilisateur (session).
 */
public interface Navigator {

    void showShell();

    /**
     * Navigation vers le Dashboard avec un contexte utilisateur
     * (typiquement après un login réussi).
     *
     * @param session session utilisateur (peut être null si déjà positionnée)
     */
    void goToDashboard(AccountService.UserSession session);

    /** Surcharge pratique : réutilise la session courante mémorisée dans le Navigator. */
    default void goToDashboard() {
        goToDashboard(null);
    }

    /** Navigation vers le catalogue. */
    void goToCatalog();

    /** Navigation vers la gestion des adhérents (ADMIN). */
    void goToAdherents();

    /** Navigation vers la gestion des emprunts (ADMIN). */
    void goToLoans();

    /** Navigation vers la gestion des pénalités (ADMIN). */
    void goToPenalites();

    /** Navigation vers la gestion des comptes applicatifs (ADMIN). */
    void goToAccounts();

    // --- Vues adhérent ---

    /** Vue adhérent : "Mes emprunts". */
    void goToMesEmprunts();

    /** Vue adhérent : "Mes pénalités". */
    void goToMesPenalites();

    // --- Écrans "hors shell" (pré-login) ---

    /** Navigue vers l'écran de connexion. */
    void goToLogin();

    /**
     * Navigue vers l'écran de création de compte adhérent (inscription publique).
     */
    void goToRegister();

    // --- Changement de mot de passe imposé ---

    /**
     * Écran de changement de mot de passe lors de la première connexion
     * ou lorsqu'un flag "passwordMustChange" est activé.
     */
    void goToChangePassword(AccountService.UserSession session);

    // --- Déconnexion ---

    /**
     * Déconnexion de l'utilisateur :
     * - efface la session courante,
     * - revient à la Landing page (LandingView.fxml).
     */
    void logout();
}