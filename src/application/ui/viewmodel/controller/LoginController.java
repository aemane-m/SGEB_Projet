package application.ui.viewmodel.controller;

import application.domain.service.AccountService; 
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox; 
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty; 

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class LoginController {

    // --- Panneaux ---
    @FXML private VBox loginPane;
    @FXML private VBox createPane;

    // --- Zone Login ---
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button btnLogin;
    @FXML private Button btnShowCreate;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator progress;

    // --- Formulaire de création ---
    @FXML private TextField createUsernameField;
    @FXML private PasswordField createPasswordField;
    @FXML private TextField createPrenomField; // <-- CORRIGÉ (Ajouté)
    @FXML private TextField createNomField;    // <-- CORRIGÉ (Ajouté)
    
    @FXML private Button btnCreate;
    @FXML private Button btnCancelCreate;
    @FXML private Label createInfoLabel;
    @FXML private Label createErrorLabel;

    // --- Service injecté (domaine) ---
    private AccountService accountService;
    
    // --- État interne ---
    private final BooleanProperty loading = new SimpleBooleanProperty(false);

    public LoginController() { }

    public void setAccountService(AccountService svc) {
        this.accountService = Objects.requireNonNull(svc, "AccountService cannot be null");
        Platform.runLater(this::detectFirstRunSafely);
    }


    @FXML
    private void initialize() {
        // --- Bindings ---
        BooleanBinding loginFieldsEmpty = Bindings.createBooleanBinding(
                () -> isBlank(usernameField.getText()) || isBlank(passwordField.getText()),
                usernameField.textProperty(), passwordField.textProperty()
        );
        btnLogin.disableProperty().bind(loginFieldsEmpty.or(loading));

        // CORRIGÉ : Le binding de validation inclut maintenant nom et prenom
        BooleanBinding createFieldsInvalid = Bindings.createBooleanBinding(
                () -> isBlank(createUsernameField.getText())
                        || createPasswordField.getText() == null
                        || createPasswordField.getText().length() < 6
                        || isBlank(createPrenomField.getText()) // <-- CORRIGÉ
                        || isBlank(createNomField.getText()),    // <-- CORRIGÉ
                createUsernameField.textProperty(), 
                createPasswordField.textProperty(),
                createPrenomField.textProperty(), // <-- CORRIGÉ
                createNomField.textProperty()     // <-- CORRIGÉ
        );
        btnCreate.disableProperty().bind(createFieldsInvalid.or(loading));
        
        btnCancelCreate.disableProperty().bind(loading);
        btnShowCreate.disableProperty().bind(loading);

        // --- Actions ---
        btnLogin.setOnAction(e -> onLoginClicked());
        btnCreate.setOnAction(e -> onCreateClicked()); // <- Cette méthode est corrigée
        btnShowCreate.setOnAction(e -> toggleCreatePane(true));
        btnCancelCreate.setOnAction(e -> toggleCreatePane(false));

        // --- État Initial ---
        progress.visibleProperty().bind(loading);
        progress.managedProperty().bind(loading);
        toggleCreatePane(false); 
        clearMessages();
        clearCreateMessages();
    }

    /**
     * CORRIGÉ: L'action de création envoie maintenant Nom et Prénom.
     */
    @FXML
    private void onCreateClicked() {
        clearCreateMessages();
        if (accountService == null) { showCreateError("Service d'authentification non initialisé."); return; }

        // Récupération des nouvelles valeurs
        final String user = trim(createUsernameField.getText());
        final String pass = createPasswordField.getText();
        final String prenom = trim(createPrenomField.getText()); // <-- CORRIGÉ
        final String nom = trim(createNomField.getText());       // <-- CORRIGÉ

        runSafely(() -> { 
            // Appel de la nouvelle signature de méthode
            accountService.createAdmin(user, pass, nom, prenom); 
            
            Platform.runLater(() -> {
                showCreateInfo("Compte administrateur créé. Vous pouvez vous connecter.");
                toggleCreatePane(false); // Revient au login
            });
        }, ex -> showCreateError("Erreur: " + msg(ex)));
    }

    // ===================================================================
    // COLLER LE RESTE DES MÉTHODES HELPERS ICI (non modifiées)
    // ===================================================================
    private void toggleCreatePane(boolean show) {
        if (createPane != null && loginPane != null) {
            createPane.setVisible(show); createPane.setManaged(show);
            loginPane.setVisible(!show); loginPane.setManaged(!show);
            if (show) {
                clearCreateMessages();
                if (createUsernameField != null && isBlank(createUsernameField.getText())) createUsernameField.requestFocus();
            } else {
                clearMessages();
                if (usernameField != null && isBlank(usernameField.getText())) usernameField.requestFocus();
            }
        }
    }
    private void detectFirstRunSafely() {
        if (accountService == null) return;
        runSafely(() -> {
            long users = accountService.countUsers();
            boolean firstRun = (users == 0);
            Platform.runLater(() -> toggleCreatePane(firstRun));
        }, ex -> showError("Erreur de détection: " + msg(ex)));
    }
    @FXML private void onLoginClicked() {
        clearMessages();
        if (accountService == null) { showError("Service d'authentification non initialisé."); return; }
        final String user = trim(usernameField.getText());
        final String pass = passwordField.getText();
        runSafely(() -> {
            var result = accountService.login(user, pass);
            Platform.runLater(() -> {
                if (!result.success()) showError(result.message());
                else accountService.navigateToMainAfterLogin(result);
            });
        }, ex -> showError("Erreur: " + msg(ex)));
    }
    private void runSafely(Runnable operation, Consumer<Throwable> errorHandler) {
        Platform.runLater(() -> loading.set(true)); 
        CompletableFuture.runAsync(operation)
                .whenComplete((result, exception) -> {
                    Platform.runLater(() -> {
                        if (exception != null) errorHandler.accept(exception);
                        loading.set(false);
                    });
                });
    }
    private void clearMessages() { if (errorLabel != null) { errorLabel.setVisible(false); errorLabel.setText(""); } }
    private void showError(String txt) { if (errorLabel != null) { errorLabel.setText(txt); errorLabel.setVisible(true); errorLabel.setManaged(true); } }
    private void clearCreateMessages() { if (createInfoLabel != null) { createInfoLabel.setVisible(false); createInfoLabel.setText(""); } if (createErrorLabel != null) { createErrorLabel.setVisible(false); createErrorLabel.setText(""); } }
    private void showCreateInfo(String txt) { if (createInfoLabel != null) { createInfoLabel.setText(txt); createInfoLabel.setVisible(true); createInfoLabel.setManaged(true); } }
    private void showCreateError(String txt) { if (createErrorLabel != null) { createErrorLabel.setText(txt); createErrorLabel.setVisible(true); createErrorLabel.setManaged(true); } }
    private static String msg(Throwable t) { if (t == null) return "Erreur inconnue"; Throwable cause = (t instanceof java.util.concurrent.CompletionException && t.getCause() != null) ? t.getCause() : t; return (cause.getMessage() != null && !cause.getMessage().isBlank()) ? cause.getMessage() : cause.toString(); }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String trim(String s) { return s == null ? "" : s.trim(); }
    // ===================================================================
}