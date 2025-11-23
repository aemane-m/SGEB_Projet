package application.ui.viewmodel.controller;

import application.domain.service.AccountService;
import application.ui.navigation.Navigator;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Contrôleur de l'écran d'inscription des adhérents (SignupAdherentView.fxml).
 *
 * <p>Rôle de cet écran :</p>
 * <ul>
 *     <li>Permettre à un futur adhérent de créer son compte (self-signup).</li>
 *     <li>Valider le formulaire côté UI (champs obligatoires, mots de passe identiques, etc.).</li>
 *     <li>Déléguer la création du compte au {@link AccountService}.</li>
 *     <li>Déléguer la navigation au {@link Navigator} (retour Landing, aller à Login, etc.).</li>
 * </ul>
 *
 * <p>Important : ce contrôleur ne charge plus lui-même les FXML d’autres écrans.
 * Toutes les transitions sont désormais gérées par le {@link Navigator}, ce qui
 * corrige notamment l’erreur
 * <code>The method setAccountService(AccountService) is undefined for the type LandingController</code>
 * (plus aucun appel direct à LandingController).</p>
 */
public class SignupAdherentController {

    // ---------------------------------------------------------------------
    // Racine FXML
    // ---------------------------------------------------------------------

    /** Conteneur racine de la vue (VBox dans le FXML). */
    @FXML
    private VBox rootPane;

    // ---------------------------------------------------------------------
    // Champs du formulaire
    // ---------------------------------------------------------------------

    /** Email de connexion (identifiant). */
    @FXML
    private TextField emailField;

    /** Mot de passe choisi par l'adhérent. */
    @FXML
    private PasswordField passwordField;

    /** Confirmation du mot de passe. */
    @FXML
    private PasswordField confirmPasswordField;

    /** Prénom de l'adhérent. */
    @FXML
    private TextField prenomField;

    /** Nom de l'adhérent. */
    @FXML
    private TextField nomField;

    /** Bouton "Créer mon compte". */
    @FXML
    private Button btnCreate;

    /** Bouton "Retour à l'accueil" (Landing). */
    @FXML
    private Button btnBack;

    /** Bouton "Aller à la page de connexion". */
    @FXML
    private Button btnGotoLogin;

    /** Label pour les messages d'information (succès, info). */
    @FXML
    private Label infoLabel;

    /** Label pour les messages d'erreur. */
    @FXML
    private Label errorLabel;

    /** Indicateur de chargement (spinner). */
    @FXML
    private ProgressIndicator progress;

    // ---------------------------------------------------------------------
    // Services injectés (domaine + navigation)
    // ---------------------------------------------------------------------

    /** Service métier pour la gestion des comptes. */
    private AccountService accountService;

    /** Service de navigation (gestion centralisée des écrans). */
    private Navigator navigator;

    /** Indique si une opération asynchrone est en cours (création de compte). */
    private final BooleanProperty loading = new SimpleBooleanProperty(false);

    // ---------------------------------------------------------------------
    // Injection des dépendances
    // ---------------------------------------------------------------------

    /**
     * Injecté depuis FxNavigator / Main pour pouvoir créer l’adhérent.
     */
    public void setAccountService(AccountService svc) {
        this.accountService = Objects.requireNonNull(svc, "AccountService ne peut pas être null");
    }

    /**
     * Injecté depuis FxNavigator / Main pour gérer la navigation.
     */
    public void setNavigator(Navigator navigator) {
        this.navigator = Objects.requireNonNull(navigator, "Navigator ne peut pas être null");
    }

    // ---------------------------------------------------------------------
    // Initialisation JavaFX
    // ---------------------------------------------------------------------

    /**
     * Méthode appelée automatiquement après le chargement du FXML.
     *
     * <p>On y configure :</p>
     * <ul>
     *     <li>La validation du formulaire (binding du bouton "Créer").</li>
     *     <li>L'état des boutons pendant le chargement.</li>
     *     <li>La visibilité du spinner de chargement.</li>
     *     <li>Le nettoyage initial des messages.</li>
     * </ul>
     */
    @FXML
    private void initialize() {
        // Formulaire invalide si :
        // - email vide
        // - mot de passe vide ou < 6 caractères
        // - confirmation vide ou différente
        // - prénom ou nom vides
        BooleanBinding invalidForm = Bindings.createBooleanBinding(
                () -> isBlank(emailField.getText())
                        || isBlank(passwordField.getText())
                        || isBlank(confirmPasswordField.getText())
                        || passwordField.getText().length() < 6
                        || !Objects.equals(passwordField.getText(), confirmPasswordField.getText())
                        || isBlank(prenomField.getText())
                        || isBlank(nomField.getText()),
                emailField.textProperty(),
                passwordField.textProperty(),
                confirmPasswordField.textProperty(),
                prenomField.textProperty(),
                nomField.textProperty()
        );

        // Le bouton "Créer" est désactivé si le formulaire est invalide ou si on est en chargement
        btnCreate.disableProperty().bind(invalidForm.or(loading));

        // Les boutons de navigation sont désactivés pendant le chargement
        if (btnBack != null) {
            btnBack.disableProperty().bind(loading);
        }
        if (btnGotoLogin != null) {
            btnGotoLogin.disableProperty().bind(loading);
        }

        // Le spinner suit l'état "loading"
        if (progress != null) {
            progress.visibleProperty().bind(loading);
            progress.managedProperty().bind(loading);
        }

        clearMessages();
    }

