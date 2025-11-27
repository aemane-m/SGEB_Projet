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
 * Contrôleur FXML pour la vue "Mes pénalités" (espace adhérent).
 *
 * <p>Cette vue est affichée dans l'espace personnel de l'adhérent et permet :</p>
 * <ul>
 *     <li>d'afficher la liste de ses pénalités,</li>
 *     <li>de voir la date, le montant et le statut de chaque pénalité,</li>
 *     <li>de déclencher une action "Demander paiement" quand c'est autorisé
 *         (par exemple si le statut est DUE ou ACTIVE).</li>
 * </ul>
 *
 * <p>La logique métier (calcul des pénalités, transition des statuts, etc.)
 * est gérée par la couche domaine et exposée via le {@link MesPenalitesViewModel}.
 * Le contrôleur se contente de :</p>
 * <ul>
 *     <li>configurer la TableView (colonnes, cellules spéciales),</li>
 *     <li>lier l'état de l'UI (spinner, messages) au ViewModel,</li>
 *     <li>ouvrir des boîtes de dialogue de confirmation pour l'utilisateur.</li>
 * </ul>
 */
public final class MesPenalitesController {

    // ---------------------------------------------------------------------
    // Table et colonnes
    // ---------------------------------------------------------------------

    /** Table affichant les pénalités de l'adhérent. */
    @FXML
    private TableView<Penalite> table;

    /** Colonne : titre du document concerné par la pénalité. */
    @FXML
    private TableColumn<Penalite, String> colDoc;

    /** Colonne : date de génération de la pénalité. */
    @FXML
    private TableColumn<Penalite, String> colDate;

    /** Colonne : montant de la pénalité. */
    @FXML
    private TableColumn<Penalite, String> colMontant;

    /** Colonne : statut de la pénalité (ACTIVE, DUE, PAID, etc.). */
    @FXML
    private TableColumn<Penalite, String> colStatut;

    /**
     * Colonne "Action" :
     * affiche un bouton "Demander paiement" pour certaines pénalités.
     */
    @FXML
    private TableColumn<Penalite, Void> colAction;

    // ---------------------------------------------------------------------
    // Widgets d'état / actions générales
    // ---------------------------------------------------------------------

    /** Indicateur de chargement global (spinner). */
    @FXML
    private ProgressIndicator progress;

    /** Message d'erreur affiché sous la table. */
    @FXML
    private Label errorLabel;

    /** Message de succès affiché sous la table. */
    @FXML
    private Label successLabel;

    /** Bouton "Rafraîchir" les données. */
    @FXML
    private Button btnRefresh;

    // ---------------------------------------------------------------------
    // ViewModel & formatage
    // ---------------------------------------------------------------------

    /** ViewModel qui fournit les données et l'état métier. */
    private MesPenalitesViewModel viewModel;

    /** Format de date pour l'affichage (ex: 23/11/2025). */
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ---------------------------------------------------------------------
    // Constructeurs & injection du ViewModel
    // ---------------------------------------------------------------------

    /**
     * Constructeur par défaut requis par JavaFX.
     */
    public MesPenalitesController() {
    }

    /**
     * Constructeur utilisé par une ControllerFactory (FxNavigator).
     *
     * @param vm ViewModel déjà initialisé
     */
    public MesPenalitesController(MesPenalitesViewModel vm) {
        this.viewModel = vm;
    }

    /**
     * Méthode d'injection alternative si l'on instancie le contrôleur
     * automatiquement via FXML puis que l'on passe le ViewModel ensuite.
     *
     * @param vm ViewModel "Mes pénalités"
     */
    public void setViewModel(MesPenalitesViewModel vm) {
        this.viewModel = vm;
    }

    // ---------------------------------------------------------------------
    // Initialisation FXML
    // ---------------------------------------------------------------------

    /**
     * Méthode appelée automatiquement par JavaFX après le chargement du FXML.
     *
     * <p>Elle vérifie que le ViewModel a bien été injecté, puis :</p>
     * <ul>
     *     <li>configure la table et ses colonnes,</li>
     *     <li>met en place les bindings entre l'UI et le ViewModel.</li>
     * </ul>
     */
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

