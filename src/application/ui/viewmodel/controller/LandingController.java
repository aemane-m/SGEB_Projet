package application.ui.viewmodel.controller;

import application.domain.service.AccountService;
import application.ui.navigation.Navigator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

public final class LandingController {

    @FXML
    private BorderPane root;

    @FXML
    private TextArea lblDetails;

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
        if (lblDetails != null) {
            String text = """
                    Bienvenue dans SGEB — Système de Gestion des Emprunts de Bibliothèque

                    Objectif de l’application
                    --------------------------------
                    • Informatiser la gestion d’une bibliothèque universitaire.
                    • Centraliser les informations sur les documents, les adhérents et les emprunts.
                    • Suivre les retards et les pénalités de manière fiable et traçable.

                    Profils utilisateurs
                    --------------------------------
                    • Administrateur :
                      - gère le catalogue (livres, magazines, CD…)
                      - enregistre et met à jour les adhérents
                      - crée et suit les emprunts en cours
                      - gère les retours, retards et pénalités
                      - consulte les indicateurs globaux (KPI) sur le tableau de bord

                    • Adhérent :
                      - consulte le catalogue et recherche un document
                      - visualise ses emprunts en cours et son historique
                      - consulte ses pénalités et peut demander la mise en paiement

                    Fonctionnalités principales
                    --------------------------------
                    • Tableau de bord (ADMIN) :
                      - nombre total de documents, d’adhérents et d’emprunts actifs
                      - nombre de retards et montant des pénalités en cours

                    • Catalogue :
                      - recherche par titre, auteur, genre, type de document
                      - filtrage et consultation détaillée d’un document
                      - ajout / modification / suppression de documents (administrateur)
                      - gestion des jaquettes (stockage local des couvertures)

                    • Gestion des adhérents (ADMIN) :
                      - création de nouveaux adhérents
                      - mise à jour des informations (nom, prénom, email…)
                      - consultation des emprunts et pénalités liés à un adhérent

                    • Gestion des emprunts (ADMIN) :
                      - enregistrement d’un nouvel emprunt pour un adhérent
                      - validation du retour et mise à jour du statut
                      - détection des retards à partir de la date prévue de retour

                    • Gestion des pénalités :
                      - génération de pénalités en cas de retard
                      - suivi de l’état (due, en attente de paiement, réglée…)
                      - côté adhérent : demande de mise en paiement d’une pénalité

                    • Espace adhérent :
                      - « Mes emprunts » : vue centrée sur les documents empruntés
                      - « Mes pénalités » : liste des pénalités avec montants et statuts

                    Pour commencer
                    --------------------------------
                    • Pour un premier lancement :
                      - créez un compte administrateur via l’assistant de configuration.
                    • Pour un adhérent :
                      - créez votre compte avec « Créer un compte adhérent », puis utilisez « Se connecter ».
                    • Vous pouvez également parcourir le catalogue en mode invité
                      via le bouton « Consulter le catalogue » (fonctionnalité à venir).
                    """;

            lblDetails.setText(text);
            lblDetails.setScrollTop(0);
        }
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
        // ⚠️ ADAPTE le chemin au VRAI nom de ton FXML d’inscription :
        switchScene("/application/ui/view/SignupAdherentView.fxml", controller -> {
            if (controller instanceof SignupAdherentController signupCtrl) {
                if (accountService != null) signupCtrl.setAccountService(accountService);
                if (navigator != null)      signupCtrl.setNavigator(navigator);
            }
        });
    }

    @FXML
    private void onGuestCatalogueClicked() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Catalogue invité");
        alert.setHeaderText("Fonctionnalité à venir");
        alert.setContentText("""
                La consultation du catalogue en mode invité
                sera disponible dans une prochaine version.

                Pour l'instant, vous pouvez :
                • Créer un compte adhérent
                • Ou vous connecter avec un compte existant.
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
            Scene scene = new Scene(newRoot,
                    stage.getScene().getWidth(),
                    stage.getScene().getHeight());
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