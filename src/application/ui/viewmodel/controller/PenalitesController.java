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
import javafx.scene.layout.VBox; // <-- Ajouté
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Controller pour la vue de gestion des pénalités (Maître-Détail).
 * CORRIGÉ : Ajout du "toggle" pour le panneau de détails.
 */
public class PenalitesController {

    // --- Table Maître ---
    @FXML private TableView<AdherentPenaliteSummary> tableAdherents;
    @FXML private TableColumn<AdherentPenaliteSummary, String> colAdherentNom;
    @FXML private TableColumn<AdherentPenaliteSummary, String> colPenaliteCount;
    @FXML private TableColumn<AdherentPenaliteSummary, String> colPenaliteTotal;

    // --- Table Détail ---
    @FXML private TableView<Penalite> tableDetails;
    @FXML private TableColumn<Penalite, String> colDetailDoc;
    @FXML private TableColumn<Penalite, String> colDetailDate;
    @FXML private TableColumn<Penalite, String> colDetailMontant;
    @FXML private TableColumn<Penalite, Void> colDetailAction;

    // --- NOUVEAU : Panneau de détails ---
    @FXML private VBox detailsPane; 
    @FXML private Button btnReglerTout;

    // --- Champs UI ---
    @FXML private ProgressIndicator progress;
    @FXML private Button btnRefresh;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private PenalitesViewModel viewModel;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // --- Constructeurs ---
    public PenalitesController() {}
    public PenalitesController(PenalitesViewModel vm) { this.viewModel = vm; }
    public void setViewModel(PenalitesViewModel vm) { this.viewModel = vm; }

    @FXML
    private void initialize() {
        if (viewModel == null) {
            throw new IllegalStateException("ViewModel non injecté.");
        }
        
        setupMasterTable();
        setupDetailTable();
        bindUIState();
    }

    /**
     * Configure la table Maître (Adhérents).
     */
    private void setupMasterTable() {
        tableAdherents.setItems(viewModel.getAdherentSummaries());

        colAdherentNom.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
            cd.getValue().adherent().getNomComplet()
        ));
        colPenaliteCount.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
            String.valueOf(cd.getValue().count())
        ));
        colPenaliteTotal.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
            cd.getValue().total().toString() // "X.XX EUR"
        ));
        
        // CORRIGÉ : Lie la sélection au ViewModel ET gère la visibilité
        tableAdherents.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            viewModel.selectedSummaryProperty().set(newSel);
            
            // --- Logique de "Toggle" ---
            boolean isDetailVisible = (newSel != null);
            if (detailsPane != null) {
                detailsPane.setVisible(isDetailVisible);
                detailsPane.setManaged(isDetailVisible);
            }
        });
    }

    /**
     * Configure la table Détail (Pénalités individuelles).
     */
    private void setupDetailTable() {
        tableDetails.setItems(viewModel.getSelectedAdherentPenalties());
        
        colDetailDoc.setCellValueFactory(cd -> {
            Emprunt e = cd.getValue().getEmprunt();
            String titre = (e != null && e.getDocument() != null) ? e.getDocument().getTitre() : "Document Inconnu";
            return new ReadOnlyStringWrapper(safe(titre));
        });
        
        colDetailDate.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
            cd.getValue().getDateGeneration().format(dateFormatter)
        ));
        
        colDetailMontant.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
            cd.getValue().getMontant().toString()
        ));
        
        // Colonne avec le bouton "Régler"
        colDetailAction.setCellFactory(param -> new ActionButtonCell());
    }

    private void bindUIState() {
        btnRefresh.setOnAction(e -> viewModel.refresh());
        
        progress.visibleProperty().bind(viewModel.loadingProperty());
        progress.managedProperty().bind(progress.visibleProperty());
        
        errorLabel.textProperty().bind(viewModel.errorProperty());
        errorLabel.visibleProperty().bind(viewModel.errorProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        
        successLabel.textProperty().bind(viewModel.successProperty());
        successLabel.visibleProperty().bind(viewModel.successProperty().isNotEmpty());
        successLabel.managedProperty().bind(successLabel.visibleProperty());

        errorLabel.visibleProperty().bind(viewModel.errorProperty().isNotEmpty().and(viewModel.successProperty().isEmpty()));
        successLabel.visibleProperty().bind(viewModel.successProperty().isNotEmpty().and(viewModel.errorProperty().isEmpty()));
    
        // Logique du bouton "Tout Régler"
        // Désactivé si aucun adhérent n'est sélectionné
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

    // --- Cellule interne pour le bouton "Régler" ---
    private class ActionButtonCell extends TableCell<Penalite, Void> {
        private final Button btnRegler = new Button("Régler");
        private final HBox pane = new HBox(btnRegler);

        ActionButtonCell() {
            btnRegler.getStyleClass().addAll("btn", "btn-primary", "btn-sm");
            btnRegler.disableProperty().bind(viewModel.loadingProperty());
            pane.setAlignment(Pos.CENTER);

            btnRegler.setOnAction(e -> {
                Penalite penalite = getTableView().getItems().get(getIndex());
                confirmAndSettle(penalite);
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : pane);
        }
    }
    
    /** Affiche la confirmation avant de régler UNE pénalité. */
    private void confirmAndSettle(Penalite penalite) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Régler Pénalité");
        alert.setHeaderText("Confirmer le paiement de " + penalite.getMontant() + " ?");
        alert.setContentText("Adhérent: " + penalite.getEmprunt().getEmprunteur().getNomComplet() +
                             "\nDocument: " + penalite.getEmprunt().getDocument().getTitre());
        addTheme(alert.getDialogPane());
        styleAsPrimary(alert.getDialogPane(), ButtonType.OK);
        styleAsSecondary(alert.getDialogPane(), ButtonType.CANCEL); // Style Annuler

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            viewModel.reglerPenalite(penalite);
        }
    }

    /** Affiche la confirmation avant de TOUT régler. */
    private void confirmAndSettleAll(AdherentPenaliteSummary summary) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Régler toutes les Pénalités");
        alert.setHeaderText("Confirmer le paiement total de " + summary.total() + " ?");
        alert.setContentText("Adhérent: " + summary.adherent().getNomComplet() +
                             "\nCela réglera " + summary.count() + " pénalité(s) active(s).");
        addTheme(alert.getDialogPane());
        styleAsPrimary(alert.getDialogPane(), ButtonType.OK);
        styleAsSecondary(alert.getDialogPane(), ButtonType.CANCEL); // Style Annuler

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            viewModel.reglerToutesPenalitesSelectionnees();
        }
    }
    
    // --- Helpers (copiés) ---
    private static String safe(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    
    private void addTheme(DialogPane pane) {
        try {
            URL css = getClass().getResource("/application/ui/css/style.css");
            if (css != null) {
                pane.getStylesheets().add(css.toExternalForm());
                pane.getStyleClass().add("entity-dialog");
            }
        } catch (Exception ignored) {}
    }

    private void styleAsPrimary(DialogPane pane, ButtonType type) {
        Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn", "btn-primary");
        }
    }
    
    // Ajout d'un helper pour le bouton Annuler
    private void styleAsSecondary(DialogPane pane, ButtonType type) {
        Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn", "btn-secondary");
        }
    }
}

