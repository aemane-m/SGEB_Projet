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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SignupAdherentController {

    // ====== Root ======
    @FXML
    private VBox rootPane;

    // ====== Champs formulaire ======
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private TextField prenomField;

    @FXML
    private TextField nomField;

    @FXML
    private Button btnCreate;

    // ⚠️ DOIT s'appeler comme dans le FXML : fx:id="btnBack"
    @FXML
    private Button btnBack;

    @FXML
    private Button btnGotoLogin;

    @FXML
    private Label infoLabel;

    @FXML
    private Label errorLabel;

    @FXML
    private ProgressIndicator progress;

    // ====== Services injectés ======
    private AccountService accountService;
    private Navigator navigator;

    private final BooleanProperty loading = new SimpleBooleanProperty(false);

    // ------------------------------------------------
    // Injection des dépendances
    // ------------------------------------------------

    public void setAccountService(AccountService svc) {
        this.accountService = Objects.requireNonNull(svc, "AccountService ne peut pas être null");
    }

    public void setNavigator(Navigator navigator) {
        this.navigator = Objects.requireNonNull(navigator, "Navigator ne peut pas être null");
    }

    // ------------------------------------------------
    // Initialisation JavaFX
    // ------------------------------------------------

    @FXML
    private void initialize() {
        // Formulaire invalide si :
        // - email vide
        // - mot de passe vide ou < 6
        // - confirmation vide ou différente
        // - prénom/nom vides
        BooleanBinding invalidForm = Bindings.createBooleanBinding(
                () -> isBlank(emailField.getText())
                        || isBlank(passwordField.getText())
                        || isBlank(confirmPasswordField.getText())
                        || passwordField.getText().length() < 6
                        || !Objects.equals(passwordField.getText(), confirmPasswordField.getText())
                        || isBlank(prenomField.getText())
                        || isBlank(nomField.getText()),
                emailField.textProperty(),
                passwordField.textProperty(),
                confirmPasswordField.textProperty(),
                prenomField.textProperty(),
                nomField.textProperty()
        );

        btnCreate.disableProperty().bind(invalidForm.or(loading));

        if (btnBack != null) {
            btnBack.disableProperty().bind(loading);
        }
        if (btnGotoLogin != null) {
            btnGotoLogin.disableProperty().bind(loading);
        }

        if (progress != null) {
            progress.visibleProperty().bind(loading);
            progress.managedProperty().bind(loading);
        }

        clearMessages();
    }

    // ------------------------------------------------
    // Actions boutons
    // ------------------------------------------------

    @FXML
    private void onCreateClicked() {
        clearMessages();

        if (accountService == null) {
            showError("Service de comptes non initialisé.");
            return;
        }

        final String email  = trim(emailField.getText());
        final String pass   = passwordField.getText();
        final String prenom = trim(prenomField.getText());
        final String nom    = trim(nomField.getText());

        // ⚠️ IMPORTANT : ordre des paramètres dans AccountService :
        // createAdherentPublic(String email, String rawPassword, String nom, String prenom)
        runSafely(
                () -> accountService.createAdherentPublic(email, pass, nom, prenom),
                ex -> showError(msg(ex)),
                () -> showInfo("Compte adhérent créé avec succès. Vous pouvez maintenant vous connecter.")
        );
    }

    @FXML
    private void onBackToLandingClicked() {
        switchScene("/application/ui/view/LandingView.fxml", controller -> {
            if (controller instanceof LandingController landingCtrl) {
                if (accountService != null) landingCtrl.setAccountService(accountService);
                if (navigator != null) landingCtrl.setNavigator(navigator);
            }
        });
    }

    @FXML
    private void onGotoLoginClicked() {
        switchScene("/application/ui/view/LoginView.fxml", controller -> {
            if (controller instanceof LoginController loginCtrl) {
                if (accountService != null) loginCtrl.setAccountService(accountService);
                if (navigator != null) loginCtrl.setNavigator(navigator);
            }
        });
    }

    // ------------------------------------------------
    // Helpers scène
    // ------------------------------------------------

    private void switchScene(String fxmlPath, Consumer<Object> controllerConfigurator) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null && controllerConfigurator != null) {
                controllerConfigurator.accept(controller);
            }

            // On récupère un Node quelconque pour accéder à la scène
            Stage stage;
            if (rootPane != null && rootPane.getScene() != null) {
                stage = (Stage) rootPane.getScene().getWindow();
            } else if (btnCreate != null && btnCreate.getScene() != null) {
                stage = (Stage) btnCreate.getScene().getWindow();
            } else {
                throw new IllegalStateException("Impossible de trouver la fenêtre courante pour switchScene.");
            }

            Scene scene = stage.getScene();
            if (scene == null) {
                // Si pas de scène existante, on en crée une nouvelle
                stage.setScene(new Scene(root));
            } else {
                // On garde la taille courante
                Scene newScene = new Scene(root, scene.getWidth(), scene.getHeight());
                stage.setScene(newScene);
            }
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Impossible de changer d'écran : " + e.getMessage());
        }
    }

    // ------------------------------------------------
    // Helpers async / UI
    // ------------------------------------------------

    private void runSafely(Runnable operation,
                           Consumer<Throwable> onError,
                           Runnable onSuccess) {
        loading.set(true);
        CompletableFuture
                .runAsync(operation)
                .whenComplete((result, exception) -> Platform.runLater(() -> {
                    loading.set(false);
                    if (exception != null) {
                        onError.accept(exception);
                    } else if (onSuccess != null) {
                        onSuccess.run();
                    }
                }));
    }

    private void clearMessages() {
        if (infoLabel != null) {
            infoLabel.setText("");
            infoLabel.setVisible(false);
            infoLabel.setManaged(false);
        }
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private void showInfo(String txt) {
        if (infoLabel != null) {
            infoLabel.setText(txt);
            infoLabel.setVisible(true);
            infoLabel.setManaged(true);
        }
    }

    private void showError(String txt) {
        if (errorLabel != null) {
            errorLabel.setText("Erreur : " + txt);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    // ------------------------------------------------
    // Utils
    // ------------------------------------------------

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trim(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static String msg(Throwable t) {
        if (t == null) return "Erreur inconnue";
        Throwable cause = (t instanceof java.util.concurrent.CompletionException && t.getCause() != null)
                ? t.getCause()
                : t;
        return (cause.getMessage() != null && !cause.getMessage().isBlank())
                ? cause.getMessage()
                : cause.toString();
    }
}