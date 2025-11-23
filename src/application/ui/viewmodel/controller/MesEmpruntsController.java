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
 * Contrôleur FXML pour la vue "Mes emprunts" (espace adhérent).
 *
 * <p>Cette vue est affichée dans l'espace adhérent et permet :</p>
 * <ul>
 *     <li>d'afficher la liste des emprunts de l'adhérent connecté,</li>
 *     <li>de filtrer la liste (uniquement les emprunts en cours),</li>
 *     <li>de créer un nouvel emprunt sur un document disponible.</li>
 * </ul>
 *
 * <p>Ce contrôleur ne contient pas de logique métier "pure" :
 * il délègue au {@link MesEmpruntsViewModel} qui lui-même utilise
 * les services / repositories de la couche domaine.</p>
 *
 * <p>Rôle du contrôleur :</p>
 * <ul>
 *     <li>Configurer les colonnes de la table (affichage des dates, statut, etc.)</li>
 *     <li>Lier les widgets (CheckBox, ProgressIndicator, Labels) aux propriétés
 *         du ViewModel (loading, error, success...)</li>
 *     <li>Gérer les actions de l'utilisateur (clic sur "Rafraîchir", "Emprunter", etc.)</li>
 * </ul>
 */
public final class MesEmpruntsController {

    // ---------------------------------------------------------------------
    // Table "Mes emprunts"
    // ---------------------------------------------------------------------

    /** Table affichant les emprunts de l'adhérent. */
    @FXML
    private TableView<Emprunt> table;

    /** Colonne : titre du document emprunté. */
    @FXML
    private TableColumn<Emprunt, String> colDocument;

    /** Colonne : date d'emprunt. */
    @FXML
    private TableColumn<Emprunt, String> colDateEmprunt;

    /** Colonne : date de retour prévue. */
    @FXML
    private TableColumn<Emprunt, String> colDatePrevue;

    /** Colonne : date de retour effective (si le livre est rendu). */
    @FXML
    private TableColumn<Emprunt, String> colDateRetour;

    /** Colonne : statut de l'emprunt (EN_COURS, RETARD, TERMINE, etc.). */
    @FXML
    private TableColumn<Emprunt, String> colStatut;

    // ---------------------------------------------------------------------
    // Widgets d'état / filtres
    // ---------------------------------------------------------------------

    /** Case à cocher : n'afficher que les emprunts encore "actifs" (non rendus). */
    @FXML
    private CheckBox chkActifsOnly;

    /** Indicateur de chargement (spinner) pendant les opérations asynchrones. */
    @FXML
    private ProgressIndicator progress;

    /** Label pour afficher un message d'erreur. */
    @FXML
    private Label errorLabel;

    /** Label pour afficher un message de succès. */
    @FXML
    private Label successLabel;

    /** Bouton pour rafraîchir la liste des emprunts. */
    @FXML
    private Button btnRefresh;

    // ---------------------------------------------------------------------
    // Zone d'emprunt (création d'un nouvel emprunt)
    // ---------------------------------------------------------------------

    /**
     * ComboBox listant les documents disponibles à l'emprunt
     * pour cet adhérent (déjà filtrés par le ViewModel).
     */
    @FXML
    private ComboBox<Document> cbDocumentDisponible;

    /** Date à partir de laquelle l'emprunt est créé (par défaut : aujourd'hui). */
    @FXML
    private DatePicker dpDateEmprunt;

    /** Bouton "Emprunter". */
    @FXML
    private Button btnBorrow;

    // ---------------------------------------------------------------------
    // ViewModel (injection par le FxNavigator)
    // ---------------------------------------------------------------------

    /** ViewModel contenant la logique de "mes emprunts". */
    private MesEmpruntsViewModel viewModel;

    /**
     * Constructeur sans argument requis par JavaFX.
     */
    public MesEmpruntsController() {
        // L'injection réelle du ViewModel se fait via le constructeur dédié
        // ou via setViewModel(...) lorsque le Navigator utilise une factory.
    }

    /**
     * Constructeur permettant d'injecter directement le ViewModel.
     * Utilisé par le FxNavigator via une ControllerFactory.
     *
     * @param vm ViewModel déjà initialisé
     */
    public MesEmpruntsController(MesEmpruntsViewModel vm) {
        this.viewModel = vm;
    }

    /**
     * Méthode d'injection alternative, si on préfère instancier
     * le contrôleur via FXML puis lui passer le ViewModel ensuite.
     *
     * @param vm ViewModel "Mes emprunts"
     */
    public void setViewModel(MesEmpruntsViewModel vm) {
        this.viewModel = vm;
    }

