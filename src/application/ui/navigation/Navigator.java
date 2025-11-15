package application.ui.navigation;

import application.domain.service.AccountService;

/**
 * Abstraction de la navigation UI.
 *
 * L’implémentation (FxNavigator) se charge de :
 *  - afficher le Shell principal,
 *  - charger les vues FXML,
 *  - injecter les ViewModels,
 *  - transporter le contexte utilisateur (session).
 */
public interface Navigator {

    /** Affiche le Shell principal si ce n'est pas déjà fait. */
    void showShell();

    /**
     * Navigation vers le Dashboard avec un contexte utilisateur.
     * Utilisée typiquement après un login réussi.
     *
     * @param session session utilisateur (peut être null si déjà positionnée auparavant)
     */
    void goToDashboard(AccountService.UserSession session);

    /**
     * Surcharge pratique : réutilise la session courante
     * mémorisée dans l’implémentation du Navigator.
     */
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

    // --- Écrans "hors shell" (pré-login) ---

    /** Navigue vers l'écran de connexion. */
    void goToLogin();

    /**
     * Navigue vers l'écran de création de compte adhérent.
     * (peut provisoirement pointer vers la même vue que Login)
     */
    void goToRegister();

    // --- Vues adhérent ---

    /** Vue adhérent : "Mes emprunts". */
    void goToMesEmprunts();

    /** Vue adhérent : "Mes pénalités". */
    void goToMesPenalites();
}