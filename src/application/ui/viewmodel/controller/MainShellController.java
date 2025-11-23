package application.ui.viewmodel.controller;

import application.ui.navigation.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

/**
 * Contrôleur du "shell" principal de l'application (MainShell.fxml).
 *
 * <p>Le shell est la fenêtre principale de l'application après connexion :
 * il contient :
 * <ul>
 *     <li>une barre de navigation (Dashboard, Catalogue, etc.)</li>
 *     <li>une zone centrale où les différentes vues (Dashboard, Emprunts, etc.)
 *         sont affichées</li>
 *     <li>une zone d'information sur l'utilisateur connecté (email + rôle)</li>
 * </ul>
 *
 * <p>Ce contrôleur :
 * <ul>
 *     <li>affiche le nom et le rôle de l'utilisateur</li>
 *     <li>active ou masque les boutons du menu en fonction du rôle
 *         (ADMIN / ADHERENT / invité)</li>
 *     <li>délègue la navigation au {@link Navigator}</li>
 *     <li>ne crée jamais lui-même les vues : il se contente de recevoir
 *         un {@link Parent} et de le placer dans la zone centrale</li>
 * </ul>
 *
 * <p><b>Important :</b> ce contrôleur ne doit pas lui-même créer de nouvelles
 * fenêtres ou charger de FXML. Tout cela est géré par {@link Navigator}
 * (par exemple {@code FxNavigator}).</p>
 */
public final class MainShellController {

    // ---------------------------------------------------------------------
    // Zone racine et zone de contenu
    // ---------------------------------------------------------------------

    /**
     * BorderPane racine du shell.
     */
    @FXML
    private BorderPane rootPane;

    /**
     * Pane de contenu situé au centre du shell (à l'intérieur du StackPane).
     * Toutes les vues (Dashboard, Catalogue, etc.) sont injectées ici
     * par le {@link Navigator} via {@link #setContent(Parent)}.
     */
    @FXML
    private AnchorPane contentPane;

    // ---------------------------------------------------------------------
    // En-tête : informations utilisateur
    // ---------------------------------------------------------------------

    /** Label affichant l'email ou le nom de l'utilisateur connecté. */
    @FXML
    private Label lblUsername;

    /** Label affichant le rôle de l'utilisateur (ADMIN, ADHERENT, etc.). */
    @FXML
    private Label lblRole;

    // ---------------------------------------------------------------------
    // Groupe de navigation et boutons du menu (définis dans MainShell.fxml)
    // ---------------------------------------------------------------------

    /** Groupe des boutons de navigation (toggle exclusif). */
    @FXML
    private ToggleGroup navGroup;

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

    // ---------------------------------------------------------------------
    // Navigation (injection depuis FxNavigator)
    // ---------------------------------------------------------------------

    /**
     * Référence vers le Navigator, injectée depuis {@code FxNavigator}.
     *
     * <p>Le Navigator sait :
     * <ul>
     *     <li>créer les ViewModels et contrôleurs</li>
     *     <li>charger les fichiers FXML</li>
     *     <li>et appeler {@link #setContent(Parent)} pour afficher la vue
     *         demandée dans ce shell</li>
     * </ul>
     */
    private Navigator navigator;

    /**
     * Appelée par le Navigator juste après la création du shell.
     *
     * @param navigator instance de {@link Navigator} à utiliser pour
     *                  changer de page (Dashboard, Catalogue, etc.)
     */
    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    // ---------------------------------------------------------------------
    // Contexte utilisateur (email + rôle)
    // ---------------------------------------------------------------------

    /**
     * Méthode appelée par le Navigator pour mettre à jour les informations
     * d'utilisateur affichées dans le header.
     *
     * @param username email ou nom de l'utilisateur (peut être null)
     * @param roleName nom du rôle ("ADMIN", "ADHERENT", etc. — peut être null)
     */
    public void setUserContext(String username, String roleName) {
        if (lblUsername != null) {
            lblUsername.setText(username != null ? username : "Invité");
        }

        if (lblRole != null) {
            lblRole.setText(roleName != null ? roleName : "Rôle inconnu");
        }

        // En fonction du rôle, on configure quels menus sont visibles
        configureMenuForRole(roleName);
    }

