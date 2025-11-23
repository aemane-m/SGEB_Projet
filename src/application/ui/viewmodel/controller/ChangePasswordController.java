package application.ui.viewmodel.controller;

import application.domain.service.AccountService;
import application.domain.service.AccountService.UserSession;
import application.ui.navigation.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Écran de changement de mot de passe imposé (première connexion).
 *
 * - Affiche un message personnalisé avec l'email de l'utilisateur.
 * - Valide le nouveau mot de passe côté UI (champs non vides, longueur, confirmation).
 * - Appelle {@link AccountService#changePasswordAndClearFlag(long, String)} en asynchrone.
 * - En cas de succès, redirige via le {@link Navigator} vers le dashboard avec la session courante.
 */
public final class ChangePasswordController {

    // ---------------------------------------------------------------------
    // FXML
    // ---------------------------------------------------------------------

    @FXML private Label          lblUserInfo;
    @FXML private PasswordField  txtNewPassword;
    @FXML private PasswordField  txtConfirmPassword;
    @FXML private Button         btnChange;
    @FXML private Button         btnCancel;
    @FXML private ProgressIndicator progress;
    @FXML private Label          errorLabel;
    @FXML private Label          successLabel;

    // ---------------------------------------------------------------------
    // Services / Contexte
    // ---------------------------------------------------------------------

    private AccountService accountService;
    private UserSession    currentSession;
    private Navigator      navigator;

    // ---------------------------------------------------------------------
    // Injection par le FxNavigator
    // ---------------------------------------------------------------------

    public void setAccountService(AccountService accountService) {
        this.accountService = Objects.requireNonNull(accountService, "accountService ne peut pas être null");
    }

    public void setNavigator(Navigator navigator) {
        this.navigator = navigator; // peut rester null => fallback fermeture fenêtre
    }

    /**
     * Appelé par le Navigator après un login avec mot de passe à changer.
     */
    public void setSession(UserSession session) {
        this.currentSession = Objects.requireNonNull(session, "session ne peut pas être null");
        if (session.email() != null && !session.email().isBlank()) {
            lblUserInfo.setText(
                    "Bonjour " + session.email()
                    + " — vous devez choisir un nouveau mot de passe avant de continuer."
            );
        } else {
            lblUserInfo.setText("Vous devez choisir un nouveau mot de passe avant de continuer.");
        }
    }

    // ---------------------------------------------------------------------
    // Initialisation FXML
    // ---------------------------------------------------------------------

    @FXML
    private void initialize() {
        hideMessages();
        setBusy(false);
    }

    // ---------------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------------

    @FXML
    private void onChangePassword() {
        hideMessages();

        if (accountService == null || currentSession == null) {
            showError("Contexte utilisateur invalide (erreur technique).");
            return;
        }

        String pwd  = safeText(txtNewPassword);
        String pwd2 = safeText(txtConfirmPassword);

        // Validation simple côté UI
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

        // Exécution asynchrone côté service
        CompletableFuture
                .runAsync(() -> accountService.changePasswordAndClearFlag(currentSession.userId(), pwd))
                .whenComplete((res, ex) -> Platform.runLater(() -> {
                    try {
                        if (ex != null) {
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            showError(cause.getMessage() != null ? cause.getMessage() : cause.toString());
                        } else {
                            showSuccess("Mot de passe mis à jour, redirection en cours...");
                            // Redirection vers le bon écran
                            if (navigator != null) {
                                navigator.goToDashboard(currentSession);
                            } else if (txtNewPassword.getScene() != null) {
                                // Fallback : fermer la fenêtre si pas de Navigator
                                txtNewPassword.getScene().getWindow().hide();
                            }
                        }
                    } finally {
                        setBusy(false);
                    }
                }));
    }

    @FXML
    private void onCancel() {
        if (navigator != null) {
            navigator.goToLogin();
        } else if (txtNewPassword != null && txtNewPassword.getScene() != null) {
            txtNewPassword.getScene().getWindow().hide();
        }
    }

    // ---------------------------------------------------------------------
    // Helpers UI (état & messages)
    // ---------------------------------------------------------------------

    private void setBusy(boolean busy) {
        if (btnChange != null)  btnChange.setDisable(busy);
        if (btnCancel != null)  btnCancel.setDisable(busy);
        if (progress != null) {
            progress.setVisible(busy);
            progress.setManaged(busy);
        }
    }

    private void hideMessages() {
        setLabelState(errorLabel,   "");
        setLabelState(successLabel, "");
    }

    private void showError(String msg) {
        setLabelState(errorLabel,   msg);
        setLabelState(successLabel, "");
    }

    private void showSuccess(String msg) {
        setLabelState(successLabel, msg);
        setLabelState(errorLabel,   "");
    }

    private void setLabelState(Label label, String text) {
        if (label == null) return;
        boolean visible = text != null && !text.isBlank();
        label.setText(text != null ? text : "");
        label.setVisible(visible);
        label.setManaged(visible);
    }

    private String safeText(PasswordField field) {
        return (field != null && field.getText() != null) ? field.getText() : "";
    }
}