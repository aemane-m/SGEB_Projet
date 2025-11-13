package application.ui.viewmodel.controller;

import application.domain.core.document.CD;
import application.domain.core.document.Document;
import application.domain.core.document.Livre;
import application.domain.core.document.Magazine;
import application.domain.ports.external.BookMeta;
import application.ui.viewmodel.CatalogueViewModel;
import application.ui.viewmodel.CatalogueViewModel.FilterParams;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Controller du catalogue :
 * - Grille 5x4 (20 cartes/page) avec placeholders
 * - Filtres et recherche
 * - Ajout via ISBN (OpenLibrary) ou manuel, édition, suppression
 * - Gestion des jaquettes (URL API ou fichier local via FileChooser + aperçu)
 */
public class CatalogueController {

    // ====== Constantes UI ======
    private static final int COLS = 5;
    private static final int ROWS = 4;
    private static final int PER_PAGE = COLS * ROWS;
    private static final double CARD_W = 190;
    private static final double CARD_H = 240;
    private static final double GAP = 15;
    public static final double MIN_W = 1350;
    public static final double MIN_H = 800;
    private static final double COVER_H = 240;
      

    // ====== FXML : Barre haute ======
    @FXML private TextField searchField;
    @FXML private Button btnSearch, btnRefresh, btnClear;
    @FXML private ProgressIndicator progress;
    @FXML private Label errorLabel;

    // Bouton d’ajout (optionnel dans le FXML)
    @FXML private Button btnAdd;

    // ====== Pagination ======
    @FXML private Pagination pagination;
    @FXML private ScrollPane rootScroll; // <-- CORRECTION AJOUTÉE


    // ====== Panneau de filtres ======
    @FXML private TitledPane filtersPane;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField tfGenre, tfEditeur;
    @FXML private Spinner<Integer> spAnneeMin, spAnneeMax;
    @FXML private CheckBox chkDisponible;
    @FXML private Button btnApplyFilters, btnResetFilters;

    // ====== VM ======
    private CatalogueViewModel viewModel;

    // ====== État local : chemin choisi pour la jaquette ======
    private String selectedCoverPath = null;

    // ====== Construction / Injection ======
    public CatalogueController() {}
    public CatalogueController(CatalogueViewModel vm) {
        this.viewModel = Objects.requireNonNull(vm, "ViewModel ne peut être null");
    }
    public void setViewModel(CatalogueViewModel vm) {
        this.viewModel = Objects.requireNonNull(vm, "ViewModel ne peut être null");
    }

