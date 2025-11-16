package application.ui.viewmodel.controller;

import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Penalite;
import application.ui.viewmodel.PenalitesViewModel;
import application.ui.viewmodel.PenalitesViewModel.AdherentPenaliteSummary;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Controller pour la vue de gestion des pénalités (Maître-Détail).
 */
public class PenalitesController {

    // --- Table Maître ---
    @FXML private TableView<AdherentPenaliteSummary> tableAdherents;
    @FXML private TableColumn<AdherentPenaliteSummary, String> colAdherentNom;
    @FXML private TableColumn<AdherentPenaliteSummary, String> colPenaliteCount;
    @FXML private TableColumn<AdherentPenaliteSummary, String> colPenaliteTotal;
    @FXML private TableColumn<AdherentPenaliteSummary, String> colPenaliteRequested;

    // --- Table Détail ---
    @FXML private TableView<Penalite> tableDetails;
    @FXML private TableColumn<Penalite, String> colDetailDoc;
    @FXML private TableColumn<Penalite, String> colDetailDate;
    @FXML private TableColumn<Penalite, String> colDetailMontant;
    @FXML private TableColumn<Penalite, Void>   colDetailAction;

    // --- Panneau de détails ---
    @FXML private VBox detailsPane;
    @FXML private Button btnReglerTout;

    // --- Champs UI globaux ---
    @FXML private ProgressIndicator progress;
    @FXML private Button btnRefresh;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private PenalitesViewModel viewModel;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // --- Constructeurs ---
    public PenalitesController() { }
    public PenalitesController(PenalitesViewModel vm) { this.viewModel = vm; }
    public void setViewModel(PenalitesViewModel vm) { this.viewModel = vm; }

    @FXML
    private void initialize() {
        if (viewModel == null) {
            throw new IllegalStateException("PenalitesViewModel non injecté dans PenalitesController.");
        }

        setupMasterTable();
        setupDetailTable();
        bindUIState();
    }

    // =====================================================================
    // ======================= Table Maître (Adhérents) =====================
    // =====================================================================

