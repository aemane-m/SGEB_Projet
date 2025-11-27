package application.ui.viewmodel.controller;

import application.domain.core.emprunt.Emprunt;
import application.domain.core.personne.Adherent;
import application.ui.viewmodel.AdherentsViewModel;
import application.ui.viewmodel.AdherentsViewModel.AdherentStatusWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur de l'écran "Gestion des adhérents".
 *
 * - Liste des adhérents avec statut (pénalités)
 * - Recherche par id / nom / email
 * - Ajout / édition via un dialogue
 * - Consultation de l'historique des emprunts
 * - Suppression (avec confirmation)
 */
public final class AdherentsController {

    // =====================================================================
    // FXML
    // =====================================================================

    @FXML private TableView<AdherentStatusWrapper> table;
    @FXML private TableColumn<AdherentStatusWrapper, String> colId;
    @FXML private TableColumn<AdherentStatusWrapper, String> colNom;
    @FXML private TableColumn<AdherentStatusWrapper, String> colPrenom;
    @FXML private TableColumn<AdherentStatusWrapper, String> colEmail;
    @FXML private TableColumn<AdherentStatusWrapper, String> colStatut;
    @FXML private TableColumn<AdherentStatusWrapper, Void>   colAction;

    @FXML private TextField         searchField;
    @FXML private Button            btnAdd;
    @FXML private Button            btnSearch;
    @FXML private Button            btnRefresh;
    @FXML private ProgressIndicator progress;
    @FXML private Label             errorLabel;

    // =====================================================================
    // ViewModel
    // =====================================================================

    private AdherentsViewModel viewModel;

    /** Constructeur par défaut (fallback / Scene Builder). */
    public AdherentsController() { }