    /**
     * Configure la visibilité des boutons du menu en fonction du rôle.
     *
     * <p>Règles simples :</p>
     * <ul>
     *     <li><b>ADMIN</b> : accès complet (Dashboard, Catalogue,
     *         Adhérents, Emprunts, Pénalités, Comptes)</li>
     *     <li><b>ADHERENT</b> : accès au Catalogue + ses propres Emprunts
     *         et Pénalités (Mes Emprunts / Mes Pénalités)</li>
     *     <li><b>Autre / invité</b> : accès minimal (Catalogue)</li>
     * </ul>
     *
     * @param roleName nom du rôle ("ADMIN", "ADHERENT", etc.)
     */
    private void configureMenuForRole(String roleName) {
        boolean isAdmin    = roleName != null && "ADMIN".equalsIgnoreCase(roleName);
        boolean isAdherent = roleName != null && "ADHERENT".equalsIgnoreCase(roleName);

        if (isAdmin) {
            // Admin : accès complet
            setVisibleAndManaged(btnDashboard,    true);
            setVisibleAndManaged(btnCatalog,      true);
            setVisibleAndManaged(btnAdherents,    true);
            setVisibleAndManaged(btnLoans,        true);
            setVisibleAndManaged(btnPenalites,    true);
            setVisibleAndManaged(btnAccounts,     true);

            setVisibleAndManaged(btnMesEmprunts,  false);
            setVisibleAndManaged(btnMesPenalites, false);

        } else if (isAdherent) {
            // Adhérent : vue catalogue + pages personnelles
            setVisibleAndManaged(btnDashboard,    false);
            setVisibleAndManaged(btnCatalog,      true);
            setVisibleAndManaged(btnAdherents,    false);
            setVisibleAndManaged(btnLoans,        false);
            setVisibleAndManaged(btnPenalites,    false);
            setVisibleAndManaged(btnAccounts,     false);

            setVisibleAndManaged(btnMesEmprunts,  true);
            setVisibleAndManaged(btnMesPenalites, true);

        } else {
            // Cas par défaut / invité : accès très limité
            setVisibleAndManaged(btnDashboard,    false);
            setVisibleAndManaged(btnCatalog,      true);
            setVisibleAndManaged(btnAdherents,    false);
            setVisibleAndManaged(btnLoans,        false);
            setVisibleAndManaged(btnPenalites,    false);
            setVisibleAndManaged(btnAccounts,     false);

            setVisibleAndManaged(btnMesEmprunts,  false);
            setVisibleAndManaged(btnMesPenalites, false);
        }
    }

    /**
     * Helper pour définir {@code visible} ET {@code managed} sur un contrôle.
     *
     * <p>En JavaFX, un nœud :
     * <ul>
     *     <li>non visible : n'est pas affiché</li>
     *     <li>non managed : n'est pas pris en compte dans les calculs de layout</li>
     * </ul>
     * En mettant les deux à {@code false}, le contrôle ne prend aucune place à l'écran.</p>
     *
     * @param control contrôle JavaFX (peut être null)
     * @param value   true pour afficher / false pour masquer complètement
     */
    private void setVisibleAndManaged(Control control, boolean value) {
        if (control != null) {
            control.setVisible(value);
            control.setManaged(value);
        }
    }

    // ---------------------------------------------------------------------
    // Injection de la vue centrale par le Navigator
    // ---------------------------------------------------------------------

    /**
     * Appelée par le Navigator pour afficher une nouvelle vue dans la zone
     * centrale du shell (à l'intérieur de {@link #contentPane}).
     *
     * @param view racine de la vue (par exemple le root d'un FXML)
     */
    public void setContent(Parent view) {
        if (contentPane != null && view != null) {
            contentPane.getChildren().setAll(view);

            // Étire la vue sur toute la surface de l'AnchorPane
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
        }
    }

    // ---------------------------------------------------------------------
    // Gestion des clics sur le menu (handlers FXML)
    // ---------------------------------------------------------------------

    /** Handler du bouton "Tableau de bord". */
    @FXML
    private void goDashboard() {
        if (navigator != null) {
            navigator.goToDashboard(null); // session déjà connue côté Navigator
        }
    }

    /** Handler du bouton "Catalogue". */
    @FXML
    private void goCatalog() {
        if (navigator != null) {
            navigator.goToCatalog();
        }
    }

    /** Handler du bouton "Adhérents". */
    @FXML
    private void goAdherents() {
        if (navigator != null) {
            navigator.goToAdherents();
        }
    }

    /** Handler du bouton "Emprunts". */
    @FXML
    private void goLoans() {
        if (navigator != null) {
            navigator.goToLoans();
        }
    }

    /** Handler du bouton "Pénalités". */
    @FXML
    private void goPenalites() {
        if (navigator != null) {
            navigator.goToPenalites();
        }
    }

    /** Handler du bouton "Mes emprunts" (vue adhérent). */
    @FXML
    private void goMesEmprunts() {
        if (navigator != null) {
            navigator.goToMesEmprunts();
        }
    }

    /** Handler du bouton "Mes pénalités" (vue adhérent). */
    @FXML
    private void goMesPenalites() {
        if (navigator != null) {
            navigator.goToMesPenalites();
        }
    }

    /** Handler du bouton "Comptes" (gestion des comptes utilisateurs par un admin). */
    @FXML
    private void goAccounts() {
        if (navigator != null) {
            navigator.goToAccounts();
        }
    }

    /**
     * Handler du bouton "Déconnexion".
     *
     * <p>Demande au Navigator de :
     * <ul>
     *     <li>oublier la session courante</li>
     *     <li>revenir à l'écran d'accueil (Landing)</li>
     * </ul>
     *
     * <p>Si, pour une raison quelconque, aucun Navigator n'est injecté,
     * on ferme complètement l'application (fallback de sécurité).</p>
     */
    @FXML
    private void logout() {
        if (navigator != null) {
            navigator.logout();
        } else {
            Platform.exit();
        }
    }
}
