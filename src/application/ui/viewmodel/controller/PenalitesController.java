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
 * Contrôleur FXML pour la vue de gestion des pénalités côté admin.
 *
 * <p>Cette vue suit un motif Maître–Détail :</p>
 * <ul>
 *     <li>Table "Maître" : liste des adhérents ayant des pénalités,
 *         avec des agrégats (nombre total, somme totale, nb de demandes de paiement).</li>
 *     <li>Table "Détail" : liste des pénalités d’un adhérent sélectionné.</li>
 * </ul>
 *
 * <p>L'objectif de ce contrôleur est de :</p>
 * <ul>
 *     <li>afficher les résumés par adhérent à partir du {@link PenalitesViewModel},</li>
 *     <li>afficher les pénalités détaillées pour l’adhérent sélectionné,</li>
 *     <li>permettre de régler une pénalité ou toutes les pénalités d’un adhérent,
 *         après confirmation par l'utilisateur.</li>
 * </ul>
 *
 * <p>La logique métier (calculs, changements de statut, etc.) est entièrement
 * gérée dans le {@link PenalitesViewModel} et la couche domaine. Ce contrôleur
 * ne fait que :</p>
 * <ul>
 *     <li>configurer les tables et les colonnes,</li>
 *     <li>lier l'état de l'UI aux propriétés du ViewModel,</li>
 *     <li>ouvrir des boîtes de dialogue de confirmation pour l'utilisateur.</li>
 * </ul>
 */
public class PenalitesController {

    // ---------------------------------------------------------------------
    // Table "Maître" : liste des adhérents + agrégats de pénalités
    // ---------------------------------------------------------------------

    @FXML
    private TableView<AdherentPenaliteSummary> tableAdherents;

    @FXML
    private TableColumn<AdherentPenaliteSummary, String> colAdherentNom;

    @FXML
    private TableColumn<AdherentPenaliteSummary, String> colPenaliteCount;

    @FXML
    private TableColumn<AdherentPenaliteSummary, String> colPenaliteTotal;

    /**
     * Colonne affichant le nombre de pénalités pour lesquelles
     * une demande de paiement a été faite.
     */
    @FXML
    private TableColumn<AdherentPenaliteSummary, String> colPenaliteRequested;

    // ---------------------------------------------------------------------
    // Table "Détail" : liste des pénalités pour un adhérent sélectionné
    // ---------------------------------------------------------------------

    @FXML
    private TableView<Penalite> tableDetails;

    @FXML
    private TableColumn<Penalite, String> colDetailDoc;

    @FXML
    private TableColumn<Penalite, String> colDetailDate;

    @FXML
    private TableColumn<Penalite, String> colDetailMontant;

    /**
     * Colonne "Action" dans le détail : bouton "Régler" pour une pénalité.
     */
    @FXML
    private TableColumn<Penalite, Void> colDetailAction;

    // ---------------------------------------------------------------------
    // Panneau de détail + actions globales
    // ---------------------------------------------------------------------

    /**
     * Panneau contenant la table de détails + boutons associés.
     * Masqué tant qu'aucun adhérent n'est sélectionné.
     */
    @FXML
    private VBox detailsPane;

    /**
     * Bouton "Tout régler" pour l’adhérent sélectionné.
     */
    @FXML
    private Button btnReglerTout;

    // ---------------------------------------------------------------------
    // Éléments d'état globaux (chargement, messages, refresh)
    // ---------------------------------------------------------------------

    @FXML
    private ProgressIndicator progress;

    @FXML
    private Button btnRefresh;

    @FXML
    private Label errorLabel;

    @FXML
    private Label successLabel;

    // ---------------------------------------------------------------------
    // ViewModel & format de date
    // ---------------------------------------------------------------------

    /** ViewModel contenant les données et la logique métier. */
    private PenalitesViewModel viewModel;

    /** Format de date pour les colonnes (ex : 23/11/2025). */
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ---------------------------------------------------------------------
    // Constructeurs & injection du ViewModel
    // ---------------------------------------------------------------------

    /**
     * Constructeur par défaut requis par JavaFX.
     */
    public PenalitesController() {
        // Le ViewModel sera injecté ultérieurement par FxNavigator ou setViewModel(...)
    }

    /**
     * Constructeur utilisé par la ControllerFactory (FxNavigator).
     *
     * @param vm ViewModel pour la gestion des pénalités
     */
    public PenalitesController(PenalitesViewModel vm) {
        this.viewModel = vm;
    }

    /**
     * Méthode d’injection du ViewModel lorsque le contrôleur est
     * instancié par JavaFX (via FXML) sans constructeur personnalisé.
     *
     * @param vm ViewModel à associer à ce contrôleur
     */
    public void setViewModel(PenalitesViewModel vm) {
        this.viewModel = vm;
    }

    // ---------------------------------------------------------------------
    // Initialisation FXML
    // ---------------------------------------------------------------------