    // ---------------------------------------------------------------------
    // Actions des boutons
    // ---------------------------------------------------------------------

    /**
     * Appelé lorsque l'utilisateur clique sur "Créer mon compte".
     *
     * <p>Étapes :</p>
     * <ol>
     *     <li>Nettoyer les messages d'erreur / info.</li>
     *     <li>Vérifier que le {@link AccountService} est bien injecté.</li>
     *     <li>Récupérer les valeurs des champs (email, mot de passe, nom, prénom).</li>
     *     <li>Appeler {@link AccountService#createAdherentPublic(String, String, String, String)} en asynchrone.</li>
     *     <li>Afficher un message de succès et éventuellement laisser l'utilisateur
     *         cliquer sur "Aller à la page de connexion".</li>
     * </ol>
     */
    @FXML
    private void onCreateClicked() {
        clearMessages();

        if (accountService == null) {
            showError("Service de comptes non initialisé.");
            return;
        }

        final String email  = trim(emailField.getText());
        final String pass   = passwordField.getText();
        final String prenom = trim(prenomField.getText());
        final String nom    = trim(nomField.getText());

        // ⚠ IMPORTANT : ordre des paramètres dans AccountService :
        // createAdherentPublic(String email, String rawPassword, String nom, String prenom)
        runSafely(
                () -> accountService.createAdherentPublic(email, pass, nom, prenom),
                ex -> showError(msg(ex)),
                () -> showInfo("Compte adhérent créé avec succès. Vous pouvez maintenant vous connecter.")
        );
    }

    /**
     * Retour à l’écran d’accueil (Landing).
     * <p>
     * Ici on délègue au Navigator. Dans l’implémentation actuelle,
     * {@link Navigator#logout()} recharge justement la LandingView
     * et réinitialise le Shell.
     */
    @FXML
    private void onBackToLandingClicked() {
        if (navigator != null) {
            navigator.logout();   // retour à l’écran d’accueil
        }
    }

    /**
     * Aller directement à l'écran de connexion (Login).
     */
    @FXML
    private void onGotoLoginClicked() {
        if (navigator != null) {
            navigator.goToLogin();
        }
    }

    // ---------------------------------------------------------------------
    // Helpers asynchrones / UI
    // ---------------------------------------------------------------------

    /**
     * Exécute une opération en arrière-plan, en gérant l'état "loading",
     * les erreurs et une action de succès.
     *
     * @param operation opération à exécuter en asynchrone
     * @param onError   callback appelé si une exception survient
     * @param onSuccess callback appelé si tout se passe bien
     */
    private void runSafely(Runnable operation,
                           Consumer<Throwable> onError,
                           Runnable onSuccess) {

        loading.set(true);

        CompletableFuture
                .runAsync(operation)
                .whenComplete((result, exception) -> Platform.runLater(() -> {
                    loading.set(false);

                    if (exception != null) {
                        onError.accept(exception);
                    } else if (onSuccess != null) {
                        onSuccess.run();
                    }
                }));
    }

    /**
     * Efface les messages d'information et d'erreur.
     */
    private void clearMessages() {
        if (infoLabel != null) {
            infoLabel.setText("");
            infoLabel.setVisible(false);
            infoLabel.setManaged(false);
        }
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    /**
     * Affiche un message d'information (succès).
     */
    private void showInfo(String txt) {
        if (infoLabel != null) {
            infoLabel.setText(txt);
            infoLabel.setVisible(true);
            infoLabel.setManaged(true);
        }
    }

    /**
     * Affiche un message d'erreur formaté.
     */
    private void showError(String txt) {
        if (errorLabel != null) {
            errorLabel.setText("Erreur : " + txt);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    // ---------------------------------------------------------------------
    // Utils simples
    // ---------------------------------------------------------------------

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trim(String s) {
        return (s == null) ? "" : s.trim();
    }

    /**
     * Récupère un message lisible à partir d'une exception,
     * en déroulant éventuellement une CompletionException.
     */
    private static String msg(Throwable t) {
        if (t == null) return "Erreur inconnue";
        Throwable cause = (t instanceof java.util.concurrent.CompletionException && t.getCause() != null)
                ? t.getCause()
                : t;
        return (cause.getMessage() != null && !cause.getMessage().isBlank())
                ? cause.getMessage()
                : cause.toString();
    }
}