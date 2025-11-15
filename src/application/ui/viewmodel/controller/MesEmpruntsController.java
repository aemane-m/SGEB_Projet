package application.ui.viewmodel.controller;

import application.domain.core.emprunt.Emprunt;
import application.ui.viewmodel.MesEmpruntsViewModel;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller FXML pour la vue "Mes emprunts" (espace adhérent).
 *
 * - Affiche uniquement les emprunts de l'adhérent courant.
 * - Permet de basculer entre "en cours" et "historique complet".
 */
public final class MesEmpruntsController {

    // --- Table ---
    @FXML private TableView<Emprunt> table;
    @FXML private TableColumn<Emprunt, String> colDocument;
    @FXML private TableColumn<Emprunt, String> colDateEmprunt;
    @FXML private TableColumn<Emprunt, String> colDatePrevue;
    @FXML private TableColumn<Emprunt, String> colDateRetour;
    @FXML private TableColumn<Emprunt, String> colStatut;

    // --- UI état ---
    @FXML private CheckBox chkActifsOnly;
    @FXML private ProgressIndicator progress;
    @FXML private Label errorLabel;
    @FXML private Button btnRefresh;

    private MesEmpruntsViewModel viewModel;

    public MesEmpruntsController() {
        // JavaFX
    }

    public MesEmpruntsController(MesEmpruntsViewModel vm) {
        this.viewModel = vm;
    }

    public void setViewModel(MesEmpruntsViewModel vm) {
        this.viewModel = vm;
    }

    @FXML
    private void initialize() {
        if (viewModel == null) {
            throw new IllegalStateException("MesEmpruntsViewModel doit être injecté dans MesEmpruntsController.");
        }

        setupTable();
        bindUI();
    }

    private void setupTable() {
        if (table == null) {
            throw new IllegalStateException("TableView<Emprunt> non injectée (vérifie le FXML).");
        }

        table.setItems(viewModel.getEmprunts());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        colDocument.setCellValueFactory(cd ->
            new ReadOnlyStringWrapper(
                safe(cd.getValue().getDocument() != null
                    ? cd.getValue().getDocument().getTitre()
                    : "—")
            )
        );

        colDateEmprunt.setCellValueFactory(cd ->
            new ReadOnlyStringWrapper(toIso(cd.getValue().getDateEmprunt()))
        );
        colDatePrevue.setCellValueFactory(cd ->
            new ReadOnlyStringWrapper(toIso(cd.getValue().getDateRetourPrevue()))
        );
        colDateRetour.setCellValueFactory(cd ->
            new ReadOnlyStringWrapper(toIso(cd.getValue().getDateRetourEffective()))
        );

        colStatut.setCellValueFactory(cd ->
            new ReadOnlyStringWrapper(
                cd.getValue().statut(java.time.LocalDate.now()).name()
            )
        );
    }

    private void bindUI() {
        if (chkActifsOnly != null) {
            chkActifsOnly.selectedProperty().bindBidirectional(viewModel.actifsOnlyProperty());
        }

        if (progress != null) {
            progress.visibleProperty().bind(viewModel.loadingProperty());
            progress.managedProperty().bind(progress.visibleProperty());
        }

        if (errorLabel != null) {
            errorLabel.textProperty().bind(viewModel.errorProperty());
            errorLabel.visibleProperty().bind(viewModel.errorProperty().isNotEmpty());
            errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        }

        if (btnRefresh != null) {
            btnRefresh.disableProperty().bind(viewModel.loadingProperty());
            btnRefresh.setOnAction(e -> viewModel.refresh());
        }
    }

    // --- Helpers locaux ---
    private static String toIso(java.time.LocalDate d) {
        return (d == null) ? "—" : d.toString();
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s.trim();
    }
}