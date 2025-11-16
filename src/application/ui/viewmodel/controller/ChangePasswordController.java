package application.ui.viewmodel.controller;

import application.domain.service.AccountService;
import application.domain.service.AccountService.UserSession;
import application.ui.navigation.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Écran de changement de mot de passe imposé (première connexion).
 */
public final class ChangePasswordController {

    @FXML private Label lblUserInfo;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Button btnChange;
    @FXML private Button btnCancel;
    @FXML private ProgressIndicator progress;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private AccountService accountService;
    private UserSession currentSession;
    private Navigator navigator;   // <-- NEW

    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    // NEW : injection du Navigator
    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    /** Appelé par le Navigator après un login avec mot de passe à changer. */
    public void setSession(UserSession session) {
        this.currentSession = session;
        if (session != null && session.email() != null) {
            lblUserInfo.setText(
                "Bonjour " + session.email() +
                " — vous devez choisir un nouveau mot de passe avant de continuer."
            );
        }
    }

    @FXML
    private void initialize() {
        hideMessages();
    }

    @FXML
    private void onChangePassword() {
        hideMessages();

        if (accountService == null || currentSession == null) {
            showError("Contexte utilisateur invalide (erreur technique).");
            return;
        }

        String pwd  = txtNewPassword.getText() != null ? txtNewPassword.getText() : "";
        String pwd2 = txtConfirmPassword.getText() != null ? txtConfirmPassword.getText() : "";

        if (pwd.isBlank() || pwd2.isBlank()) {
            showError("Les deux champs sont obligatoires.");
            return;
        }
        if (!pwd.equals(pwd2)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }
        if (pwd.length() < 8) {
            showError("Le mot de passe doit contenir au moins 8 caractères.");
            return;
        }

        setBusy(true);

        new Thread(() -> {
            try {
                accountService.changePasswordAndClearFlag(currentSession.userId(), pwd);

                Platform.runLater(() -> {
                    showSuccess("Mot de passe mis à jour, redirection en cours...");
                    // Redirection immédiate vers le bon écran
                    if (navigator != null) {
                        navigator.goToDashboard(currentSession);
                    } else {
                        // fallback : fermer la fenêtre
                        txtNewPassword.getScene().getWindow().hide();
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> showError(
                        ex.getMessage() != null ? ex.getMessage() : ex.toString()
                ));
            } finally {
                Platform.runLater(() -> setBusy(false));
            }
        }).start();
    }

    @FXML
    private void onCancel() {
        // retour à l'écran de Login (ou fermeture)
        txtNewPassword.getScene().getWindow().hide();
    }

    private void setBusy(boolean busy) {
        btnChange.setDisable(busy);
        btnCancel.setDisable(busy);
        progress.setVisible(busy);
        progress.setManaged(busy);
    }

    private void hideMessages() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        successLabel.setText("");
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        successLabel.setText("");
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successLabel.setVisible(true);
        successLabel.setManaged(true);

        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}