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
    	// Rien a initialiser.
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
        switchScene("/application/ui/view/SignupAdherentView.fxml", controller -> {
            if (controller instanceof SignupAdherentController signupCtrl) {
                if (accountService != null) signupCtrl.setAccountService(accountService);
                if (navigator != null)      signupCtrl.setNavigator(navigator);
            }
        });
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