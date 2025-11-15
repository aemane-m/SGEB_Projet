package application.ui.viewmodel.controller;

import application.domain.service.AccountService;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SetupWizardController {

    @FXML private TextField tfNom;
    @FXML private TextField tfPrenom;
    @FXML private TextField tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirm;
    @FXML private Label lblError;

    private AccountService accountService;

    /** Injecté depuis Main via réflexion (injectAccountService). */
    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    @FXML
    private void onCreateAdmin() {
        lblError.setText("");

        if (accountService == null) {
            lblError.setText("Service de comptes indisponible.");
            return;
        }

        String nom     = safe(tfNom.getText());
        String prenom  = safe(tfPrenom.getText());
        String email   = safe(tfEmail.getText());
        String pwd     = pfPassword.getText() != null ? pfPassword.getText().trim() : "";
        String confirm = pfConfirm.getText() != null ? pfConfirm.getText().trim() : "";

        // Validations simples
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || pwd.isEmpty() || confirm.isEmpty()) {
            lblError.setText("Tous les champs sont obligatoires.");
            return;
        }
        if (!pwd.equals(confirm)) {
            lblError.setText("Les mots de passe ne correspondent pas.");
            return;
        }

        try {
            // Signature de AccountService : (email, rawPassword, nom, prenom)
            accountService.createFirstAdmin(email, pwd, nom, prenom);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Administrateur créé");
            alert.setHeaderText("Compte administrateur créé avec succès.");
            alert.setContentText("Vous pouvez maintenant relancer l'application et vous connecter.");
            alert.showAndWait();

            // On ferme la fenêtre (pour ce TP, on ne chaîne pas encore vers Landing)
            tfNom.getScene().getWindow().hide();

        } catch (Exception e) {
            lblError.setText(e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}