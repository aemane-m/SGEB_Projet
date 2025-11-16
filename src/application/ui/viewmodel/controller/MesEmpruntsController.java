package application.ui.viewmodel.controller;

import application.domain.core.document.Document;
import application.domain.core.emprunt.Emprunt;
import application.ui.viewmodel.MesEmpruntsViewModel;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.LocalDate;

/**
 * Controller FXML pour la vue "Mes emprunts" (espace adhérent).
 *
 * - Affiche uniquement les emprunts de l'adhérent courant.
 * - Permet de basculer entre "en cours" et "historique complet".
 * - Permet à l'adhérent de créer un nouvel emprunt (si les règles métier le permettent).
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

    // --- Zone d’emprunt adhérent ---
    @FXML private ComboBox<Document> cbDocumentDisponible;
    @FXML private DatePicker dpDateEmprunt;
    @FXML private Button btnBorrow;

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
            throw new IllegalStateException(
                "MesEmpruntsViewModel doit être injecté dans MesEmpruntsController."
            );
        }

        setupTable();
        bindUI();
        setupBorrowSection();   // <--- nouveau
    }

    // ----------------------------------------------------------------
    // Table des emprunts
    // ----------------------------------------------------------------

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

    // ----------------------------------------------------------------
    // Bindings généraux (filtres, erreurs, refresh)
    // ----------------------------------------------------------------

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

    // ----------------------------------------------------------------
    // Zone d’emprunt adhérent
    // ----------------------------------------------------------------

    private void setupBorrowSection() {
        if (cbDocumentDisponible == null || dpDateEmprunt == null || btnBorrow == null) {
            // Si tu testes la vue dans SceneBuilder sans ces champs, on évite un NPE
            return;
        }

        // Liste des documents disponibles pour CET adhérent
        cbDocumentDisponible.setItems(viewModel.getAvailableDocuments());

        cbDocumentDisponible.setConverter(new StringConverter<>() {
            @Override
            public String toString(Document doc) {
                if (doc == null) return "";
                String base = doc.getTitre();
                Long id = doc.getId();
                return (id != null) ? base + " (ID: " + id + ")" : base;
            }

            @Override
            public Document fromString(String s) {
                return null; // pas utilisé
            }
        });

        // Date d’emprunt par défaut : aujourd’hui
        dpDateEmprunt.setValue(LocalDate.now());

        // Désactivation du bouton si :
        // - chargement en cours
        // - aucun document sélectionné
        // - aucune date sélectionnée
        btnBorrow.disableProperty().bind(
                viewModel.loadingProperty()
                        .or(cbDocumentDisponible.valueProperty().isNull())
                        .or(dpDateEmprunt.valueProperty().isNull())
        );

        btnBorrow.setOnAction(e -> {
            Document doc = cbDocumentDisponible.getValue();
            LocalDate dateEmprunt = dpDateEmprunt.getValue();

            if (doc == null || dateEmprunt == null) {
                // Par sécurité, même si c'est déjà filtré par le disableProperty
                viewModel.reportError("Veuillez choisir un document et une date d'emprunt.");
                return;
            }

            viewModel.borrowForCurrentAdherent(doc, dateEmprunt);
        });
    }

    // --- Helpers locaux ---
    private static String toIso(java.time.LocalDate d) {
        return (d == null) ? "—" : d.toString();
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s.trim();
    }
}