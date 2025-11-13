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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Gère l'affichage du statut (pénalités) et ouvre un dialogue pour l'historique (emprunts).
 * Utilise AdherentStatusWrapper pour la TableView.
 */
public class AdherentsController {

    // --- FXML Fields ---
    @FXML private TableView<AdherentStatusWrapper> table;
    @FXML private TableColumn<AdherentStatusWrapper, String> colId;
    @FXML private TableColumn<AdherentStatusWrapper, String> colNom;
    @FXML private TableColumn<AdherentStatusWrapper, String> colPrenom;
    @FXML private TableColumn<AdherentStatusWrapper, String> colEmail;
    @FXML private TableColumn<AdherentStatusWrapper, String> colStatut;
    @FXML private TableColumn<AdherentStatusWrapper, Void>   colAction;

    @FXML private TextField        searchField;
    @FXML private Button           btnAdd;
    @FXML private Button           btnSearch;
    @FXML private Button           btnRefresh;
    @FXML private ProgressIndicator progress;
    @FXML private Label             errorLabel;

    // --- ViewModel ---
    private AdherentsViewModel viewModel;

    // --- Constructeurs / Injection ---
    public AdherentsController() {}
    public AdherentsController(AdherentsViewModel vm) { this.viewModel = vm; }
    public void setViewModel(AdherentsViewModel vm)   { this.viewModel = vm; }

    @FXML
    private void initialize() {
        if (viewModel == null) {
            throw new IllegalStateException("ViewModel non injecté.");
        }
        setupTableColumns();
        setupTableInteractions();
        setupActionButtons();
        bindUIControls();
    }

    // ========================================================================
    // Table : colonnes & interactions
    // ========================================================================

    private void setupTableColumns() {
        table.setItems(viewModel.getItems());

        // Colonnes simples via wrapper.getAdherent()
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

        // Le statut est déjà calculé côté wrapper (binding direct)
        colStatut.setCellValueFactory(cellData -> cellData.getValue().statutProperty());

        // Colonne actions (éditer, historique, supprimer)
        colAction.setCellFactory(col -> new ActionButtonCell());
    }