    // ---------------------------------------------------------------------
    // Configuration de la table et des colonnes
    // ---------------------------------------------------------------------

    /**
     * Configure la TableView :
     * <ul>
     *     <li>liste observable fournie par {@link MesPenalitesViewModel#getPenalites()},</li>
     *     <li>colonnes pour le document, la date, le montant, le statut,</li>
     *     <li>colonne "Action" avec un bouton dynamique.</li>
     * </ul>
     */
    private void setupTable() {
        if (table == null) {
            throw new IllegalStateException(
                    "TableView<Penalite> non injectée (vérifier le FXML : fx:id=\"table\")."
            );
        }

        // Source des données : liste observable du ViewModel
        table.setItems(viewModel.getPenalites());

        // Politique de redimensionnement : dernière colonne prend la place restante
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // ---- Colonne "Document" ----
        colDoc.setCellValueFactory(cd -> {
            Emprunt emprunt = cd.getValue().getEmprunt();
            String titre = (emprunt != null && emprunt.getDocument() != null)
                    ? emprunt.getDocument().getTitre()
                    : "Document inconnu";
            return new ReadOnlyStringWrapper(safe(titre));
        });

        // ---- Colonne "Date" (date de génération) ----
        colDate.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(
                        cd.getValue().getDateGeneration() != null
                                ? cd.getValue().getDateGeneration().format(dateFormatter)
                                : "—"
                )
        );

