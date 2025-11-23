package application.ui.viewmodel.controller;

import application.ui.navigation.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

/**
 * Contrôleur de l'écran d'accueil ("Landing").
 *
 * - Ne fait plus que déléguer la navigation au {@link Navigator}.
 * - Le choix de la vue (Login, Signup, etc.) et l'injection des services
 *   sont entièrement gérés par le Navigator (FxNavigator).
 */
public final class LandingController {

    @FXML
    private BorderPane root;

    @FXML
    private Button btnLogin;

    @FXML
    private Button btnRegister;

    /** Point d'entrée unique pour la navigation. */
    private Navigator navigator;

    // ====================================================================
    // Injection
    // ====================================================================

    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    // ====================================================================
    // Initialisation FXML
    // ====================================================================

    @FXML
    private void initialize() {
        // Rien de particulier à initialiser pour le moment.
    }

    // ====================================================================
    // Actions
    // ====================================================================

    @FXML
    private void onLoginClicked() {
        if (navigator == null) {
            showNavError("Impossible d'ouvrir l'écran de connexion : Navigator non initialisé.");
            return;
        }
        navigator.goToLogin();
    }

    @FXML
    private void onRegisterClicked() {
        if (navigator == null) {
            showNavError("Impossible d'ouvrir l'écran d'inscription : Navigator non initialisé.");
            return;
        }
        navigator.goToRegister();
    }

    // ====================================================================
    // Helper
    // ====================================================================

    private void showNavError(String message) {
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setTitle("Erreur de navigation");
        error.setHeaderText(null);
        error.setContentText(message);
        error.showAndWait();
    }
}