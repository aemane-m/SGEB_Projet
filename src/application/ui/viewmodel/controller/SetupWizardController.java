package application.ui.viewmodel.controller;

import application.domain.service.AccountService;
import application.infrastructure.sqlite.DemoDataSeeder;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Contrôleur de l'écran de configuration initiale (SetupWizard.fxml).
 *
 * <p>Rôle de cet écran :</p>
 * <ul>
 *     <li>Créer le tout premier compte administrateur de l'application.</li>
 *     <li>Optionnellement, installer des données de démonstration (documents, adhérents, etc.).</li>
 * </ul>
 *
 * <p>Contexte :</p>
 * <ul>
 *     <li>Ce wizard n'est affiché qu'au tout premier lancement, lorsqu'aucun compte n'existe.</li>
 *     <li>Une fois l'admin créé, l'application peut basculer vers l'écran de login classique.</li>
 * </ul>
 *
 * <p>Ce contrôleur ne connaît pas la navigation globale (Navigator) :
 * il se limite à créer l'admin, éventuellement installer la démo, puis
 * fermer la fenêtre. La navigation suivante est gérée ailleurs (dans Main / FxNavigator).</p>
 */
public class SetupWizardController {

    // ---------------------------------------------------------------------
    // Champs FXML (formulaire)
    // ---------------------------------------------------------------------

    /** Champ "Nom" de l'administrateur. */
    @FXML
    private TextField tfNom;

    /** Champ "Prénom" de l'administrateur. */
    @FXML
    private TextField tfPrenom;

    /** Champ "Email" utilisé comme identifiant de connexion. */
    @FXML
    private TextField tfEmail;

    /** Champ "Mot de passe". */
    @FXML
    private PasswordField pfPassword;

    /** Champ "Confirmation du mot de passe". */
    @FXML
    private PasswordField pfConfirm;

    /** Zone d'affichage des messages d'erreur. */
    @FXML
    private Label lblError;

    /** Case à cocher "Installer des données de démonstration". */
    @FXML
    private CheckBox cbInstallDemo;

    // ---------------------------------------------------------------------
    // Services injectés
    // ---------------------------------------------------------------------

    /**
     * Service de gestion des comptes.
     * Utilisé pour créer le tout premier administrateur.
     */
    private AccountService accountService;

    /**
     * Service responsable de l'installation des données de démonstration.
     * Peut rester null si les données de démo ne sont pas supportées.
     */
    private DemoDataSeeder demoDataSeeder;

    // ---------------------------------------------------------------------
    // Injection des dépendances (depuis Main / FxNavigator)
    // ---------------------------------------------------------------------

    /**
     * Injecté depuis le code de démarrage (Main/FxNavigator).
     *
     * @param accountService service permettant de créer le premier admin
     */
    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Injecté depuis Main pour pouvoir installer des données de démonstration.
     *
     * @param demoDataSeeder composant chargé de créer les données de démo
     */
    public void setDemoDataSeeder(DemoDataSeeder demoDataSeeder) {
        this.demoDataSeeder = demoDataSeeder;
    }

    // ---------------------------------------------------------------------
    // Action principale : création du premier administrateur
    // ---------------------------------------------------------------------

    /**
     * Appelé lorsque l'utilisateur clique sur le bouton "Créer l'administrateur".
     *
     * <p>Étapes :</p>
     * <ol>
     *     <li>Vider le message d'erreur actuel.</li>
     *     <li>Vérifier que {@link #accountService} est bien injecté.</li>
     *     <li>Lire et nettoyer les valeurs des champs.</li>
     *     <li>Faire des validations simples côté UI (champs vides, mots de passe identiques).</li>
     *     <li>Appeler {@link AccountService#createFirstAdmin(String, String, String, String)}.</li>
     *     <li>Si l'option est cochée, appeler {@link DemoDataSeeder#installDemoData()}.</li>
     *     <li>Afficher une boîte de dialogue de succès puis proposer de quitter l’application.</li>
     *     <li>Fermer la fenêtre du wizard si l’utilisateur choisit de continuer.</li>
     * </ol>
     */
    @FXML
    private void onCreateAdmin() {
        clearError();

        if (accountService == null) {
            showError("Service de comptes indisponible.");
            return;
        }

        String nom     = safe(tfNom.getText());
        String prenom  = safe(tfPrenom.getText());
        String email   = safe(tfEmail.getText());
        String pwd     = safe(pfPassword.getText());
        String confirm = safe(pfConfirm.getText());

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || pwd.isEmpty() || confirm.isEmpty()) {
            showError("Tous les champs sont obligatoires.");
            return;
        }

        if (!pwd.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        try {
            accountService.createFirstAdmin(email, pwd, nom, prenom);

            if (cbInstallDemo != null && cbInstallDemo.isSelected()) {
                if (demoDataSeeder == null) {
                    showError("Impossible d'installer les données de démonstration.");
                    return;
                }
                demoDataSeeder.installDemoData();
            }

            String baseContent = (cbInstallDemo != null && cbInstallDemo.isSelected())
                    ? "Le compte administrateur a été créé et les données de démonstration ont été installées."
                    : "Le compte administrateur a été créé avec succès.";

            Alert restartDialog = new Alert(Alert.AlertType.CONFIRMATION);
            restartDialog.setTitle("Configuration initiale terminée");
            restartDialog.setHeaderText("Configuration terminée avec succès.");
            restartDialog.setContentText(
                    baseContent
                            + "\n\nPour finaliser la configuration, SGEB doit être redémarré.\n\n"
                            + "Voulez-vous quitter maintenant ?"
            );

            ButtonType btnQuit   = new ButtonType("Quitter SGEB", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnLater  = new ButtonType("Continuer", ButtonBar.ButtonData.CANCEL_CLOSE);
            restartDialog.getButtonTypes().setAll(btnQuit, btnLater);

            ButtonType result = restartDialog.showAndWait().orElse(btnLater);

            if (result == btnQuit) {
                Platform.exit();
            } else {
                tfNom.getScene().getWindow().hide();
            }

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Helpers d'affichage des messages
    // ---------------------------------------------------------------------

    /**
     * Efface le message d'erreur courant.
     */
    private void clearError() {
        if (lblError != null) {
            lblError.setText("");
        }
    }

    /**
     * Affiche un message d'erreur dans le label prévu à cet effet.
     *
     * @param message texte à afficher (non null de préférence)
     */
    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message != null ? message : "");
        }
    }

    /**
     * Nettoie une chaîne en supprimant les espaces en début / fin.
     * Retourne une chaîne vide si l'entrée est null.
     *
     * @param s chaîne potentiellement null
     * @return chaîne trimée ou vide
     */
    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}