package application.ui.viewmodel.controller;

import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Penalite;
import application.ui.viewmodel.MesPenalitesViewModel;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Controller FXML pour la vue "Mes pénalités" (espace adhérent).
 *
 * - Liste des pénalités de l'adhérent.
 * - Colonne "Statut".
 * - Bouton "Demander paiement" si status == DUE / ACTIVE.
 */
public final class MesPenalitesController {

    @FXML private TableView<Penalite> table;
    @FXML private TableColumn<Penalite, String> colDoc;
    @FXML private TableColumn<Penalite, String> colDate;
    @FXML private TableColumn<Penalite, String> colMontant;
    @FXML private TableColumn<Penalite, String> colStatut;
    @FXML private TableColumn<Penalite, Void>   colAction;

    @FXML private ProgressIndicator progress;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button btnRefresh;

    private MesPenalitesViewModel viewModel;
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public MesPenalitesController() {}

    public MesPenalitesController(MesPenalitesViewModel vm) {
        this.viewModel = vm;
    }

    public void setViewModel(MesPenalitesViewModel vm) {
        this.viewModel = vm;
    }

    @FXML
    private void initialize() {
        if (viewModel == null) {
            throw new IllegalStateException(
                "MesPenalitesViewModel doit être injecté dans MesPenalitesController."
            );
        }

        setupTable();
        bindUI();
    }

    // ------------------------------------------------------------------------
    // Table & colonnes
    // ------------------------------------------------------------------------

    private void setupTable() {
        if (table == null) {
            throw new IllegalStateException("TableView<Penalite> non injectée (vérifie le FXML).");
        }

        table.setItems(viewModel.getPenalites());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        colDoc.setCellValueFactory(cd -> {
            Emprunt e = cd.getValue().getEmprunt();
            String titre = (e != null && e.getDocument() != null)
                    ? e.getDocument().getTitre()
                    : "Document inconnu";
            return new ReadOnlyStringWrapper(safe(titre));
        });

        colDate.setCellValueFactory(cd ->
            new ReadOnlyStringWrapper(
                cd.getValue().getDateGeneration() != null
                    ? cd.getValue().getDateGeneration().format(dateFormatter)
                    : "—"
            )
        );

        colMontant.setCellValueFactory(cd ->
            new ReadOnlyStringWrapper(
                cd.getValue().getMontant() != null
                    ? cd.getValue().getMontant().toString()
                    : "—"
            )
        );

        colStatut.setCellValueFactory(cd ->
            new ReadOnlyStringWrapper(
                cd.getValue().getStatus() != null
                    ? cd.getValue().getStatus().name()
                    : "—"
            )
        );

        colAction.setCellFactory(param -> new ActionButtonCell());
    }

    private void bindUI() {
        if (progress != null) {
            progress.visibleProperty().bind(viewModel.loadingProperty());
            progress.managedProperty().bind(progress.visibleProperty());
        }

        if (errorLabel != null) {
            errorLabel.textProperty().bind(viewModel.errorProperty());
            errorLabel.visibleProperty().bind(viewModel.errorProperty().isNotEmpty());
            errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        }

        if (successLabel != null) {
            successLabel.textProperty().bind(viewModel.successProperty());
            successLabel.visibleProperty().bind(viewModel.successProperty().isNotEmpty());
            successLabel.managedProperty().bind(successLabel.visibleProperty());
        }

        if (btnRefresh != null) {
            btnRefresh.disableProperty().bind(viewModel.loadingProperty());
            btnRefresh.setOnAction(e -> viewModel.refresh());
        }
    }

    // ------------------------------------------------------------------------
    // Cellule "Demander paiement"
    // ------------------------------------------------------------------------

    private class ActionButtonCell extends TableCell<Penalite, Void> {
        private final Button btn = new Button("Demander paiement");
        private final HBox box = new HBox(btn);

        ActionButtonCell() {
            box.setAlignment(Pos.CENTER);
            btn.getStyleClass().addAll("btn", "btn-primary", "btn-sm");
            btn.disableProperty().bind(viewModel.loadingProperty());

            btn.setOnAction(e -> {
                int index = getIndex();
                if (index < 0 || index >= getTableView().getItems().size()) return;

                Penalite p = getTableView().getItems().get(index);
                if (p != null) {
                    confirmAndAskPayment(p);
                }
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
                return;
            }

            int index = getIndex();
            if (index < 0 || index >= getTableView().getItems().size()) {
                setGraphic(null);
                return;
            }

            Penalite p = getTableView().getItems().get(index);
            boolean show = isDue(p);
            btn.setVisible(show);
            btn.setManaged(show);
            setGraphic(show ? box : null);
        }
    }

    // ------------------------------------------------------------------------
    // Boîte de dialogue stylée
    // ------------------------------------------------------------------------

    private void confirmAndAskPayment(Penalite p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Demande de paiement");
        alert.setHeaderText("Demander la mise en paiement de " + p.getMontant() + " ?");

        String titreDoc = "Inconnu";
        if (p.getEmprunt() != null && p.getEmprunt().getDocument() != null) {
            titreDoc = safe(p.getEmprunt().getDocument().getTitre());
        }
        alert.setContentText("Document : " + titreDoc);

        ButtonType btnConfirm = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel  = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnCancel, btnConfirm); // ordre : Annuler à gauche

        DialogPane pane = alert.getDialogPane();
        addTheme(pane);

        // Style des boutons
        styleAsPrimary(pane, btnConfirm);   // bouton bleu, par ex.
        styleAsDanger(pane, btnCancel);     // bouton "danger" pour annuler

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == btnConfirm) {
            viewModel.demanderPaiement(p.getId());
        }
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private boolean isDue(Penalite p) {
        if (p == null || p.getStatus() == null) return false;
        // Adapte si ton enum est différent
        String name = p.getStatus().name();
        return "ACTIVE".equalsIgnoreCase(name) || "DUE".equalsIgnoreCase(name);
    }

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
        } catch (Exception e) {
            System.err.println("Warning: Could not load CSS for MesPenalites dialog: " + e.getMessage());
        }
    }

    private void styleAsPrimary(DialogPane pane, ButtonType type) {
        Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn", "btn-primary");
            button.setDefaultButton(true);
        }
    }

    private void styleAsDanger(DialogPane pane, ButtonType type) {
        Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn", "btn-danger");
            button.setCancelButton(true);
        }
    }
}