    // ====== Initialisation FXML ======
    @FXML
    private void initialize() {
        if (viewModel == null) {
            throw new IllegalStateException("CatalogueViewModel non injecté. Appelez setViewModel() avant le chargement FXML.");
        }

        // Bindings et actions barre haute
        searchField.textProperty().bindBidirectional(viewModel.queryProperty());
        btnSearch.setOnAction(e -> viewModel.searchWithFilters());
        btnRefresh.setOnAction(e -> viewModel.refreshWithFilters());
        btnClear.setOnAction(e -> { searchField.clear(); viewModel.searchWithFilters(); });

        if (btnAdd != null) {
            btnAdd.setOnAction(e -> showIsbnLookupDialog());
        }

        progress.visibleProperty().bind(viewModel.loadingProperty());
        progress.managedProperty().bind(progress.visibleProperty());

        errorLabel.textProperty().bind(viewModel.errorProperty());
        errorLabel.visibleProperty().bind(viewModel.errorProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        btnRefresh.disableProperty().bind(viewModel.loadingProperty());
        btnClear.disableProperty().bind(
                viewModel.loadingProperty().or(viewModel.queryProperty().isEmpty())
        );

        // Filtres
        if (cbType != null) {
            cbType.getItems().setAll("Tous", "Livre", "Magazine", "CD");
            cbType.getSelectionModel().selectFirst();
        }
        if (spAnneeMin != null) {
            spAnneeMin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1500, 2100, 1990));
            spAnneeMin.setEditable(true);
        }
        if (spAnneeMax != null) {
            spAnneeMax.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1500, 2100, 2025));
            spAnneeMax.setEditable(true);
        }
        if (btnApplyFilters != null) {
            btnApplyFilters.setOnAction(e -> {
                applyFiltersAndSearch();
                if (filtersPane != null) filtersPane.setExpanded(false);
            });
        }
        if (btnResetFilters != null) {
            btnResetFilters.setOnAction(e -> {
                if (cbType != null) cbType.getSelectionModel().selectFirst();
                if (tfGenre != null) tfGenre.clear();
                if (tfEditeur != null) tfEditeur.clear();
                if (spAnneeMin != null && spAnneeMin.getValueFactory() != null) spAnneeMin.getValueFactory().setValue(1990);
                if (spAnneeMax != null && spAnneeMax.getValueFactory() != null) spAnneeMax.getValueFactory().setValue(2025);
                if (chkDisponible != null) chkDisponible.setSelected(false);
                applyFiltersAndSearch();
            });
        }

        // Pagination
        if (pagination != null) {
            pagination.setPageFactory(this::buildPage);
        }
        viewModel.getDocuments().addListener((javafx.collections.ListChangeListener<Document>) c -> rebuildPaginationPreserveIndex());

        // Bornes de la fenêtre
        Platform.runLater(() -> {
            try {
                // CORRIGÉ : utilise rootScroll (le parent de la pagination) pour trouver la scène
                var scene = rootScroll != null ? rootScroll.getScene() : (pagination != null ? pagination.getScene() : null);
                if (scene != null) {
                    var win = scene.getWindow();
                    if (win instanceof javafx.stage.Stage st) {
                        st.setMinWidth(MIN_W);
                        st.setMinHeight(MIN_H);
                    }
                }
            } catch (Exception ignored) {}
        });

        // Chargement initial
        viewModel.refreshWithFilters();
    }

    // ====== Filtres ======
    private void applyFiltersAndSearch() {
        FilterParams fp = viewModel.getFilters();
        if (fp == null) fp = new FilterParams();

        if (cbType != null) {
            String t = cbType.getValue();
            fp.setType(
                    "Livre".equals(t) ? FilterParams.Type.LIVRE :
                    "Magazine".equals(t) ? FilterParams.Type.MAGAZINE :
                    "CD".equals(t) ? FilterParams.Type.CD :
                    FilterParams.Type.TOUS
            );
        }
        if (tfGenre != null) fp.setGenre(nullIfBlank(tfGenre.getText()));
        if (tfEditeur != null) fp.setEditeur(nullIfBlank(tfEditeur.getText()));
        if (spAnneeMin != null && spAnneeMin.getValueFactory() != null) fp.setAnneeMin(spAnneeMin.getValue());
        if (spAnneeMax != null && spAnneeMax.getValueFactory() != null) fp.setAnneeMax(spAnneeMax.getValue());
        if (chkDisponible != null) fp.setDisponibleUniquement(chkDisponible.isSelected());

        viewModel.setFilters(fp);
        viewModel.searchWithFilters();
    }

    private static String nullIfBlank(String s) { return (s == null || s.isBlank()) ? null : s; }
    private static String safe(String s) { return (s == null || s.isBlank()) ? "—" : s; }

    // ====== Pagination ======
    private void rebuildPaginationPreserveIndex() {
        if (pagination == null) return;

        int oldIndex = pagination.getCurrentPageIndex();
        ObservableList<Document> docs = viewModel.getDocuments();
        int total = docs.size();
        int pages = Math.max(1, (int) Math.ceil(total / (double) PER_PAGE));

        pagination.setPageCount(pages);
        int newIndex = Math.min(Math.max(oldIndex, 0), pages - 1);

        if (newIndex != oldIndex) {
            pagination.setCurrentPageIndex(newIndex);
        } else {
            // Force la reconstruction de la page courante
            pagination.setPageFactory(this::buildPage);
        }
    }

    private Node buildPage(int pageIndex) {
        List<Document> docs = viewModel.getDocuments();
        int from = pageIndex * PER_PAGE;
        int to = Math.min(from + PER_PAGE, docs.size());
        List<Document> slice = (from < to) ? docs.subList(from, to) : List.of();

        GridPane grid = new GridPane();
        grid.setHgap(GAP);
        grid.setVgap(GAP);
        grid.setPadding(new Insets(10));
        grid.setAlignment(Pos.CENTER);

        grid.getColumnConstraints().clear();
        for (int c = 0; c < COLS; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHalignment(HPos.CENTER);
            grid.getColumnConstraints().add(cc);
        }

        int idx = 0;
        for (Document d : slice) {
            int r = idx / COLS, c = idx % COLS;
            grid.add(createCard(d), c, r);
            idx++;
        }
        while (idx < PER_PAGE) {
            int r = idx / COLS, c = idx % COLS;
            grid.add(createPlaceholder(), c, r);
            idx++;
        }
        return grid;
    }

    // ====== Cartes ======
    private Region createCard(Document doc) {
        VBox card = new VBox(6); // Espacement vertical interne
        // Garde les classes de base pour la forme et l'ombre
        card.getStyleClass().addAll("product-card", "product-card--compact");
        card.setPrefSize(CARD_W, CARD_H);
        card.setMaxSize(CARD_W, CARD_H);
        card.setPickOnBounds(true);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setAlignment(Pos.TOP_CENTER); // Centre le contenu horizontalement

        // --- Zone Image ---
        StackPane coverPane = new StackPane();
        // **CORRIGÉ**: Supprime la classe .product-cover par défaut
        // coverPane.getStyleClass().add("product-cover");
        coverPane.setPrefHeight(COVER_H); // Hauteur définie plus haut
        coverPane.setMinHeight(COVER_H);
        coverPane.setMaxHeight(COVER_H);
        coverPane.setAlignment(Pos.CENTER);
        // **CORRIGÉ**: Ajoute un fond gris clair subtil (visible si image absente/petite)
        coverPane.setStyle("-fx-background-color: #eeeeee;");

        String coverRef = doc.getCoverRef();
        if (coverRef != null && !coverRef.isBlank()) {
            try {
                File imageFile = new File(coverRef);
                if (imageFile.exists() && imageFile.isFile()) {
                    String imageUrl = imageFile.toURI().toString();
                    Image img = new Image(imageUrl, true); // Chargement arrière-plan

                    img.errorProperty().addListener((obs, oldError, newError) -> {
                        if (newError) {
                            System.err.println("Erreur chargement image carte : " + imageUrl);
                            if (img.getException() != null) img.getException().printStackTrace();
                        }
                    });

                    if (!img.isError()) {
                        ImageView imgView = new ImageView(img);
                        // **CORRIGÉ**: Ajuste à la LARGEUR de la carte
                        imgView.setFitWidth(CARD_W);
                        // **CORRIGÉ**: Ajuste aussi à la HAUTEUR de la zone cover
                        imgView.setFitHeight(COVER_H);
                        // **CORRIGÉ**: Garde les proportions (letterbox/pillarbox possible)
                        imgView.setPreserveRatio(false);
                        imgView.setSmooth(true);
                        coverPane.getChildren().add(imgView);
                    } else {
                        System.err.println("Erreur immédiate chargement image carte : " + imageUrl);
                        if (img.getException() != null) img.getException().printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur création URL/Image pour carte : " + coverRef + " - " + e.getMessage());
            }
        }

        // --- Zone Texte ---
        VBox textContent = new VBox(4); // Espacement plus petit pour le texte
        textContent.setPadding(new Insets(8, 8, 8, 8)); // Padding autour du texte
        textContent.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(Objects.toString(doc.getTitre(),"Sans titre"));
        title.getStyleClass().add("product-title");
        title.setWrapText(true);
        title.setMaxWidth(CARD_W - 16); // Empêche le texte de déborder

        String metaStr = String.format("%s • %s • %s",
                safe(doc.getGenre()), safe(doc.getEditeur()),
                doc.getAnneePublication()!=null ? doc.getAnneePublication() : "—");
        Label meta = new Label(metaStr);
        meta.getStyleClass().add("product-meta");
        meta.setWrapText(true);
        meta.setMaxWidth(CARD_W - 16);

        textContent.getChildren().addAll(title, meta);

        // Ajoute les zones image et texte à la carte principale
        card.getChildren().addAll(coverPane, textContent);

        // Actions au clic (inchangé)
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) showDetails(doc);
            else if (e.getButton() == MouseButton.SECONDARY) showAddOrEditDialog(doc, null);
        });

        return card;
    }


    private Region createPlaceholder() {
        VBox ph = new VBox(6);
        ph.getStyleClass().addAll("product-card", "product-card--compact", "product-card--placeholder");
        ph.setPrefSize(CARD_W, CARD_H);
        ph.setMaxSize(CARD_W, CARD_H);
        ph.setPickOnBounds(true);
        ph.setCursor(javafx.scene.Cursor.HAND);

        StackPane cover = new StackPane();
        cover.getStyleClass().addAll("product-cover", "skeleton-block");
        cover.setPrefHeight(130);

        Label l1 = new Label(); l1.getStyleClass().add("skeleton-line");
        Label l2 = new Label(); l2.getStyleClass().add("skeleton-line");
        Label l3 = new Label(); l3.getStyleClass().addAll("skeleton-line", "short");

        ph.getChildren().addAll(cover, l1, l2, l3);
        ph.setOnMouseClicked(e -> showIsbnLookupDialog());
        return ph;
    }

    // ====== Détails / Ajout / Édition / Suppression ======
    private void showDetails(Document doc) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Détails du document");
        addTheme(d.getDialogPane());

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(8); gp.setPadding(new Insets(16));
        gp.addRow(0, new Label("Titre"),   new Label(safe(doc.getTitre())));
        gp.addRow(1, new Label("Type"),    new Label(safe(doc.getDtype())));
        gp.addRow(2, new Label("Genre"),   new Label(safe(doc.getGenre())));
        gp.addRow(3, new Label("Éditeur"), new Label(safe(doc.getEditeur())));
        gp.addRow(4, new Label("Année"),   new Label(doc.getAnneePublication()==null?"—":doc.getAnneePublication().toString()));

        d.getDialogPane().setContent(gp);

        ButtonType BTN_EDIT   = new ButtonType("Modifier",  ButtonBar.ButtonData.OK_DONE);
        ButtonType BTN_DELETE = new ButtonType("Supprimer", ButtonBar.ButtonData.LEFT);
        d.getDialogPane().getButtonTypes().addAll(BTN_EDIT, BTN_DELETE, ButtonType.CLOSE);

        styleAsPrimary(d.getDialogPane(), BTN_EDIT);
        styleAsDanger(d.getDialogPane(), BTN_DELETE);

        Optional<ButtonType> res = d.showAndWait();
        if (res.isPresent()) {
            if (res.get() == BTN_EDIT) {
                showAddOrEditDialog(doc, null);
            } else if (res.get() == BTN_DELETE) {
                confirmAndDelete(doc);
            }
        }
    }

    private void confirmAndDelete(Document doc) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmer la suppression");
        a.setHeaderText("Supprimer « " + safe(doc.getTitre()) + " » ?");
        a.setContentText("Cette action est irréversible. Le document doit ne pas être emprunté.");
        addTheme(a.getDialogPane());

        Button btnOk = (Button) a.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.getStyleClass().addAll("btn", "btn-danger");

        Button btnCancel = (Button) a.getDialogPane().lookupButton(ButtonType.CANCEL);
        btnCancel.getStyleClass().addAll("btn", "btn-secondary");

        Optional<ButtonType> res = a.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            viewModel.delete(doc.getId()); // VM gère la règle métier côté service
        }
    }

    // ====== Ajout via ISBN (OpenLibrary) ======
    private void showIsbnLookupDialog() {
        Dialog<String> dlg = new Dialog<>();
        dlg.setTitle("Ajouter un document");
        dlg.setHeaderText("Recherche par ISBN");
        addTheme(dlg.getDialogPane());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField tfIsbn = new TextField();
        tfIsbn.setPromptText("Ex: 9782070643028");
        grid.add(new Label("ISBN:"), 0, 0);
        grid.add(tfIsbn, 1, 0);

        ProgressIndicator apiProgress = new ProgressIndicator();
        apiProgress.setVisible(false);
        grid.add(apiProgress, 2, 0);

        dlg.getDialogPane().setContent(grid);

        ButtonType BTN_SEARCH = new ButtonType("Rechercher", ButtonBar.ButtonData.OK_DONE);
        ButtonType BTN_MANUAL = new ButtonType("Ajout manuel", ButtonBar.ButtonData.LEFT);
        dlg.getDialogPane().getButtonTypes().addAll(BTN_SEARCH, BTN_MANUAL, ButtonType.CANCEL);

        styleAsPrimary(dlg.getDialogPane(), BTN_SEARCH);
        styleAsSecondary(dlg.getDialogPane(), BTN_MANUAL);

        Node btnSearch = dlg.getDialogPane().lookupButton(BTN_SEARCH);
        btnSearch.setDisable(true);
        tfIsbn.textProperty().addListener((o, old, aNew) -> btnSearch.setDisable(aNew.trim().isEmpty()));

        dlg.setResultConverter(btnType -> {
            if (btnType == BTN_SEARCH) {
                return tfIsbn.getText().trim();
            } else if (btnType == BTN_MANUAL) {
                Platform.runLater(() -> showAddOrEditDialog(null, null));
            }
            return null;
        });

        Optional<String> result = dlg.showAndWait();

        if (result.isPresent() && !result.get().isBlank()) {
            String isbn = result.get();
            apiProgress.setVisible(true);
            btnSearch.setDisable(true);

            viewModel.fetchBookInfoByIsbn(isbn, (success, meta, error) -> {
                apiProgress.setVisible(false);
                btnSearch.setDisable(false);

                if (success) {
                    showAddOrEditDialog(null, meta);
                } else {
                    showErrorDialog("Erreur API", "Document non trouvé", error);
                }
            });
        }
    }

    /**
     * Formulaire d'ajout/édition.
     * @param existing Document existant (édition) ou null (ajout)
     * @param meta     Métadonnées retournées par OpenLibrary (ajout auto) ou null
     */
    private void showAddOrEditDialog(Document existing, BookMeta meta) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(existing == null ? "Ajouter un document" : "Modifier le document");
        addTheme(dlg.getDialogPane());

        TitledPane pane = new TitledPane();
        pane.setText(existing == null ? "Nouveau document" : "Éditer le document");
        pane.setExpanded(true);
        pane.getStyleClass().addAll("titled-pane", "card-like", "collapsible");
        pane.setMinWidth(550);

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10); form.setPadding(new Insets(12));
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(30);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(70);
        form.getColumnConstraints().addAll(c1, c2);

        // Reset du chemin choisi
        selectedCoverPath = null;

        // Champs communs
        TextField tfTitre   = new TextField(existing != null ? existing.getTitre() : (meta != null ? meta.titre() : ""));
        TextField tfEditeur = new TextField(existing != null ? existing.getEditeur() : (meta != null ? meta.editeur() : ""));
        TextField tfGenre   = new TextField(existing != null ? existing.getGenre() : (meta != null ? meta.genre() : ""));
        Spinner<Integer> spAnnee = new Spinner<>(1500, 2100,
                existing != null && existing.getAnneePublication() != null
                        ? existing.getAnneePublication()
                        : (meta != null && meta.anneePublication() != null ? meta.anneePublication() : 2024),
                1);
        spAnnee.setEditable(true);

        ComboBox<String> cbTypeDoc = new ComboBox<>();
        cbTypeDoc.getItems().addAll("Livre", "Magazine", "CD");
        String defaultType = (meta != null) ? "Livre" : (existing != null && existing.getDtype() != null ? existing.getDtype() : "Livre");
        cbTypeDoc.getSelectionModel().select(defaultType);
        if (meta != null) cbTypeDoc.setDisable(true);

        // Champs spécifiques
        TextField tfAuteur   = new TextField();
        TextField tfIsbn     = new TextField();
        Spinner<Integer> spPages    = new Spinner<>(1, 10000, 200, 1); spPages.setEditable(true);
        Spinner<Integer> spNumero   = new Spinner<>(1, 10000, 1, 1);  spNumero.setEditable(true);
        TextField tfParution = new TextField();
        Spinner<Integer> spDuree    = new Spinner<>(1, 2000, 60, 1);  spDuree.setEditable(true);
        TextField tfArtiste  = new TextField();
        TextField tfLabel    = new TextField();

        // Pré-remplissage édition
        if (existing != null) {
            if (existing instanceof Livre l) {
                tfAuteur.setText(l.getAuteur());
                tfIsbn.setText(l.getIsbn());
                if (l.getNbPages() != null) spPages.getValueFactory().setValue(l.getNbPages());
            } else if (existing instanceof Magazine m) {
                if (m.getNumero() != null) spNumero.getValueFactory().setValue(m.getNumero());
                tfParution.setText(m.getMoisParution());
            } else if (existing instanceof CD cd) {
                if (cd.getDureeMinutes() != null) spDuree.getValueFactory().setValue(cd.getDureeMinutes());
                tfArtiste.setText(cd.getArtistePrincipal());
                tfLabel.setText(cd.getLabel());
            }
        }

        // Pré-remplissage API
        if (meta != null) {
            tfAuteur.setText(meta.auteur());
            tfIsbn.setText(meta.isbn());
            if (meta.nbPages() != null && meta.nbPages() > 0) {
                spPages.getValueFactory().setValue(meta.nbPages());
            }
            tfIsbn.setDisable(true);
        }

        // Zone jaquette : aperçu + bouton Choisir
        ImageView coverPreview = new ImageView();
        coverPreview.setFitHeight(80);
        coverPreview.setPreserveRatio(false);
        coverPreview.setSmooth(true);
        Button btnChooseCover = new Button("Choisir une image...");
        HBox coverBox = new HBox(10, coverPreview, btnChooseCover);
        coverBox.setAlignment(Pos.CENTER_LEFT);

        // Charger jaquette existante (édition)
        if (existing != null && existing.getCoverRef() != null) {
            updateCoverPreview(coverPreview, existing.getCoverRef(), false);
        }

        btnChooseCover.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choisir une image de couverture");
            fileChooser.getExtensionFilters().addAll(
                    new ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"),
                    new ExtensionFilter("Tous Fichiers", "*.*")
            );
            File selectedFile = fileChooser.showOpenDialog(dlg.getDialogPane().getScene().getWindow());
            if (selectedFile != null) {
                selectedCoverPath = selectedFile.getAbsolutePath();
                updateCoverPreview(coverPreview, selectedCoverPath, true);
            }
        });

        // Sous-formulaire spécifique selon type
        VBox specificBox = new VBox(10);
        cbTypeDoc.setOnAction(e -> {
            String type = cbTypeDoc.getValue();
            specificBox.getChildren().setAll(
                    buildSpecificPaneFor(type, tfAuteur, tfIsbn, spPages, spNumero, tfParution, spDuree, tfArtiste, tfLabel)
            );
        });
        cbTypeDoc.getOnAction().handle(null); // initialisation

        // Layout général
        int r = 0;
        form.add(new Label("Type"),    0, r); form.add(cbTypeDoc, 1, r++);
        form.add(new Label("Titre"),   0, r); form.add(tfTitre,   1, r++);
        form.add(new Label("Éditeur"), 0, r); form.add(tfEditeur, 1, r++);
        form.add(new Label("Genre"),   0, r); form.add(tfGenre,   1, r++);
        form.add(new Label("Année"),   0, r); form.add(spAnnee,   1, r++);
        form.add(new Label("Couverture"), 0, r); form.add(coverBox, 1, r++);
        form.add(new Label("Détails"), 0, r); form.add(specificBox, 1, r++);

        pane.setContent(form);
        dlg.getDialogPane().setContent(pane);

        ButtonType BTN_SAVE = new ButtonType(existing == null ? "Enregistrer" : "Mettre à jour", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(BTN_SAVE, ButtonType.CANCEL);
        styleAsPrimary(dlg.getDialogPane(), BTN_SAVE);

        Button btnSave = (Button) dlg.getDialogPane().lookupButton(BTN_SAVE);
        btnSave.disableProperty().bind(tfTitre.textProperty().isEmpty());

        Optional<ButtonType> res = dlg.showAndWait();
        if (res.isPresent() && res.get() == BTN_SAVE) {
            String type = cbTypeDoc.getValue();
            Document docToSave = null;
            String coverUrlApi = (meta != null) ? meta.coverUrl() : null;

            // Construire l'entité selon le type
            switch (type) {
                case "Livre" -> {
                    Livre l = (existing instanceof Livre) ? (Livre) existing : new Livre();
                    l.setDtype("Livre");
                    l.setTitre(tfTitre.getText().trim());
                    l.setEditeur(nullIfBlank(tfEditeur.getText()));
                    l.setGenre(nullIfBlank(tfGenre.getText()));
                    l.setAnneePublication(spAnnee.getValue());
                    l.setAuteur(nullIfBlank(tfAuteur.getText()));
                    l.setIsbn(nullIfBlank(tfIsbn.getText()));
                    l.setNbPages(spPages.getValue());
                    docToSave = l;
                }
                case "Magazine" -> {
                    Magazine m = (existing instanceof Magazine) ? (Magazine) existing : new Magazine();
                    m.setDtype("Magazine");
                    m.setTitre(tfTitre.getText().trim());
                    m.setEditeur(nullIfBlank(tfEditeur.getText()));
                    m.setGenre(nullIfBlank(tfGenre.getText()));
                    m.setAnneePublication(spAnnee.getValue());
                    m.setNumero(spNumero.getValue());
                    m.setMoisParution(nullIfBlank(tfParution.getText()));
                    docToSave = m;
                }
                case "CD" -> {
                    CD cd = (existing instanceof CD) ? (CD) existing : new CD();
                    cd.setDtype("CD");
                    cd.setTitre(tfTitre.getText().trim());
                    cd.setEditeur(nullIfBlank(tfEditeur.getText()));
                    cd.setGenre(nullIfBlank(tfGenre.getText()));
                    cd.setAnneePublication(spAnnee.getValue());
                    cd.setDureeMinutes(spDuree.getValue());
                    cd.setArtistePrincipal(nullIfBlank(tfArtiste.getText()));
                    cd.setLabel(nullIfBlank(tfLabel.getText()));
                    docToSave = cd;
                }
            }

            if (docToSave != null) {
                if (existing == null) {
                    // Ajout
                    if (selectedCoverPath != null) {
                        viewModel.createWithLocalCover(docToSave, selectedCoverPath);
                    } else {
                        // Ajout manuel (pas d'image) ou ajout API (coverUrlApi potentiellement non null)
                        viewModel.create(docToSave, coverUrlApi);
                    }
                } else {
                    // Édition
                    docToSave.setId(existing.getId());
                    if (selectedCoverPath != null) {
                        viewModel.updateWithLocalCover(docToSave, selectedCoverPath);
                    } else {
                        // Ne pas écraser la jaquette existante par une URL API à ce stade
                        viewModel.update(docToSave, null);
                    }
                }
            }
        }
    }

    /** Met à jour l’aperçu de jaquette dans le formulaire. */
    private void updateCoverPreview(ImageView imageView, String path, boolean isLocalAbsolutePath) {
        if (path == null || path.isBlank()) {
            imageView.setImage(null); // Efface l'aperçu si pas de chemin
            return;
        }
        try {
            String imageUrl;
            File imageFile = new File(path); // Crée l'objet File dans tous les cas

            if (isLocalAbsolutePath) {
                // Chemin absolu d'un fichier local choisi
                if (imageFile.exists() && imageFile.isFile()) {
                    imageUrl = imageFile.toURI().toString();
                } else {
                    imageUrl = null;
                    System.err.println("Aperçu : Fichier local choisi non trouvé : " + path);
                }
            } else {
                // Chemin relatif stocké en BDD (ex: "covers/...")
                 if (imageFile.exists() && imageFile.isFile()) {
                    imageUrl = imageFile.toURI().toString();
                } else {
                    imageUrl = null; // Fichier non trouvé dans ./covers
                    System.err.println("Aperçu : Fichier image BDD non trouvé : " + path);
                }
            }

            if (imageUrl != null) {
                Image img = new Image(imageUrl, true); // Background loading
                imageView.setImage(img);
                // Optionnel : Gérer les erreurs de chargement pour l'aperçu
                img.errorProperty().addListener((obs, old, err) -> {
                    if (err) {
                        System.err.println("Erreur chargement aperçu: " + imageUrl);
                         imageView.setImage(null); // Efface l'aperçu en cas d'erreur
                    }
                });
                if (img.isError()) { // Vérifie aussi l'erreur immédiate
                     System.err.println("Erreur immédiate chargement aperçu: " + imageUrl);
                     imageView.setImage(null);
                }
            } else {
                imageView.setImage(null); // Efface si le fichier n'existe pas
            }
        } catch (Exception e) {
            imageView.setImage(null); // Efface en cas d'erreur
            System.err.println("Erreur mise à jour aperçu : " + path + " - " + e.getMessage());
        }
    }

    /** Construit la section des champs spécifiques selon le type. */
    private Node buildSpecificPaneFor(String type,
                                      TextField tfAuteur, TextField tfIsbn, Spinner<Integer> spPages,
                                      Spinner<Integer> spNumero, TextField tfParution,
                                      Spinner<Integer> spDuree, TextField tfArtiste, TextField tfLabel) {
        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(6);
        gp.getColumnConstraints().addAll(
                new ColumnConstraints(),
                new ColumnConstraints(Region.USE_PREF_SIZE, Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE, Priority.ALWAYS, HPos.LEFT, true)
        );

        int r = 0;
        switch (type != null ? type : "") {
            case "Livre" -> {
                gp.add(new Label("Auteur"), 0, r); gp.add(tfAuteur, 1, r++);
                gp.add(new Label("ISBN"),   0, r); gp.add(tfIsbn,   1, r++);
                gp.add(new Label("Nb pages"), 0, r); gp.add(spPages, 1, r++);
            }
            case "Magazine" -> {
                gp.add(new Label("Numéro"),  0, r); gp.add(spNumero,   1, r++);
                gp.add(new Label("Parution"),0, r); gp.add(tfParution, 1, r++);
            }
            case "CD" -> {
                gp.add(new Label("Durée (min)"), 0, r); gp.add(spDuree, 1, r++);
                gp.add(new Label("Artiste"),     0, r); gp.add(tfArtiste, 1, r++);
                gp.add(new Label("Label"),       0, r); gp.add(tfLabel,   1, r++);
            }
            default -> {
                // Rien
            }
        }
        return gp;
    }

    // ====== Helpers de thème (CSS) ======
    private void addTheme(DialogPane pane) {
        try {
            URL css = getClass().getResource("/application/ui/css/style.css");
            if (css != null) {
                pane.getStylesheets().add(css.toExternalForm());
                pane.getStyleClass().add("document-dialog");
            } else {
                System.err.println("CSS introuvable : /application/ui/css/style.css");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void styleAsPrimary(DialogPane pane, ButtonType type) {
        Node n = pane.lookupButton(type);
        if (n instanceof Button b) b.getStyleClass().addAll("btn", "btn-primary");
    }

    private void styleAsSecondary(DialogPane pane, ButtonType type) {
        Node n = pane.lookupButton(type);
        if (n instanceof Button b) b.getStyleClass().addAll("btn", "btn-secondary");
    }

    private void styleAsDanger(DialogPane pane, ButtonType type) {
        Node n = pane.lookupButton(type);
        if (n instanceof Button b) b.getStyleClass().addAll("btn", "btn-danger");
    }

    private void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        addTheme(alert.getDialogPane());
        alert.showAndWait();
    }
}