    // ---------------------------------------------------------------------
    // Initialisation FXML
    // ---------------------------------------------------------------------

    /**
     * Méthode appelée automatiquement par JavaFX après le chargement du FXML
     * et l'injection des champs annotés avec {@link FXML}.
     *
     * <p>Elle vérifie que le ViewModel est bien injecté, puis :</p>
     * <ul>
     *     <li>configure la table,</li>
     *     <li>met en place les bindings (spinner, erreurs, succès, etc.),</li>
     *     <li>configure la zone de création d'emprunt.</li>
     * </ul>
     */
    @FXML
    private void initialize() {
        if (viewModel == null) {
            // Si on voit cette erreur, cela veut dire que le Navigator (ou un autre code)
            // n'a pas injecté correctement le ViewModel.
            throw new IllegalStateException(
                    "MesEmpruntsViewModel doit être injecté dans MesEmpruntsController."
            );
        }

        setupTable();
        bindUI();
        setupBorrowSection();
    }

    // ---------------------------------------------------------------------
    // Configuration de la table des emprunts
    // ---------------------------------------------------------------------

    /**
     * Configure la TableView et ses colonnes :
     * <ul>
     *     <li>source des données : {@code viewModel.getEmprunts()}</li>
     *     <li>affichage du titre du document</li>
     *     <li>affichage des dates (emprunt, retour prévu, retour effectif)</li>
     *     <li>affichage du statut courant (calculé à partir de la date du jour)</li>
     * </ul>
     */
    private void setupTable() {
        if (table == null) {
            throw new IllegalStateException(
                    "TableView<Emprunt> non injectée (vérifier le FXML : fx:id=\"table\")."
            );
        }

        // On branche la liste observable du ViewModel sur la table
        table.setItems(viewModel.getEmprunts());

        // Politique de redimensionnement des colonnes :
        // la dernière colonne prend la largeur restante.
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // ---- Colonne "Document" ----
        colDocument.setCellValueFactory(cd -> {
            Emprunt emprunt = cd.getValue();
            String titre = (emprunt != null && emprunt.getDocument() != null)
                    ? emprunt.getDocument().getTitre()
                    : "—";
            return new ReadOnlyStringWrapper(safe(titre));
        });

        // ---- Colonnes de dates (format ISO simple, ex: 2025-11-23) ----
        colDateEmprunt.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(toIso(cd.getValue().getDateEmprunt()))
        );

