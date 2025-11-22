package application.ui.viewmodel.controller;

import application.domain.service.AccountService;
import application.infrastructure.sqlite.DemoDataSeeder;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SetupWizardController {

    @FXML private TextField tfNom;
    @FXML private TextField tfPrenom;
    @FXML private TextField tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirm;
    @FXML private Label lblError;
    @FXML private CheckBox cbInstallDemo;

    private AccountService accountService;
    private DemoDataSeeder demoDataSeeder;

    /**
     * Injecté depuis Main (FxNavigator / FXMLLoader).
     */
    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Injecté depuis Main pour pouvoir installer les données de démonstration.
     */
    public void setDemoDataSeeder(DemoDataSeeder demoDataSeeder) {
        this.demoDataSeeder = demoDataSeeder;
    }

    @FXML
    private void onCreateAdmin() {
        lblError.setText("");

        if (accountService == null) {
            lblError.setText("Service de comptes indisponible.");
            System.err.println("[SetupWizard] accountService est null, impossible de créer l'admin.");
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
            System.out.println("[SetupWizard] Création du premier administrateur...");
            // Signature de AccountService : (email, rawPassword, nom, prenom)
            accountService.createFirstAdmin(email, pwd, nom, prenom);
            System.out.println("[SetupWizard] Administrateur créé avec succès.");

            // Installer éventuellement les données de démonstration
            if (cbInstallDemo != null && cbInstallDemo.isSelected()) {
                System.out.println("[SetupWizard] Option démo cochée.");
                if (demoDataSeeder == null) {
                    // Cas d'erreur : l'injection n'a pas fonctionné
                    String msg = "Impossible d'installer les données de démonstration : DemoDataSeeder non injecté.";
                    System.err.println("[SetupWizard] " + msg);
                    lblError.setText(msg);
                } else {
                    System.out.println("[SetupWizard] Lancement de DemoDataSeeder.installDemoData()...");
                    demoDataSeeder.installDemoData();
                    System.out.println("[SetupWizard] Installation des données de démo terminée.");
                }
            } else {
                System.out.println("[SetupWizard] Option démo non cochée, aucune donnée de démo installée.");
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Configuration initiale terminée");
            alert.setHeaderText("Compte administrateur créé avec succès.");
            alert.setContentText(
                    cbInstallDemo != null && cbInstallDemo.isSelected()
                            ? "Les données de démonstration ont été installées.\nVous pouvez maintenant vous connecter à SGEB."
                            : "Vous pouvez maintenant vous connecter à SGEB avec ce compte administrateur."
            );
            alert.showAndWait();

            // On ferme la fenêtre (la navigation vers Login/Landing est gérée par Main)
            tfNom.getScene().getWindow().hide();

        } catch (Exception e) {
            e.printStackTrace();
            lblError.setText(e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