    private void setupTableInteractions() {
        // Double-clic sur une ligne pour ouvrir le dialogue d'édition
        table.setRowFactory(tv -> {
            final TableRow<AdherentStatusWrapper> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    showAddOrEditAdherentDialog(row.getItem().getAdherent());
                }
            });
            return row;
        });

        // Lier sélection Table <-> ViewModel
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) ->
            viewModel.setSelected(newSel)
        );

        viewModel.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                table.getSelectionModel().clearSelection();
            } else {
                table.getSelectionModel().select(newVal);
            }
        });
    }

    private void setupActionButtons() {
        btnSearch.setOnAction(e -> {
            final String q = searchField.getText();
            viewModel.searchByIdOrName(q == null ? "" : q.trim());
        });

        btnRefresh.setOnAction(e -> viewModel.refresh());

        btnAdd.setOnAction(e -> showAddOrEditAdherentDialog(null));
    }

    private void bindUIControls() {
        progress.visibleProperty().bind(viewModel.loadingProperty());
        progress.managedProperty().bind(progress.visibleProperty());

        errorLabel.textProperty().bind(viewModel.errorProperty());
        errorLabel.visibleProperty().bind(viewModel.errorProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        btnAdd.disableProperty().bind(viewModel.loadingProperty());
        btnSearch.disableProperty().bind(viewModel.loadingProperty());
        btnRefresh.disableProperty().bind(viewModel.loadingProperty());
    }

    // ========================================================================
    // Dialogs
    // ========================================================================

    private void showAddOrEditAdherentDialog(Adherent existing) {
        final Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(existing == null ? "Ajouter un adhérent" : "Modifier l'adhérent");
        addTheme(dlg.getDialogPane());

        if (existing == null) {
            viewModel.createNew();
        } else {
            viewModel.edit(existing);
        }

        dlg.getDialogPane().setContent(createAdherentFormPane(existing == null));

        final ButtonType BTN_SAVE = new ButtonType(
            existing == null ? "Enregistrer" : "Mettre à jour",
            ButtonBar.ButtonData.OK_DONE
        );
        dlg.getDialogPane().getButtonTypes().addAll(BTN_SAVE, ButtonType.CANCEL);
        styleAsPrimary(dlg.getDialogPane(), BTN_SAVE);

        final Button btnSave = (Button) dlg.getDialogPane().lookupButton(BTN_SAVE);
        btnSave.disableProperty().bind(
            viewModel.formNomProperty().isEmpty()
                .or(viewModel.formPrenomProperty().isEmpty())
                .or(viewModel.formEmailProperty().isEmpty()) // TODO: ajouter validation email rigoureuse
                .or(viewModel.loadingProperty())
        );

        final Optional<ButtonType> res = dlg.showAndWait();
        if (res.isPresent() && res.get() == BTN_SAVE) {
            viewModel.saveCurrent();
        } else {
            // Annulé : recharger l'édition avec l'adhérent sélectionné (ou vide)
            viewModel.edit(viewModel.getSelectedAdherent());
        }
    }

    private Node createAdherentFormPane(boolean isCreating) {
        final TitledPane pane = new TitledPane();
        pane.setText(isCreating ? "Nouvel adhérent" : "Éditer l'adhérent");
        pane.setExpanded(true);
        pane.setCollapsible(false);
        pane.getStyleClass().addAll("titled-pane", "card-like");

        final GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(16));

        final ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(30);
        final ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(70);
        form.getColumnConstraints().addAll(col1, col2);

        final TextField tfNom       = new TextField();
        final TextField tfPrenom    = new TextField();
        final TextField tfEmail     = new TextField();
        final TextField tfTelephone = new TextField();

        tfNom.textProperty().bindBidirectional(viewModel.formNomProperty());
        tfPrenom.textProperty().bindBidirectional(viewModel.formPrenomProperty());
        tfEmail.textProperty().bindBidirectional(viewModel.formEmailProperty());
        tfTelephone.textProperty().bindBidirectional(viewModel.formTelephoneProperty());

        int rowIndex = 0;
        form.add(new Label("Nom:"),        0, rowIndex);
        form.add(tfNom,                    1, rowIndex++);

        form.add(new Label("Prénom:"),     0, rowIndex);
        form.add(tfPrenom,                 1, rowIndex++);

        form.add(new Label("Email:"),      0, rowIndex);
        form.add(tfEmail,                  1, rowIndex++);

        form.add(new Label("Téléphone:"),  0, rowIndex);
        form.add(tfTelephone,              1, rowIndex++);

        pane.setContent(form);
        return pane;
    }

    private void confirmAndDeleteAdherent(Adherent adherent) {
        if (adherent == null) {
            viewModel.reportError("Impossible de supprimer : aucun adhérent fourni.");
            return;
        }

        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setHeaderText("Supprimer « " + safe(adherent.getNomComplet()) + " » ?");
        alert.setContentText(
            "Cette action est irréversible. La suppression échouera si cet adhérent a des emprunts en cours."
        );
        addTheme(alert.getDialogPane());
        styleAsDanger(alert.getDialogPane(), ButtonType.OK);

        final Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            viewModel.delete(adherent);
        }
    }


 // --- Dialogue d'Historique ---
    private void showHistoriqueDialog(Adherent adherent) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Historique des emprunts");
        dlg.setHeaderText("Historique pour : " + adherent.getNomComplet());
        addTheme(dlg.getDialogPane());

        TableView<Emprunt> historiqueTable = new TableView<>();
        historiqueTable.setItems(viewModel.getSelectedAdherentHistorique());

        // --- CORRECTION DU TODO ---
        TableColumn<Emprunt, String> colDoc = new TableColumn<>("Document");
        colDoc.setCellValueFactory(cd -> {
            // Le repository a peuplé le titre dans le proxy
            String titre = cd.getValue().getDocument().getTitre();
            return new ReadOnlyStringWrapper(safe(titre));
        });
        
        TableColumn<Emprunt, String> colDateEmp = new TableColumn<>("Date Emprunt");
        colDateEmp.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
            cd.getValue().getDateEmprunt().toString()
        ));
        
        TableColumn<Emprunt, String> colDateRetPrev = new TableColumn<>("Retour Prévu");
        colDateRetPrev.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
            cd.getValue().getDateRetourPrevue().toString()
        ));
        
        TableColumn<Emprunt, String> colDateRetEff = new TableColumn<>("Retour Effectif");
        colDateRetEff.setCellValueFactory(cd -> {
            LocalDate date = cd.getValue().getDateRetourEffective();
            return new ReadOnlyStringWrapper(date == null ? "EN COURS" : date.toString());
        });

        historiqueTable.getColumns().setAll(colDoc, colDateEmp, colDateRetPrev, colDateRetEff);
        historiqueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS); // JavaFX 21+
        historiqueTable.setPrefHeight(300);
        historiqueTable.setPrefWidth(550);
        historiqueTable.setPlaceholder(new Label("Aucun emprunt dans l'historique."));
        
        VBox content = new VBox(10, historiqueTable);
        content.setPadding(new Insets(10));
        
        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Charge les données
        viewModel.loadHistoriqueFor(adherent);
        
        dlg.showAndWait();
    }
    // ========================================================================
    // Helpers UI (thème, styles, utils)
    // ========================================================================

    private void addTheme(DialogPane pane) {
        try {
            final URL css = getClass().getResource("/application/ui/css/style.css");
            if (css != null) {
                pane.getStylesheets().add(css.toExternalForm());
                pane.getStyleClass().add("entity-dialog");
            }
        } catch (Exception ignored) {
            System.err.println("Warning: Could not load CSS for dialog.");
        }
    }

    private void styleAsPrimary(DialogPane pane, ButtonType type) {
        final Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn", "btn-primary");
            button.setDefaultButton(true);
        }
    }

    private void styleAsDanger(DialogPane pane, ButtonType type) {
        final Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn", "btn-danger");
        }
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    // ========================================================================
    // Inner class : cellule d’actions
    // ========================================================================

    private class ActionButtonCell extends TableCell<AdherentStatusWrapper, Void> {
        private final Button btnEdit    = new Button("Modifier");
        private final Button btnHistory = new Button("Historique");
        private final Button btnDelete  = new Button("Supprimer");
        private final HBox   pane       = new HBox(8, btnEdit, btnHistory, btnDelete);

        ActionButtonCell() {
            btnEdit.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
            btnHistory.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
            btnDelete.getStyleClass().addAll("btn", "btn-danger", "btn-sm");

            btnEdit.disableProperty().bind(viewModel.loadingProperty());
            btnHistory.disableProperty().bind(viewModel.loadingProperty());
            btnDelete.disableProperty().bind(viewModel.loadingProperty());

            btnEdit.setOnAction(event -> {
                final AdherentStatusWrapper wrap = getWrapper();
                if (wrap != null) {
                    showAddOrEditAdherentDialog(wrap.getAdherent());
                }
            });

            btnHistory.setOnAction(event -> {
                final AdherentStatusWrapper wrap = getWrapper();
                if (wrap != null) {
                    showHistoriqueDialog(wrap.getAdherent());
                }
            });

            btnDelete.setOnAction(event -> {
                final AdherentStatusWrapper wrap = getWrapper();
                if (wrap != null) {
                    confirmAndDeleteAdherent(wrap.getAdherent());
                }
            });
        }

        private AdherentStatusWrapper getWrapper() {
            if (getTableView() == null || getIndex() < 0) return null;
            final var items = getTableView().getItems();
            if (items == null || getIndex() >= items.size()) return null;
            return items.get(getIndex());
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty || getWrapper() == null ? null : pane);
        }
    }
}