        colDatePrevue.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(toIso(cd.getValue().getDateRetourPrevue()))
        );

        colDateRetour.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(toIso(cd.getValue().getDateRetourEffective()))
        );

        // ---- Colonne "Statut" (appel à la méthode statut(...) de Emprunt) ----
        colStatut.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().statut(LocalDate.now()).name())
        );
    }

    // ---------------------------------------------------------------------
    // Bindings généraux : filtres, état de chargement, messages, refresh
    // ---------------------------------------------------------------------

    /**
     * Lie les composants UI aux propriétés exposées par le ViewModel :
     * <ul>
     *     <li>chkActifsOnly &lt;=&gt; viewModel.actifsOnlyProperty()</li>
     *     <li>progress.visible &lt;= viewModel.loadingProperty()</li>
     *     <li>errorLabel / successLabel &lt;= viewModel.errorProperty() / successProperty()</li>
     *     <li>btnRefresh déclenche un rafraîchissement des données</li>
     * </ul>
     */
    private void bindUI() {
        // --- Filtre "Actifs seulement" ---
        if (chkActifsOnly != null) {
            // Lien bidirectionnel : la CheckBox reflète la valeur du ViewModel
            // et la modifie quand l'utilisateur clique.
            chkActifsOnly.selectedProperty()
                         .bindBidirectional(viewModel.actifsOnlyProperty());
        }

        // --- Indicateur de chargement ---
        if (progress != null) {
            progress.visibleProperty().bind(viewModel.loadingProperty());
            progress.managedProperty().bind(progress.visibleProperty());
        }

        // --- Message d'erreur ---
        if (errorLabel != null) {
            errorLabel.textProperty().bind(viewModel.errorProperty());
            // Visible si erreur non vide ET aucun message de succès
            errorLabel.visibleProperty().bind(
                    viewModel.errorProperty().isNotEmpty()
                            .and(viewModel.successProperty().isEmpty())
            );
            errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        }

        // --- Message de succès ---
        if (successLabel != null) {
            successLabel.textProperty().bind(viewModel.successProperty());
            // Visible si succès non vide ET aucune erreur
            successLabel.visibleProperty().bind(
                    viewModel.successProperty().isNotEmpty()
                            .and(viewModel.errorProperty().isEmpty())
            );
            successLabel.managedProperty().bind(successLabel.visibleProperty());
        }

        // --- Bouton "Rafraîchir" ---
        if (btnRefresh != null) {
            btnRefresh.disableProperty().bind(viewModel.loadingProperty());
            btnRefresh.setOnAction(e -> {
                // Rafraîchit les emprunts et la liste des documents dispo
                viewModel.refresh();
                viewModel.refreshAvailableDocuments();
            });
        }
    }

    // ---------------------------------------------------------------------
    // Zone d’emprunt adhérent (création d’un nouveau prêt)
    // ---------------------------------------------------------------------

    /**
     * Configure la section "Nouvel emprunt" :
     * <ul>
     *     <li>alimentation du ComboBox avec les documents disponibles,</li>
     *     <li>affichage "Titre (ID: X)" dans la liste,</li>
     *     <li>date par défaut = aujourd'hui,</li>
     *     <li>gestion du clic sur le bouton "Emprunter".</li>
     * </ul>
     *
     * <p>Si certains contrôles ne sont pas injectés (par exemple si la vue
     * est partiellement chargée dans SceneBuilder), la méthode retourne
     * simplement sans lever d'exception.</p>
     */
    private void setupBorrowSection() {
        // Si on ouvre la vue dans SceneBuilder ou si la section n'existe pas,
        // on évite de crasher.
        if (cbDocumentDisponible == null || dpDateEmprunt == null || btnBorrow == null) {
            return;
        }

        // --- Liste des documents disponibles ---
        // Cette liste est fournie par le ViewModel, déjà filtrée :
        // seulement les documents non empruntés et autorisés pour cet adhérent.
        cbDocumentDisponible.setItems(viewModel.getAvailableDocuments());

        // Transformer un Document en texte lisible dans la ComboBox
        cbDocumentDisponible.setConverter(new StringConverter<>() {
            @Override
            public String toString(Document doc) {
                if (doc == null) return "";
                String titre = doc.getTitre();
                Long id = doc.getId();
                return (id != null)
                        ? titre + " (ID: " + id + ")"
                        : titre;
            }

            @Override
            public Document fromString(String s) {
                // Non utilisé : l'utilisateur choisit dans la liste,
                // on ne convertit jamais un texte libre en Document.
                return null;
            }
        });

        // Date d’emprunt par défaut : aujourd'hui
        dpDateEmprunt.setValue(LocalDate.now());

        // Bouton "Emprunter" désactivé si :
        // - le ViewModel est en cours de chargement
        // - aucun document n'est sélectionné
        // - aucune date n'est choisie
        btnBorrow.disableProperty().bind(
                viewModel.loadingProperty()
                         .or(cbDocumentDisponible.valueProperty().isNull())
                         .or(dpDateEmprunt.valueProperty().isNull())
        );

        // Action du bouton "Emprunter"
        btnBorrow.setOnAction(e -> {
            Document doc = cbDocumentDisponible.getValue();
            LocalDate dateEmprunt = dpDateEmprunt.getValue();

            if (doc == null || dateEmprunt == null) {
                // Sécurité supplémentaire au cas où le binding aurait été cassé
                viewModel.reportError("Veuillez choisir un document et une date d'emprunt.");
                return;
            }

            // Toute la logique métier (plafond d'emprunts, pénalités, etc.)
            // est gérée par EmpruntService.borrow(...) via le ViewModel.
            // Si une règle n'est pas respectée, une erreur remonte dans
            // viewModel.errorProperty() et s'affiche automatiquement.
            viewModel.borrowForCurrentAdherent(doc, dateEmprunt);
        });
    }

    // ---------------------------------------------------------------------
    // Helpers locaux pour l'affichage
    // ---------------------------------------------------------------------

    /**
     * Convertit une date éventuelle en chaîne lisible.
     *
     * @param d date (peut être null)
     * @return "—" si null, sinon {@code d.toString()} (format ISO-8601)
     */
    private static String toIso(LocalDate d) {
        return (d == null) ? "—" : d.toString();
    }

    /**
     * Nettoie une chaîne pour l'affichage : trim et remplacement des chaînes
     * vides / null par "—".
     *
     * @param s chaîne d'entrée
     * @return chaîne nettoyée pour affichage
     */
    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s.trim();
    }
}