    /** Constructeur utilisé par FxNavigator (injection explicite). */
    public AdherentsController(AdherentsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public void setViewModel(AdherentsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    // =====================================================================
    // Initialisation FXML
    // =====================================================================

    @FXML
    private void initialize() {
        if (viewModel == null) {
            throw new IllegalStateException("AdherentsViewModel non injecté dans AdherentsController.");
        }

        setupTable();
        setupInteractions();
        setupButtons();
        setupBindings();

        // Chargement initial
        viewModel.refresh();
    }

    // =====================================================================
    // Table : colonnes & interactions
    // =====================================================================

    private void setupTable() {
        table.setItems(viewModel.getItems());

        colId.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue().getAdherent().getId() == null
                        ? "—"
                        : String.valueOf(cd.getValue().getAdherent().getId())
        ));

        colNom.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                safe(cd.getValue().getAdherent().getNom())
        ));

        colPrenom.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                safe(cd.getValue().getAdherent().getPrenom())
        ));

        colEmail.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                safe(cd.getValue().getAdherent().getEmail())
        ));

        colStatut.setCellValueFactory(cd -> cd.getValue().statutProperty());

        colAction.setCellFactory(col -> new ActionButtonCell());
    }

    private void setupInteractions() {
        // Double-clic sur une ligne → ouverture du dialogue d'édition
        table.setRowFactory(tv -> {
            TableRow<AdherentStatusWrapper> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    showAddOrEditAdherentDialog(row.getItem().getAdherent());
                }
            });
            return row;
        });

        // Sélection de la table → ViewModel.selected
        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> viewModel.setSelected(newSel)
        );

        // Changements de selected dans le ViewModel → sélection de la table
        viewModel.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                table.getSelectionModel().clearSelection();
            } else {
                table.getSelectionModel().select(newVal);
            }
        });
    }

    // =====================================================================
    // Boutons & actions
    // =====================================================================

    private void setupButtons() {
        btnSearch.setOnAction(e -> {
            String q = searchField.getText();
            viewModel.searchByIdOrName(q == null ? "" : q.trim());
        });

        btnRefresh.setOnAction(e -> viewModel.refresh());

        btnAdd.setOnAction(e -> showAddOrEditAdherentDialog(null));
    }

    private void setupBindings() {
        // Loading
        progress.visibleProperty().bind(viewModel.loadingProperty());
        progress.managedProperty().bind(progress.visibleProperty());

        // Erreur
        errorLabel.textProperty().bind(viewModel.errorProperty());
        errorLabel.visibleProperty().bind(viewModel.errorProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        // Désactivation des boutons pendant les opérations
        btnAdd.disableProperty().bind(viewModel.loadingProperty());
        btnSearch.disableProperty().bind(viewModel.loadingProperty());
        btnRefresh.disableProperty().bind(viewModel.loadingProperty());
    }

    // =====================================================================
    // Dialog : création / édition d’un adhérent
    // =====================================================================

    private void showAddOrEditAdherentDialog(Adherent existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter un utilisateur" : "Modifier l'utilisateur");

        // Thème / CSS
        addTheme(dialog.getDialogPane());

        dialog.getDialogPane().setMinWidth(600);
        dialog.getDialogPane().setPrefWidth(600);

        boolean isCreating = (existing == null);

        if (isCreating) {
            viewModel.createNew();
        } else {
            viewModel.edit(existing);
        }

        dialog.getDialogPane().setContent(createAdherentFormPane(isCreating));

        ButtonType BTN_SAVE = new ButtonType(
                isCreating ? "Créer" : "Mettre à jour",
                ButtonBar.ButtonData.OK_DONE
        );
        dialog.getDialogPane().getButtonTypes().addAll(BTN_SAVE, ButtonType.CANCEL);

        styleAsPrimary(dialog.getDialogPane(), BTN_SAVE);
        styleAsSecondary(dialog.getDialogPane(), ButtonType.CANCEL);

        Button btnSave = (Button) dialog.getDialogPane().lookupButton(BTN_SAVE);

        // Validation
        if (isCreating) {
            // Création : nom, prénom, email, rôle, mot de passe temporaire (min 8)
            btnSave.disableProperty().bind(
                    viewModel.formNomProperty().isEmpty()
                            .or(viewModel.formPrenomProperty().isEmpty())
                            .or(viewModel.formEmailProperty().isEmpty())
                            .or(viewModel.formRoleProperty().isEmpty())
                            .or(viewModel.formPasswordProperty().length().lessThan(8))
                            .or(viewModel.loadingProperty())
            );
        } else {
            // Édition : nom / prénom / email obligatoires
            btnSave.disableProperty().bind(
                    viewModel.formNomProperty().isEmpty()
                            .or(viewModel.formPrenomProperty().isEmpty())
                            .or(viewModel.formEmailProperty().isEmpty())
                            .or(viewModel.loadingProperty())
            );
        }

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == BTN_SAVE) {
            viewModel.saveCurrent();
        } else {
            // Annulation : on rétablit le selected dans le formulaire
            viewModel.edit(viewModel.getSelectedAdherent());
        }
    }

    /**
     * Formulaire principal utilisé dans le dialog de création / édition.
     */
    private Node createAdherentFormPane(boolean isCreating) {
        TitledPane wrapper = new TitledPane();
        wrapper.setText(isCreating ? "Nouvel utilisateur" : "Éditer l'utilisateur");
        wrapper.setExpanded(true);
        wrapper.setCollapsible(false);
        wrapper.getStyleClass().add("card-like");

        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(16));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(30);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(70);
        form.getColumnConstraints().addAll(col1, col2);

        TextField tfNom       = new TextField();
        TextField tfPrenom    = new TextField();
        TextField tfEmail     = new TextField();
        TextField tfTelephone = new TextField();

        tfNom.textProperty().bindBidirectional(viewModel.formNomProperty());
        tfPrenom.textProperty().bindBidirectional(viewModel.formPrenomProperty());
        tfEmail.textProperty().bindBidirectional(viewModel.formEmailProperty());
        tfTelephone.textProperty().bindBidirectional(viewModel.formTelephoneProperty());
        tfTelephone.setPromptText(AdherentsViewModel.getTelephoneHint());

        tfNom.getStyleClass().add("form-field");
        tfPrenom.getStyleClass().add("form-field");
        tfEmail.getStyleClass().add("form-field");
        tfTelephone.getStyleClass().add("form-field");

        int row = 0;

        form.add(createFormLabel("Nom"),       0, row);
        form.add(tfNom,                        1, row++);

        form.add(createFormLabel("Prénom"),    0, row);
        form.add(tfPrenom,                     1, row++);

        form.add(createFormLabel("Email"),     0, row);
        form.add(tfEmail,                      1, row++);

        form.add(createFormLabel("Téléphone"), 0, row);
        form.add(tfTelephone,                  1, row++);

        if (isCreating) {
            // --- Création : rôle + mot de passe temporaire ---
            ComboBox<String> cbRole = new ComboBox<>();
            cbRole.getItems().addAll("ADHERENT", "ADMIN");
            cbRole.setPromptText("Choisir un rôle");
            cbRole.valueProperty().bindBidirectional(viewModel.formRoleProperty());
            cbRole.getStyleClass().add("form-field");

            form.add(createFormLabel("Rôle"), 0, row);
            form.add(cbRole,                 1, row++);

            PasswordField pfPassword = new PasswordField();
            pfPassword.setPromptText("Mot de passe temporaire (min. 8 caractères)");
            pfPassword.textProperty().bindBidirectional(viewModel.formPasswordProperty());
            pfPassword.getStyleClass().add("form-field");

            form.add(createFormLabel("Mot de passe temporaire"), 0, row);
            form.add(pfPassword,                                 1, row++);

            Label hint = new Label(
                    "Ce mot de passe est temporaire. L'utilisateur devra le modifier lors " +
                    "de sa première connexion."
            );
            hint.getStyleClass().add("hint-label");
            hint.setWrapText(true);
            hint.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(hint, Priority.ALWAYS);

            form.add(new Label(), 0, row);
            form.add(hint,        1, row++);

        } else {
            // --- Édition : nouveau mot de passe optionnel + forcer le changement ---
            PasswordField pfNewPassword = new PasswordField();
            pfNewPassword.setPromptText("Nouveau mot de passe (laisser vide pour ne pas changer)");
            pfNewPassword.textProperty().bindBidirectional(viewModel.formNewPasswordProperty());
            pfNewPassword.getStyleClass().add("form-field");

            form.add(createFormLabel("Nouveau mot de passe"), 0, row);
            form.add(pfNewPassword,                           1, row++);

            CheckBox chkForceChange = new CheckBox(
                    "Forcer l'utilisateur à changer son mot de passe à la prochaine connexion"
            );
            chkForceChange.setWrapText(true);
            chkForceChange.selectedProperty().bindBidirectional(
                    viewModel.formForcePasswordChangeProperty()
            );
            chkForceChange.getStyleClass().add("form-checkbox");

            form.add(new Label(),      0, row);
            form.add(chkForceChange,   1, row++);
        }

        wrapper.setContent(form);
        return wrapper;
    }

    private Label createFormLabel(String text) {
        Label label = new Label(text + " :");
        label.getStyleClass().add("form-label");
        return label;
    }

    // =====================================================================
    // Historique des emprunts
    // =====================================================================

    private void showHistoriqueDialog(Adherent adherent) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Historique des emprunts");
        dlg.setHeaderText("Historique pour : " + adherent.getNomComplet());
        addTheme(dlg.getDialogPane());

        TableView<Emprunt> historiqueTable = new TableView<>();
        historiqueTable.setItems(viewModel.getSelectedAdherentHistorique());

        TableColumn<Emprunt, String> colDoc = new TableColumn<>("Document");
        colDoc.setCellValueFactory(cd -> {
            String titre = cd.getValue().getDocument().getTitre();
            return new ReadOnlyStringWrapper(safe(titre));
        });

        TableColumn<Emprunt, String> colDateEmp = new TableColumn<>("Date Emprunt");
        colDateEmp.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getDateEmprunt().toString())
        );

        TableColumn<Emprunt, String> colDateRetPrev = new TableColumn<>("Retour Prévu");
        colDateRetPrev.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getDateRetourPrevue().toString())
        );

        TableColumn<Emprunt, String> colDateRetEff = new TableColumn<>("Retour Effectif");
        colDateRetEff.setCellValueFactory(cd -> {
            LocalDate date = cd.getValue().getDateRetourEffective();
            return new ReadOnlyStringWrapper(date == null ? "EN COURS" : date.toString());
        });

        historiqueTable.getColumns().setAll(List.of(colDoc, colDateEmp, colDateRetPrev, colDateRetEff));        
        historiqueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        historiqueTable.setPrefHeight(300);
        historiqueTable.setPrefWidth(550);
        historiqueTable.setPlaceholder(new Label("Aucun emprunt dans l'historique."));

        VBox content = new VBox(10, historiqueTable);
        content.setPadding(new Insets(10));

        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Charge l'historique pour cet adhérent
        viewModel.loadHistoriqueFor(adherent);

        dlg.showAndWait();
    }

    // =====================================================================
    // Helpers UI (thème, styles, utils)
    // =====================================================================

    private void addTheme(DialogPane pane) {
        try {
            URL css = getClass().getResource("/application/ui/css/style.css");
            if (css != null) {
                pane.getStylesheets().add(css.toExternalForm());
                pane.getStyleClass().add("entity-dialog");
            }
        } catch (Exception ignored) {
            System.err.println("Warning: Could not load CSS for dialog.");
        }
    }

    private void styleAsPrimary(DialogPane pane, ButtonType type) {
        Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn-primary");
            button.setDefaultButton(true);
        }
    }

    private void styleAsDanger(DialogPane pane, ButtonType type) {
        Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn-danger");
        }
    }

    private void styleAsSecondary(DialogPane pane, ButtonType type) {
        Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn-secondary");
        }
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    // =====================================================================
    // Cellule d’actions (Modifier / Historique / Supprimer)
    // =====================================================================

    private final class ActionButtonCell extends TableCell<AdherentStatusWrapper, Void> {

        private final Button btnEdit    = new Button("Modifier");
        private final Button btnHistory = new Button("Historique");
        private final Button btnDelete  = new Button("Supprimer");
        private final HBox   pane       = new HBox(8, btnEdit, btnHistory, btnDelete);

        private ActionButtonCell() {
            btnEdit.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
            btnHistory.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
            btnDelete.getStyleClass().addAll("btn", "btn-danger", "btn-sm");

            btnEdit.disableProperty().bind(viewModel.loadingProperty());
            btnHistory.disableProperty().bind(viewModel.loadingProperty());
            btnDelete.disableProperty().bind(viewModel.loadingProperty());

            btnEdit.setOnAction(event -> {
                AdherentStatusWrapper wrap = getWrapper();
                if (wrap != null) {
                    showAddOrEditAdherentDialog(wrap.getAdherent());
                }
            });

            btnHistory.setOnAction(event -> {
                AdherentStatusWrapper wrap = getWrapper();
                if (wrap != null) {
                    showHistoriqueDialog(wrap.getAdherent());
                }
            });

            btnDelete.setOnAction(event -> {
                AdherentStatusWrapper wrap = getWrapper();
                if (wrap != null) {
                    confirmAndDeleteAdherent(wrap.getAdherent());
                }
            });
        }

        private AdherentStatusWrapper getWrapper() {
            if (getTableView() == null || getIndex() < 0) {
                return null;
            }
            var items = getTableView().getItems();
            if (items == null || getIndex() >= items.size()) {
                return null;
            }
            return items.get(getIndex());
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty || getWrapper() == null ? null : pane);
        }
    }

    // =====================================================================
    // Suppression d’un adhérent
    // =====================================================================

    private void confirmAndDeleteAdherent(Adherent adherent) {
        if (adherent == null) {
            viewModel.reportError("Impossible de supprimer : aucun adhérent fourni.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setHeaderText("Supprimer « " + safe(adherent.getNomComplet()) + " » ?");
        alert.setContentText(
                "Cette action est irréversible. La suppression échouera si cet adhérent a des emprunts en cours."
        );

        addTheme(alert.getDialogPane());
        styleAsDanger(alert.getDialogPane(), ButtonType.OK);

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            viewModel.delete(adherent);
        }
    }
}