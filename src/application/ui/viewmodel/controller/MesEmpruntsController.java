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
    @FXML private Label successLabel;
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
        setupBorrowSection();
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
                cd.getValue().statut(LocalDate.now()).name()
            )
        );
    }

    // ----------------------------------------------------------------
    // Bindings généraux (filtres, erreurs, succès, refresh)
    // ----------------------------------------------------------------

    private void bindUI() {
        if (chkActifsOnly != null) {
            // Lien bidirectionnel avec le filtre du ViewModel
            chkActifsOnly.selectedProperty().bindBidirectional(viewModel.actifsOnlyProperty());
        }

        if (progress != null) {
            progress.visibleProperty().bind(viewModel.loadingProperty());
            progress.managedProperty().bind(progress.visibleProperty());
        }

        if (errorLabel != null) {
            errorLabel.textProperty().bind(viewModel.errorProperty());
            // Visible seulement si erreur non vide et pas de message de succès
            errorLabel.visibleProperty().bind(
                viewModel.errorProperty().isNotEmpty()
                    .and(viewModel.successProperty().isEmpty())
            );
            errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        }

        if (successLabel != null) {
            successLabel.textProperty().bind(viewModel.successProperty());
            // Visible seulement si succès non vide et pas d'erreur
            successLabel.visibleProperty().bind(
                viewModel.successProperty().isNotEmpty()
                    .and(viewModel.errorProperty().isEmpty())
            );
            successLabel.managedProperty().bind(successLabel.visibleProperty());
        }

        if (btnRefresh != null) {
            btnRefresh.disableProperty().bind(viewModel.loadingProperty());
            btnRefresh.setOnAction(e -> {
                viewModel.refresh();
                viewModel.refreshAvailableDocuments();
            });
        }
    }

    // ----------------------------------------------------------------
    // Zone d’emprunt adhérent
    // ----------------------------------------------------------------

    private void setupBorrowSection() {
        if (cbDocumentDisponible == null || dpDateEmprunt == null || btnBorrow == null) {
            // Si on ouvre la vue dans SceneBuilder sans tout initialiser
            return;
        }

        // Liste des documents disponibles (déjà filtrés dans le ViewModel
        // pour ne proposer que les documents non empruntés / autorisés)
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
                return null; // non utilisé
            }
        });

        // Date d’emprunt par défaut : aujourd’hui
        dpDateEmprunt.setValue(LocalDate.now());

        // Bouton désactivé si chargement OU pas de document OU pas de date
        btnBorrow.disableProperty().bind(
            viewModel.loadingProperty()
                .or(cbDocumentDisponible.valueProperty().isNull())
                .or(dpDateEmprunt.valueProperty().isNull())
        );

        btnBorrow.setOnAction(e -> {
            Document doc = cbDocumentDisponible.getValue();
            LocalDate dateEmprunt = dpDateEmprunt.getValue();

            if (doc == null || dateEmprunt == null) {
                // Sécurité supplémentaire (au cas où le binding serait cassé)
                viewModel.reportError("Veuillez choisir un document et une date d'emprunt.");
                return;
            }

            // Toutes les règles métier (limite de prêts, pénalités non payées, etc.)
            // sont levées sous forme d'exceptions par EmpruntService.borrow(...)
            // et remontent dans viewModel.errorProperty(), donc visibles ici.
            viewModel.borrowForCurrentAdherent(doc, dateEmprunt);
        });
    }

    // --- Helpers locaux ---
    private static String toIso(LocalDate d) {
        return (d == null) ? "—" : d.toString();
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s.trim();
    }
}