        // ---- Colonne "Montant" ----
        colMontant.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(
                        cd.getValue().getMontant() != null
                                ? cd.getValue().getMontant().toString()
                                : "—"
                )
        );

        // ---- Colonne "Statut" ----
        colStatut.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(
                        cd.getValue().getStatus() != null
                                ? cd.getValue().getStatus().name()
                                : "—"
                )
        );

        // ---- Colonne "Action" avec bouton "Demander paiement" ----
        colAction.setCellFactory(param -> new ActionButtonCell());
    }

    // ---------------------------------------------------------------------
    // Bindings UI ↔ ViewModel
    // ---------------------------------------------------------------------

    /**
     * Lie l'état de l'UI aux propriétés du ViewModel :
     * <ul>
     *     <li>spinner visible pendant le chargement,</li>
     *     <li>affichage des messages d'erreur et de succès,</li>
     *     <li>bouton "Rafraîchir" qui recharge les pénalités.</li>
     * </ul>
     */
    private void bindUI() {
        // --- Spinner de chargement ---
        if (progress != null) {
            progress.visibleProperty().bind(viewModel.loadingProperty());
            progress.managedProperty().bind(progress.visibleProperty());
        }

        // --- Message d'erreur ---
        if (errorLabel != null) {
            errorLabel.textProperty().bind(viewModel.errorProperty());
            errorLabel.visibleProperty().bind(viewModel.errorProperty().isNotEmpty());
            errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        }

        // --- Message de succès ---
        if (successLabel != null) {
            successLabel.textProperty().bind(viewModel.successProperty());
            successLabel.visibleProperty().bind(viewModel.successProperty().isNotEmpty());
            successLabel.managedProperty().bind(successLabel.visibleProperty());
        }

        // --- Bouton "Rafraîchir" ---
        if (btnRefresh != null) {
            btnRefresh.disableProperty().bind(viewModel.loadingProperty());
            btnRefresh.setOnAction(e -> viewModel.refresh());
        }
    }

    // ---------------------------------------------------------------------
    // Cellule personnalisée : bouton "Demander paiement"
    // ---------------------------------------------------------------------

    /**
     * Cellule personnalisée pour la colonne "Action".
     *
     * <p>Affiche un bouton "Demander paiement" au centre de la cellule si :</p>
     * <ul>
     *     <li>la ligne n'est pas vide,</li>
     *     <li>la pénalité est dans un statut autorisant la demande de paiement
     *         (ACTIVE ou DUE, selon l'implémentation de {@link #isDue(Penalite)}).</li>
     * </ul>
     *
     * <p>Quand l'utilisateur clique sur le bouton, une boîte de dialogue de confirmation
     * s'ouvre. Si l'utilisateur confirme, le contrôleur demande au ViewModel de
     * déclencher la logique métier via {@link MesPenalitesViewModel#demanderPaiement(Long)}.</p>
     */
    private class ActionButtonCell extends TableCell<Penalite, Void> {

        /** Bouton affiché dans la cellule. */
        private final Button btn = new Button("Demander paiement");
        /** Conteneur horizontal pour centrer le bouton. */
        private final HBox box = new HBox(btn);

        ActionButtonCell() {
            // Alignement du bouton au centre de la cellule
            box.setAlignment(Pos.CENTER);

            // Classes CSS pour réutiliser ton thème
            btn.getStyleClass().addAll("btn", "btn-primary", "btn-sm");

            // Le bouton suit l'état "loading" du ViewModel
            btn.disableProperty().bind(viewModel.loadingProperty());

            // Action au clic sur le bouton
            btn.setOnAction(e -> {
                int index = getIndex();
                if (index < 0 || index >= getTableView().getItems().size()) {
                    return;
                }

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
                // Ligne vide (en dehors de la zone de données)
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

            // On affiche le bouton seulement si la pénalité est "éligible"
            btn.setVisible(show);
            btn.setManaged(show);
            setGraphic(show ? box : null);
        }
    }

    // ---------------------------------------------------------------------
    // Boîte de dialogue de confirmation
    // ---------------------------------------------------------------------

    /**
     * Ouvre une boîte de dialogue de confirmation avant de demander le paiement.
     *
     * <p>Si l'utilisateur confirme, appelle {@link MesPenalitesViewModel#demanderPaiement(Long)}
     * avec l'ID de la pénalité.</p>
     *
     * @param p pénalité pour laquelle on veut demander le paiement
     */
    private void confirmAndAskPayment(Penalite p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Demande de paiement");
        alert.setHeaderText("Demander la mise en paiement de " + p.getMontant() + " ?");

        // Affichage du titre de document associé à la pénalité (si disponible)
        String titreDoc = "Inconnu";
        if (p.getEmprunt() != null && p.getEmprunt().getDocument() != null) {
            titreDoc = safe(p.getEmprunt().getDocument().getTitre());
        }
        alert.setContentText("Document : " + titreDoc);

        // Boutons personnalisés
        ButtonType btnConfirm = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel  = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        // Ordre : Annuler à gauche, Confirmer à droite
        alert.getButtonTypes().setAll(btnCancel, btnConfirm);

        // Application du thème CSS projet
        DialogPane pane = alert.getDialogPane();
        addTheme(pane);
        styleAsPrimary(pane, btnConfirm);
        styleAsDanger(pane, btnCancel);

        // Affichage modal
        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == btnConfirm) {
            // L'utilisateur a confirmé -> on délègue au ViewModel
            viewModel.demanderPaiement(p.getId());
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /**
     * Indique si une pénalité est "éligible" à la demande de paiement.
     *
     * <p>Ici, on considère que seuls les statuts "ACTIVE" ou "DUE"
     * permettent de déclencher l'action.</p>
     *
     * @param p pénalité
     * @return true si un bouton "Demander paiement" doit être affiché
     */
    private boolean isDue(Penalite p) {
        if (p == null || p.getStatus() == null) return false;
        String name = p.getStatus().name();
        return "ACTIVE".equalsIgnoreCase(name) || "DUE".equalsIgnoreCase(name);
    }

    /**
     * Nettoie une chaîne pour l'affichage.
     *
     * @param s texte brut
     * @return texte trimé ou "—" si null / vide
     */
    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s.trim();
    }

    /**
     * Applique la feuille de style globale du projet aux boîtes de dialogue.
     *
     * <p>Permet d'avoir des Alert JavaFX qui ressemblent visuellement
     * au reste de l'application.</p>
     */
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

    /**
     * Applique un style "action principale".
     */
    private void styleAsPrimary(DialogPane pane, ButtonType type) {
        Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn", "btn-primary");
            button.setDefaultButton(true);
        }
    }

    /**
     * Applique un style "danger" ou "annulation".
     */
    private void styleAsDanger(DialogPane pane, ButtonType type) {
        Node node = pane.lookupButton(type);
        if (node instanceof Button button) {
            button.getStyleClass().addAll("btn", "btn-danger");
            button.setCancelButton(true);
        }
    }
}