    /**
     * Méthode appelée automatiquement après le chargement du FXML.
     *
     * <p>Elle suppose que {@link #viewModel} a déjà été injecté. Elle :</p>
     * <ul>
     *     <li>configure la table maître (adhérents),</li>
     *     <li>configure la table détail (pénalités),</li>
     *     <li>met en place les bindings entre l’UI et le ViewModel.</li>
     * </ul>
     */
    @FXML
    private void initialize() {
        if (viewModel == null) {
            throw new IllegalStateException(
                    "PenalitesViewModel non injecté dans PenalitesController."
            );
        }

        setupMasterTable();
        setupDetailTable();
        bindUIState();
    }

    // =====================================================================
    // Table Maître (Adhérents)
    // =====================================================================

    /**
     * Configure la table des adhérents (partie "Maître" du Master–Detail).
     *
     * <p>Chaque ligne représente un adhérent avec :</p>
     * <ul>
     *     <li>son nom complet,</li>
     *     <li>le nombre total de pénalités,</li>
     *     <li>le montant total des pénalités,</li>
     *     <li>le nombre de pénalités ayant fait l’objet d’une demande de paiement.</li>
     * </ul>
     *
     * <p>Quand un adhérent est sélectionné, on met à jour
     * {@link PenalitesViewModel#selectedSummaryProperty()} et on affiche
     * le panneau de détails.</p>
     */
    private void setupMasterTable() {
        if (tableAdherents == null) {
            throw new IllegalStateException(
                    "TableView<AdherentPenaliteSummary> non injectée (fx:id=\"tableAdherents\")."
            );
        }

        // Source des données : liste observable du ViewModel
        tableAdherents.setItems(viewModel.getAdherentSummaries());

        // Colonne "Adhérent"
        colAdherentNom.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().adherent().getNomComplet())
        );

        // Colonne "Nombre de pénalités"
        colPenaliteCount.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(String.valueOf(cd.getValue().count()))
        );

        // Colonne "Total des pénalités"
        colPenaliteTotal.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().total().toString())
        );

        // Colonne "Demandes de paiement"
        if (colPenaliteRequested != null) {
            colPenaliteRequested.setCellValueFactory(cd ->
                    new ReadOnlyStringWrapper(String.valueOf(cd.getValue().requestedCount()))
            );
        }

        // Sélection d’un adhérent => mise à jour du ViewModel + affichage détails
        tableAdherents.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> {
                    viewModel.selectedSummaryProperty().set(newSel);

                    boolean showDetails = (newSel != null);
                    if (detailsPane != null) {
                        detailsPane.setVisible(showDetails);
                        detailsPane.setManaged(showDetails);
                    }
                });
    }

    // =====================================================================
    // Table Détail (Pénalités d’un adhérent)
    // =====================================================================

    /**
     * Configure la table des pénalités (partie "Détail" du Master–Detail).
     *
     * <p>Les lignes de cette table sont le détail des pénalités pour l’adhérent
     * sélectionné dans la table "Maître". La liste observable est fournie par
     * {@link PenalitesViewModel#getSelectedAdherentPenalties()}.</p>
     *
     * <p>Une colonne "Action" offre un bouton "Régler" pour chaque pénalité.</p>
     */
    private void setupDetailTable() {
        if (tableDetails == null) {
            throw new IllegalStateException(
                    "TableView<Penalite> non injectée (fx:id=\"tableDetails\")."
            );
        }

        tableDetails.setItems(viewModel.getSelectedAdherentPenalties());

        // Colonne "Document"
        colDetailDoc.setCellValueFactory(cd -> {
            Emprunt e = cd.getValue().getEmprunt();
            String titre = (e != null && e.getDocument() != null)
                    ? e.getDocument().getTitre()
                    : "Document inconnu";
            return new ReadOnlyStringWrapper(safe(titre));
        });

        // Colonne "Date" (date de génération)
        colDetailDate.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(
                        cd.getValue().getDateGeneration() != null
                                ? cd.getValue().getDateGeneration().format(dateFormatter)
                                : "—"
                )
        );

        // Colonne "Montant"
        colDetailMontant.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(
                        cd.getValue().getMontant() != null
                                ? cd.getValue().getMontant().toString()
                                : "—"
                )
        );

        // Colonne "Action" : bouton "Régler" par ligne
        colDetailAction.setCellFactory(param -> new ActionButtonCell());
    }

    /**
     * Cellule personnalisée pour la colonne "Action" de la table détails.
     *
     * <p>Affiche un bouton "Régler" centré dans la cellule. Quand l’utilisateur
     * clique dessus, une boîte de dialogue de confirmation s’affiche ; si l’admin
     * confirme, on délègue le règlement au ViewModel.</p>
     */
    private class ActionButtonCell extends TableCell<Penalite, Void> {

        /** Bouton "Régler" affiché dans la cellule. */
        private final Button btnRegler = new Button("Régler");

        /** Conteneur horizontal pour centrer le bouton. */
        private final HBox pane = new HBox(btnRegler);

        ActionButtonCell() {
            pane.setAlignment(Pos.CENTER);
            btnRegler.getStyleClass().addAll("btn", "btn-primary", "btn-sm");

            // Le bouton suit l'état de chargement global (désactivé si loading = true)
            btnRegler.disableProperty().bind(viewModel.loadingProperty());

            btnRegler.setOnAction(e -> {
                int index = getIndex();
                if (index < 0 || index >= getTableView().getItems().size()) {
                    return;
                }

                Penalite penalite = getTableView().getItems().get(index);
                if (penalite != null) {
                    confirmAndSettle(penalite);
                }
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                // Pas de donnée : cellule vide
                setGraphic(null);
                return;
            }

            int index = getIndex();
            if (index < 0 || index >= getTableView().getItems().size()) {
                setGraphic(null);
                return;
            }

            setGraphic(pane);
        }
    }

    // =====================================================================
    // Bindings UI ↔ ViewModel
    // =====================================================================

    /**
     * Met en place les liaisons entre l’état de l’UI et le ViewModel :
     * <ul>
     *     <li>bouton "Rafraîchir" et spinner de chargement,</li>
     *     <li>messages d’erreur et de succès,</li>
     *     <li>bouton "Tout régler" pour l’adhérent sélectionné.</li>
     * </ul>
     */
    private void bindUIState() {
        // --- Bouton "Rafraîchir" ---
        if (btnRefresh != null) {
            btnRefresh.setOnAction(e -> viewModel.refresh());
            btnRefresh.disableProperty().bind(viewModel.loadingProperty());
        }

        // --- Spinner de chargement ---
        if (progress != null) {
            progress.visibleProperty().bind(viewModel.loadingProperty());
            progress.managedProperty().bind(progress.visibleProperty());
        }

        // --- Messages d’erreur et de succès ---
        if (errorLabel != null) {
            errorLabel.textProperty().bind(viewModel.errorProperty());
            errorLabel.visibleProperty().bind(
                    viewModel.errorProperty().isNotEmpty()
                            .and(viewModel.successProperty().isEmpty())
            );
            errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        }

        if (successLabel != null) {
            successLabel.textProperty().bind(viewModel.successProperty());
            successLabel.visibleProperty().bind(
                    viewModel.successProperty().isNotEmpty()
                            .and(viewModel.errorProperty().isEmpty())
            );
            successLabel.managedProperty().bind(successLabel.visibleProperty());
        }

        // --- Bouton "Tout régler" pour l’adhérent sélectionné ---
        if (btnReglerTout != null) {
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
    }

    // =====================================================================
    // Dialogues de confirmation
    // =====================================================================

    /**
     * Affiche une boîte de dialogue de confirmation avant de régler UNE pénalité.
     *
     * <p>En cas de confirmation, délègue au ViewModel via
     * {@link PenalitesViewModel#reglerPenalite(Penalite)}.</p>
     */
    private void confirmAndSettle(Penalite penalite) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Régler la pénalité");
        alert.setHeaderText("Confirmer le paiement de " + penalite.getMontant() + " ?");

        String nomAdherent = "Inconnu";
        String titreDoc    = "Inconnu";

        if (penalite.getEmprunt() != null) {
            if (penalite.getEmprunt().getEmprunteur() != null) {
                nomAdherent = safe(penalite.getEmprunt().getEmprunteur().getNomComplet());
            }
            if (penalite.getEmprunt().getDocument() != null) {
                titreDoc = safe(penalite.getEmprunt().getDocument().getTitre());
            }
        }

        alert.setContentText(
                "Adhérent : " + nomAdherent +
                "\nDocument : " + titreDoc
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

    /**
     * Affiche une boîte de dialogue de confirmation avant de régler TOUTES
     * les pénalités d’un adhérent.
     *
     * <p>En cas de confirmation, délègue au ViewModel via
     * {@link PenalitesViewModel#reglerToutesPenalitesSelectionnees()}.</p>
     */
    private void confirmAndSettleAll(AdherentPenaliteSummary summary) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Régler toutes les pénalités");
        alert.setHeaderText("Confirmer le paiement total de " + summary.total() + " ?");
        alert.setContentText(
                "Adhérent : " + summary.adherent().getNomComplet() +
                "\nNombre de pénalités : " + summary.count()
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
    // Helpers (style, texte)
    // =====================================================================

    /**
     * Retourne une chaîne non vide pour l'affichage.
     *
     * @param s texte d’origine
     * @return texte trimé ou "—" si null ou vide
     */
    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s.trim();
    }

    /**
     * Applique la feuille de style globale aux boîtes de dialogue,
     * pour les intégrer visuellement au thème de l’application.
     */
    private void addTheme(DialogPane pane) {
        try {
            URL css = getClass().getResource("/application/ui/css/style.css");
            if (css != null) {
                pane.getStylesheets().add(css.toExternalForm());
                pane.getStyleClass().add("entity-dialog");
            }
        } catch (Exception ignored) {
            // En cas d'échec, on garde le style par défaut de JavaFX.
        }
    }

    /**
     * Applique un style "action principale" (ex : bouton bleu).
     */
    private void styleAsPrimary(DialogPane pane, ButtonType type) {
        Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn", "btn-primary");
        }
    }

    /**
     * Applique un style "secondaire" (ex : bouton gris) à un bouton de dialogue.
     */
    private void styleAsSecondary(DialogPane pane, ButtonType type) {
        Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn", "btn-secondary");
        }
    }
}