    private void setupMasterTable() {
        tableAdherents.setItems(viewModel.getAdherentSummaries());

        colAdherentNom.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().adherent().getNomComplet())
        );

        colPenaliteCount.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(String.valueOf(cd.getValue().count()))
        );

        colPenaliteTotal.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().total().toString()) // ex: "12.50 EUR"
        );

        // NOUVEAU : colonne "Demandes de paiement"
        if (colPenaliteRequested != null) {
            colPenaliteRequested.setCellValueFactory(cd ->
                    new ReadOnlyStringWrapper(String.valueOf(cd.getValue().requestedCount()))
            );
        }

        // Sélection d’un adhérent => on affiche / masque le panneau de détails
        tableAdherents.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            viewModel.selectedSummaryProperty().set(newSel);

            boolean showDetails = (newSel != null);
            if (detailsPane != null) {
                detailsPane.setVisible(showDetails);
                detailsPane.setManaged(showDetails);
            }
        });
    }

    // =====================================================================
    // ====================== Table Détail (Pénalités) ======================
    // =====================================================================

    private void setupDetailTable() {
        tableDetails.setItems(viewModel.getSelectedAdherentPenalties());

        colDetailDoc.setCellValueFactory(cd -> {
            Emprunt e = cd.getValue().getEmprunt();
            String titre = (e != null && e.getDocument() != null)
                    ? e.getDocument().getTitre()
                    : "Document inconnu";
            return new ReadOnlyStringWrapper(safe(titre));
        });

        colDetailDate.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(
                        cd.getValue().getDateGeneration() != null
                                ? cd.getValue().getDateGeneration().format(dateFormatter)
                                : "—"
                )
        );

        colDetailMontant.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(
                        cd.getValue().getMontant() != null
                                ? cd.getValue().getMontant().toString()
                                : "—"
                )
        );

        // Colonne action : bouton "Régler"
        colDetailAction.setCellFactory(param -> new ActionButtonCell());
    }

    // Cellule interne pour la colonne Action
    private class ActionButtonCell extends TableCell<Penalite, Void> {
        private final Button btnRegler = new Button("Régler");
        private final HBox pane = new HBox(btnRegler);

        ActionButtonCell() {
            pane.setAlignment(Pos.CENTER);
            btnRegler.getStyleClass().addAll("btn", "btn-primary", "btn-sm");
            btnRegler.disableProperty().bind(viewModel.loadingProperty());

            btnRegler.setOnAction(e -> {
                Penalite penalite = getTableView().getItems().get(getIndex());
                if (penalite != null) {
                    confirmAndSettle(penalite);
                }
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : pane);
        }
    }

    // =====================================================================
    // =========================== Bindings UI ==============================
    // =====================================================================

    private void bindUIState() {
        // Bouton rafraîchir
        btnRefresh.setOnAction(e -> viewModel.refresh());
        btnRefresh.disableProperty().bind(viewModel.loadingProperty());

        // Progress
        progress.visibleProperty().bind(viewModel.loadingProperty());
        progress.managedProperty().bind(progress.visibleProperty());

        // Messages
        errorLabel.textProperty().bind(viewModel.errorProperty());
        successLabel.textProperty().bind(viewModel.successProperty());

        errorLabel.visibleProperty().bind(
                viewModel.errorProperty().isNotEmpty()
                        .and(viewModel.successProperty().isEmpty())
        );
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        successLabel.visibleProperty().bind(
                viewModel.successProperty().isNotEmpty()
                        .and(viewModel.errorProperty().isEmpty())
        );
        successLabel.managedProperty().bind(successLabel.visibleProperty());

        // Bouton "Tout régler"
        btnReglerTout.disableProperty().bind(
                viewModel.selectedSummaryProperty().isNull()
                        .or(viewModel.loadingProperty())
        );
        btnReglerTout.setOnAction(e -> {
            AdherentPenaliteSummary summary = viewModel.selectedSummaryProperty().get();
            if (summary != null) {
                confirmAndSettleAll(summary);
            }
        });
    }

    // =====================================================================
    // ======================= Dialogues de confirmation ====================
    // =====================================================================

    /** Confirmation pour régler UNE pénalité. */
    private void confirmAndSettle(Penalite penalite) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Régler la pénalité");
        alert.setHeaderText("Confirmer le paiement de " + penalite.getMontant() + " ?");
        alert.setContentText(
                "Adhérent : " + (penalite.getEmprunt() != null && penalite.getEmprunt().getEmprunteur() != null
                        ? penalite.getEmprunt().getEmprunteur().getNomComplet()
                        : "Inconnu")
                        + "\nDocument : " + (penalite.getEmprunt() != null && penalite.getEmprunt().getDocument() != null
                        ? penalite.getEmprunt().getDocument().getTitre()
                        : "Inconnu")
        );

        addTheme(alert.getDialogPane());

        ButtonType ok     = new ButtonType("Régler", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(ok, cancel);

        styleAsPrimary(alert.getDialogPane(), ok);
        styleAsSecondary(alert.getDialogPane(), cancel);

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ok) {
            viewModel.reglerPenalite(penalite);
        }
    }

    /** Confirmation pour tout régler pour un adhérent. */
    private void confirmAndSettleAll(AdherentPenaliteSummary summary) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Régler toutes les pénalités");
        alert.setHeaderText("Confirmer le paiement total de " + summary.total() + " ?");
        alert.setContentText(
                "Adhérent : " + summary.adherent().getNomComplet()
                        + "\nNombre de pénalités : " + summary.count()
        );

        addTheme(alert.getDialogPane());

        ButtonType ok     = new ButtonType("Tout régler", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(ok, cancel);

        styleAsPrimary(alert.getDialogPane(), ok);
        styleAsSecondary(alert.getDialogPane(), cancel);

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ok) {
            viewModel.reglerToutesPenalitesSelectionnees();
        }
    }

    // =====================================================================
    // ============================== Helpers ===============================
    // =====================================================================

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s.trim();
    }

    private void addTheme(DialogPane pane) {
        try {
            URL css = getClass().getResource("/application/ui/css/style.css");
            if (css != null) {
                pane.getStylesheets().add(css.toExternalForm());
                pane.getStyleClass().add("entity-dialog");
            }
        } catch (Exception ignored) {
        }
    }

    private void styleAsPrimary(DialogPane pane, ButtonType type) {
        Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn", "btn-primary");
        }
    }

    private void styleAsSecondary(DialogPane pane, ButtonType type) {
        Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn", "btn-secondary");
        }
    }
}