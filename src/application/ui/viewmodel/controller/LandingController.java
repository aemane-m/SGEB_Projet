package application.ui.viewmodel.controller;

import application.domain.service.AccountService;
import application.ui.navigation.Navigator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

public final class LandingController {

    @FXML
    private BorderPane root;

    @FXML
    private Button btnLogin;

    @FXML
    private Button btnRegister;

    @FXML
    private Button btnGuestCatalogue;

    private AccountService accountService;
    private Navigator navigator;

    // ================== Injection ==================

    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    // ================== Initialisation ==================

    @FXML
    private void initialize() {
        // Rien à initialiser côté contenu : tout est dans le FXML.
        // Tu peux éventuellement désactiver certains boutons si besoin.
    }

    // ================== Actions ==================

    @FXML
    private void onLoginClicked() {
        switchScene("/application/ui/view/LoginView.fxml", controller -> {
            if (controller instanceof LoginController loginCtrl) {
                if (accountService != null) loginCtrl.setAccountService(accountService);
                if (navigator != null)      loginCtrl.setNavigator(navigator);
            }
        });
    }

    @FXML
    private void onRegisterClicked() {
        // ⚠️ Vérifie que ce chemin correspond bien à ton FXML d'inscription adhérent
        switchScene("/application/ui/view/SignupAdherentView.fxml", controller -> {
            if (controller instanceof SignupAdherentController signupCtrl) {
                if (accountService != null) signupCtrl.setAccountService(accountService);
                if (navigator != null)      signupCtrl.setNavigator(navigator);
            }
        });
    }

    @FXML
    private void onGuestCatalogueClicked() {
        // Si tu as déjà un Navigator.goToCatalogGuest(), appelle-le ici.
        // En attendant, on met une alerte propre.
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Catalogue invité");
        alert.setHeaderText("Mode invité");
        alert.setContentText("""
                La consultation du catalogue en mode invité
                permet de parcourir les documents sans être connecté.

                Fonctionnalité à brancher sur la vue Catalogue :
                • Accès en lecture seule
                • Pas d’emprunt possible
                """);
        alert.showAndWait();
    }

    // ================== Helper ==================

    private void switchScene(String fxmlPath, Consumer<Object> controllerConfigurator) {
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IllegalStateException("FXML introuvable sur le classpath : " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent newRoot = loader.load();

            Object controller = loader.getController();
            if (controller != null && controllerConfigurator != null) {
                controllerConfigurator.accept(controller);
            }

            Stage stage = (Stage) root.getScene().getWindow();
            Scene scene = new Scene(
                    newRoot,
                    stage.getScene().getWidth(),
                    stage.getScene().getHeight()
            );
            stage.setScene(scene);
            stage.centerOnScreen();

        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText("Impossible d'ouvrir l'écran demandé.");
            error.setContentText(e.getMessage());
            error.showAndWait();
        }
    }
}