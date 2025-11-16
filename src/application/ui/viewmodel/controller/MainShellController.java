package application.ui.viewmodel.controller;

import application.ui.navigation.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;

/**
 * Contrôleur du Shell principal (MainShell.fxml).
 * - Affiche le nom + rôle de l'utilisateur
 * - Active / désactive les entrées du menu selon le rôle
 * - Ne déclenche PAS de navigation automatique sur le Dashboard,
 *   pour éviter toute récursion avec FxNavigator.
 */
public class MainShellController {

    // --- Zone centrale dans laquelle le Navigator injecte les vues ---
    @FXML
    private BorderPane rootPane;

    // --- Barre haute / header (IDs selon le FXML !) ---
    @FXML
    private Label lblUsername;

    @FXML
    private Label lblRole;

    // --- Boutons / menus (ToggleButton comme dans le FXML) ---
    @FXML
    private ToggleButton btnDashboard;

    @FXML
    private ToggleButton btnCatalog;

    @FXML
    private ToggleButton btnAdherents;

    @FXML
    private ToggleButton btnLoans;

    @FXML
    private ToggleButton btnPenalites;

    @FXML
    private ToggleButton btnMesEmprunts;

    @FXML
    private ToggleButton btnMesPenalites;
    
    @FXML
    private ToggleButton btnAccounts;

    // --- Référence au Navigator ---
    private Navigator navigator;

    // =========================================================
    // Injection du Navigator
    // =========================================================

    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    // =========================================================
    // Contexte utilisateur
    // =========================================================

    /**
     * Méthode appelée par FxNavigator.applyUserContextToShell().
     * @param username  email ou nom à afficher
     * @param roleName  "ADMIN", "ADHERENT", etc.
     */
    public void setUserContext(String username, String roleName) {
        if (lblUsername != null) {
            lblUsername.setText(username != null ? username : "Invité");
        }
        if (lblRole != null) {
            lblRole.setText(roleName != null ? roleName : "Rôle inconnu");
        }

        configureForRole(roleName);
    }

    /**
     * Active / désactive les boutons/menu selon le rôle.
     * ⚠️ NE DOIT PAS APPELER navigator.goToDashboard() !
     */
    private void configureForRole(String roleName) {
        boolean isAdmin    = roleName != null && "ADMIN".equalsIgnoreCase(roleName);
        boolean isAdherent = roleName != null && "ADHERENT".equalsIgnoreCase(roleName);

        if (isAdmin) {
            // Admin : accès complet
            setVisibleAndManaged(btnDashboard, true);
            setVisibleAndManaged(btnCatalog, true);
            setVisibleAndManaged(btnAdherents, true);
            setVisibleAndManaged(btnLoans, true);
            setVisibleAndManaged(btnPenalites, true);
            
            setVisibleAndManaged(btnMesEmprunts, false);
            setVisibleAndManaged(btnMesPenalites, false);
            setVisibleAndManaged(btnAccounts, true);

        } else if (isAdherent) {
            // Adhérent : accès au catalogue + vues perso
            setVisibleAndManaged(btnDashboard, false);
            setVisibleAndManaged(btnCatalog, true);
            setVisibleAndManaged(btnAdherents, false);
            setVisibleAndManaged(btnLoans, false);
            setVisibleAndManaged(btnPenalites, false);

            setVisibleAndManaged(btnMesEmprunts, true);
            setVisibleAndManaged(btnMesPenalites, true);
            setVisibleAndManaged(btnAccounts, false);

        } else {
            // Invité / rôle inconnu
            setVisibleAndManaged(btnDashboard, false);
            setVisibleAndManaged(btnCatalog, true);
            setVisibleAndManaged(btnAdherents, false);
            setVisibleAndManaged(btnLoans, false);
            setVisibleAndManaged(btnPenalites, false);

            setVisibleAndManaged(btnMesEmprunts, false);
            setVisibleAndManaged(btnMesPenalites, false);
        }
    }

    private void setVisibleAndManaged(Control c, boolean value) {
        if (c != null) {
            c.setVisible(value);
            c.setManaged(value);
        }
    }

    // =========================================================
    // Méthode appelée par le Navigator pour injecter une vue
    // =========================================================

    /**
     * Appelée par FxNavigator.setCenter(...).
     */
    public void setContent(Parent view) {
        if (rootPane != null) {
            rootPane.setCenter(view);
        }
    }

    // =========================================================
    // Handlers des boutons du menu (NOMS = onAction du FXML)
    // =========================================================

    @FXML
    private void goDashboard() {
        if (navigator != null) {
            navigator.goToDashboard(null);
        }
    }

    @FXML
    private void goCatalog() {
        if (navigator != null) {
            navigator.goToCatalog();
        }
    }

    @FXML
    private void goAdherents() {
        if (navigator != null) {
            navigator.goToAdherents();
        }
    }

    @FXML
    private void goLoans() {
        if (navigator != null) {
            navigator.goToLoans();
        }
    }

    @FXML
    private void goPenalites() {
        if (navigator != null) {
            navigator.goToPenalites();
        }
    }

    @FXML
    private void goMesEmprunts() {
        if (navigator != null) {
            navigator.goToMesEmprunts();
        }
    }

    @FXML
    private void goMesPenalites() {
        if (navigator != null) {
            navigator.goToMesPenalites();
        }
    }
    
    @FXML
    private void goAccounts() {
        if (navigator != null) {
            navigator.goToAccounts();
        }
    }

    @FXML
    private void logout() {
        if (navigator != null) {
            navigator.logout();
        } else {
            // fallback "sécurité" si jamais
            Platform.exit();
        }
    }
}