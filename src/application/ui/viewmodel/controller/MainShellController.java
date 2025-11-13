package application.ui.viewmodel.controller;

import application.ui.navigation.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane; // Import manquant

/**
 * Contrôleur pour le "Shell" principal de l'application (la navigation).
 * Gère l'injection du contenu central et les actions de navigation.
 *
 * CORRIGÉ : Ajout de tous les @FXML requis par MainShell.fxml.
 */
public class MainShellController {

    // --- CORRIGÉ : Ajout des @FXML manquants ---
    @FXML private BorderPane rootPane;
    @FXML private AnchorPane contentPane;
    @FXML private ToggleGroup navGroup;
    @FXML private Label lblUsername;
    @FXML private Label lblRole;
    // --- Fin des corrections ---
    
    private Navigator navigator;

    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    /**
     * Injecte une vue (chargée par le Navigator) dans la zone de contenu.
     */
    public void setContent(Node view) {
        if (contentPane != null) {
            contentPane.getChildren().setAll(view);
            // Assure que le contenu remplit l'AnchorPane
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
        }
    }

    /**
     * Met à jour les infos utilisateur dans le menu.
     * (Utilise les fx:id corrigés 'lblUsername' et 'lblRole')
     */
    public void setUserContext(String username, String role) {
        if (lblUsername != null) {
            lblUsername.setText(username != null ? username : "Utilisateur");
        }
        if (lblRole != null) {
            lblRole.setText(role != null ? role : "N/A");
        }
    }

    // --- Actions de Navigation ---

    @FXML
    private void goDashboard() {
        if (navigator != null) navigator.goToDashboard();
    }

    @FXML
    private void goCatalog() {
        if (navigator != null) navigator.goToCatalog();
    }

    @FXML
    private void goAdherents() {
        if (navigator != null) navigator.goToAdherents();
    }

    @FXML
    private void goLoans() {
        if (navigator != null) navigator.goToLoans();
    }
    
    /**
     * NOUVEAU : Gère le clic sur le bouton Pénalités.
     */
    @FXML
    private void goPenalites() {
        // Doit caster car l'interface Navigator de base ne connaît pas cette méthode
        if (navigator != null && navigator instanceof application.ui.navigation.fx.FxNavigator) {
             ((application.ui.navigation.fx.FxNavigator) navigator).goToPenalites();
        }
    }

    @FXML
    private void exitApp() {
        // Utiliser Platform.exit() est plus propre que System.exit(0)
        Platform.exit();
    }
}

