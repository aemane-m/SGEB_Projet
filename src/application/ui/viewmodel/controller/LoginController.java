package application.ui.viewmodel.controller;

import application.domain.service.AccountService;
import application.ui.navigation.Navigator;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Method;
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

                // ⚠️ Nouveau : mot de passe à changer ?
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
                    navigator.showShell();             // ouvre MainShell.fxml si besoin
                    navigator.goToDashboard(session);  // dashboard + contexte utilisateur
                } else {
                    System.err.println("Navigator non injecté dans LoginController ; impossible de naviguer vers le Dashboard.");
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
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/application/ui/view/LandingView.fxml")
            );
            Parent root = loader.load();

            // Injection légère du service + navigator dans le contrôleur de la Landing
            Object controller = loader.getController();
            if (controller != null) {
                // setAccountService(AccountService)
                if (accountService != null) {
                    try {
                        Method m = controller.getClass()
                                .getMethod("setAccountService", AccountService.class);
                        m.invoke(controller, accountService);
                    } catch (NoSuchMethodException ignored) {
                        // la Landing ne gère peut-être pas l'AccountService, ce n'est pas grave
                    }
                }
                // setNavigator(Navigator)
                if (navigator != null) {
                    try {
                        Method m = controller.getClass()
                                .getMethod("setNavigator", Navigator.class);
                        m.invoke(controller, navigator);
                    } catch (NoSuchMethodException ignored) {
                        // la Landing ne gère peut-être pas le Navigator, ce n'est pas grave
                    }
                }
            }

            // On remplace la scène actuelle par la Landing
            Stage stage = (Stage) loginPane.getScene().getWindow();
            Scene scene = new Scene(root,
                    stage.getScene().getWidth(),
                    stage.getScene().getHeight());
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Impossible de revenir à l'accueil : " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur inattendue lors du retour à l'accueil.");
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