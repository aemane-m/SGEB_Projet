package application.ui.viewmodel.controller;

import application.domain.service.AccountService;
import application.ui.navigation.Navigator;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Contrôleur de l'écran de connexion.
 *
 * - Le 1er admin est créé via le SetupWizard, pas ici.
 * - Cet écran sert uniquement à authentifier un utilisateur
 *   puis à déléguer la navigation au Navigator (shell + dashboard).
 */
public class LoginController {

    // --- Panneau principal ---
    @FXML private VBox loginPane;

    // --- Zone Login ---
    @FXML private TextField usernameField;   // email de connexion
    @FXML private PasswordField passwordField;
    @FXML private Button btnLogin;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator progress;
    @FXML private Hyperlink lnkForgotPassword;


    // --- Bouton retour à la Landing ---
    @FXML private Button btnBackToLanding;

    // --- Service injecté (domaine) ---
    private AccountService accountService;
    

    // --- Navigation injectée (UI) ---
    private Navigator navigator;

    // --- État interne ---
    private final BooleanProperty loading = new SimpleBooleanProperty(false);

    public LoginController() {
        // requis par JavaFX
    }

    // ===================================================================
    // Injections
    // ===================================================================

    public void setAccountService(AccountService svc) {
        this.accountService = Objects.requireNonNull(svc, "AccountService ne peut pas être null");
    }

    public void setNavigator(Navigator navigator) {
        this.navigator = Objects.requireNonNull(navigator, "Navigator ne peut pas être null");
    }

    // ===================================================================
    // Initialisation JavaFX
    // ===================================================================

    @FXML
    private void initialize() {
        // --- Validation du formulaire de login ---
        BooleanBinding loginFieldsEmpty = Bindings.createBooleanBinding(
                () -> isBlank(usernameField.getText()) || isBlank(passwordField.getText()),
                usernameField.textProperty(), passwordField.textProperty()
        );
        btnLogin.disableProperty().bind(loginFieldsEmpty.or(loading));

        // --- Bouton retour désactivé pendant le chargement ---
        if (btnBackToLanding != null) {
            btnBackToLanding.disableProperty().bind(loading);
        }
        if (lnkForgotPassword != null) {
            lnkForgotPassword.disableProperty().bind(loading);
            lnkForgotPassword.setOnAction(e -> onForgotPasswordClicked());
        }    

        // --- Spinner de chargement ---
        progress.visibleProperty().bind(loading);
        progress.managedProperty().bind(loading);

        // --- Actions boutons ---
        btnLogin.setOnAction(e -> onLoginClicked());

        clearMessages();
    }

    // ===================================================================
    // Login
    // ===================================================================

    @FXML
    private void onLoginClicked() {
        clearMessages();

        if (accountService == null) {
            showError("Service d'authentification non initialisé.");
            return;
        }

        final String email = trim(usernameField.getText());
        final String pass  = passwordField.getText();

        // On évite de lancer une requête vide si jamais le binding a été cassé
        if (isBlank(email) || isBlank(pass)) {
            showError("Veuillez renseigner votre email et votre mot de passe.");
            return;
        }

        runSafely(() -> {
            // exécution côté worker thread
            AccountService.LoginResult result = accountService.login(email, pass);

            // retour sur le thread JavaFX pour mettre à jour l'UI
            Platform.runLater(() -> {
                if (!result.success()) {
                    showError(result.message());
                    passwordField.clear();
                    return;
                }

                AccountService.UserSession session = result.session();
                if (session == null) {
                    showError("Erreur interne : session manquante après authentification.");
                    return;
                }

                // Mot de passe à changer ?
                if (session.passwordMustChange()) {
                    if (navigator != null) {
                        navigator.goToChangePassword(session);
                    } else {
                        System.err.println("Navigator non injecté ; impossible d'ouvrir l'écran de changement de mot de passe.");
                        showError("Erreur technique : navigation indisponible.");
                    }
                    return;
                }

                // Chemin normal : on ouvre le Shell + Dashboard
                if (navigator != null) {
                    // goToDashboard gère déjà showShell() en interne
                    navigator.goToDashboard(session);
                } else {
                    System.err.println("Navigator non injecté dans LoginController ; impossible de naviguer vers le Dashboard.");
                    showError("Erreur technique : navigation indisponible.");
                }
            });
        }, ex -> showError("Erreur : " + msg(ex)));
    }
    // ===================================================================
    // Mot de passe oublié
    // ===================================================================

    @FXML
    private void onForgotPasswordClicked() {
        clearMessages();

        if (accountService == null) {
            showError("Service d'authentification non initialisé.");
            return;
        }

        // On propose l'email déjà saisi, si présent
        TextInputDialog dialog = new TextInputDialog(trim(usernameField.getText()));
        dialog.setTitle("Mot de passe oublié");
        dialog.setHeaderText("Saisissez votre adresse email.");
        dialog.setContentText("Email :");

        var result = dialog.showAndWait();
        if (result.isEmpty()) {
            return; // annulé
        }

        String email = trim(result.get());
        if (isBlank(email)) {
            showError("Veuillez saisir une adresse email.");
            return;
        }

        runSafely(() -> {
            var optSession = accountService.findSessionForPasswordReset(email);

            Platform.runLater(() -> {
                if (optSession.isEmpty()) {
                    showError("Aucun compte ne correspond à cet email.");
                    return;
                }

                var session = optSession.get();

                if (navigator != null) {
                    // On réutilise l’écran de changement de mot de passe
                    navigator.goToChangePassword(session);
                } else {
                    showError("Erreur technique : navigation indisponible.");
                }
            });
        }, ex -> showError("Erreur : " + msg(ex)));
    }
    
    
    // ===================================================================
    // Retour à la Landing
    // ===================================================================

    /**
     * Appelé par le bouton "Retour à l'écran d'accueil" (Landing).
     * FXML : onAction="#onBackToLandingClicked"
     */
    @FXML
    private void onBackToLandingClicked() {
        // On délègue complètement la logique au Navigator, qui sait
        // comment afficher la Landing + réinitialiser le Shell.
        if (navigator != null) {
            navigator.logout();
        } else {
            // Fallback : message d'erreur (cas théorique)
            showError("Navigation indisponible (navigator non injecté).");
        }
    }

    // ===================================================================
    // Helpers UI / Async
    // ===================================================================

    private void runSafely(Runnable operation, Consumer<Throwable> errorHandler) {
        Platform.runLater(() -> loading.set(true));
        CompletableFuture
                .runAsync(operation)
                .whenComplete((result, exception) -> {
                    Platform.runLater(() -> {
                        if (exception != null) {
                            errorHandler.accept(exception);
                        }
                        loading.set(false);
                    });
                });
    }

    // --- Messages / erreurs ---

    private void clearMessages() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            errorLabel.setText("");
        }
    }

    private void showError(String txt) {
        if (errorLabel != null) {
            errorLabel.setText(txt);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    // --- Utils ---

    private static String msg(Throwable t) {
        if (t == null) return "Erreur inconnue";
        Throwable cause = (t instanceof java.util.concurrent.CompletionException && t.getCause() != null)
                ? t.getCause()
                : t;
        return (cause.getMessage() != null && !cause.getMessage().isBlank())
                ? cause.getMessage()
                : cause.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trim(String s) {
        return (s == null) ? "" : s.trim();
    }
}