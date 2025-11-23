package application.ui.viewmodel.controller;

import application.domain.service.AccountService;
import application.ui.viewmodel.AccountsViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * Contrôleur de la vue de gestion des comptes (ADMIN uniquement).
 *
 * - Affiche tous les comptes (admins + adhérents)
 * - Permet d'activer / suspendre un compte
 * - Permet de supprimer un compte (avec confirmation)
 */
public final class AccountsController {

    @FXML private VBox root;

    @FXML private TableView<AccountService.UserAccountSummary> table;
    @FXML private TableColumn<AccountService.UserAccountSummary, String> colEmail;
    @FXML private TableColumn<AccountService.UserAccountSummary, String> colRole;
    @FXML private TableColumn<AccountService.UserAccountSummary, String> colStatus;
    @FXML private TableColumn<AccountService.UserAccountSummary, String> colPwdMustChange;

    @FXML private Button btnRefresh;
    @FXML private Button btnToggleStatus;
    @FXML private Button btnDelete;

    @FXML private Label errorLabel;
    @FXML private ProgressIndicator progress;

    private final AccountsViewModel viewModel;

    public AccountsController(AccountsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    // =====================================================================
    // Initialisation FXML
    // =====================================================================

    @FXML
    private void initialize() {
        setupTable();
        setupBindings();
        setupActions();

        // Charge la liste au premier affichage
        viewModel.refresh();
    }

    // =====================================================================
    // Table & colonnes
    // =====================================================================

    private void setupTable() {
        table.setItems(viewModel.getAccounts());

        colEmail.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().email())
        );

        colRole.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().roleName())
        );

        colStatus.setCellValueFactory(cd -> {
            String raw = cd.getValue().status();
            String label;

            if (raw == null) {
                label = "Inconnu";
            } else {
                switch (raw.toUpperCase()) {
                    case "ACTIVE"    -> label = "Actif";
                    case "PENDING"   -> label = "En attente";
                    case "SUSPENDED" -> label = "Suspendu";
                    default          -> label = raw;
                }
            }
            return new SimpleStringProperty(label);
        });

        colPwdMustChange.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().passwordMustChange() ? "Oui" : "Non")
        );

        // Propagation de la sélection vers le ViewModel
        table.getSelectionModel()
             .selectedItemProperty()
             .addListener((obs, oldVal, newVal) -> viewModel.setSelected(newVal));
    }

    // =====================================================================
    // Bindings UI ↔ ViewModel
    // =====================================================================

    private void setupBindings() {
        // Boutons
        btnRefresh.disableProperty().bind(viewModel.loadingProperty());

        btnToggleStatus.disableProperty().bind(
                viewModel.loadingProperty().or(
                        Bindings.isNull(viewModel.selectedProperty())
                )
        );

        btnDelete.disableProperty().bind(
                viewModel.loadingProperty().or(
                        Bindings.isNull(viewModel.selectedProperty())
                )
        );

        // Erreurs
        errorLabel.textProperty().bind(viewModel.errorProperty());
        errorLabel.visibleProperty().bind(viewModel.errorProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        // Indicateur de chargement
        progress.visibleProperty().bind(viewModel.loadingProperty());
        progress.managedProperty().bind(progress.visibleProperty());
    }

    // =====================================================================
    // Actions
    // =====================================================================

    private void setupActions() {
        btnRefresh.setOnAction(e -> viewModel.refresh());
        btnToggleStatus.setOnAction(e -> viewModel.toggleStatusForSelected());
        btnDelete.setOnAction(e -> onDeleteAccountClicked());
    }

    // =====================================================================
    //  Confirmation et suppression
    // =====================================================================

    @FXML
    private void onDeleteAccountClicked() {
        AccountService.UserAccountSummary selectedAccount =
                table.getSelectionModel().getSelectedItem();

        if (selectedAccount == null) {
            showInfo("Suppression de compte", "Aucun compte sélectionné.");
            return;
        }

        if (!confirmDeletion(selectedAccount)) {
            // L’utilisateur a annulé
            return;
        }

        // La logique métier est dans le ViewModel
        viewModel.deleteSelected();
    }

    private boolean confirmDeletion(AccountService.UserAccountSummary account) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer ce compte ?");
        alert.setContentText(
                "Email : " + account.email() + "\n" +
                "Rôle  : " + account.roleName() + "\n\n" +
                "Cette action est irréversible."
        );

        ButtonType btnSupprimer = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler   = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnSupprimer, btnAnnuler);

        var result = alert.showAndWait();
        return result.isPresent() && result.get() == btnSupprimer;
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}