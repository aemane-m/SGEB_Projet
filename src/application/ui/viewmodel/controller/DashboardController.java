package application.ui.viewmodel.controller;

import application.ui.viewmodel.DashboardViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contrôleur du Dashboard.
 * Version Corrigée pour prendre en compte le filtre de période.
 */
public final class DashboardController {

    // ---------------------------------------------------------------------
    // FXML : KPI & état global
    // ---------------------------------------------------------------------

    @FXML private Label lblDocs;
    @FXML private Label lblAdherents;
    @FXML private Label lblEmprunts;
    @FXML private Label lblPenalites;
    @FXML private Label lblRetards;
    @FXML private Label lblTauxRotation;
    @FXML private Label lblTauxOccupation;
    @FXML private Label lblLastUpdated;
    @FXML private Label lblTauxUtilisationFonds;
    @FXML private Label lblTauxReemprunt;
    @FXML private Label lblDureeMoyenneEmprunts;
    @FXML private Label lblTauxRetard;
    @FXML private Label lblTauxEngagementAdherents;
    @FXML private Label lblError;

    @FXML private ProgressIndicator progress;

    @FXML private Button btnRefresh;
    @FXML private Button btnExportPDF;

    @FXML private ToggleButton btn7Days;
    @FXML private ToggleButton btn30Days;
    @FXML private ToggleButton btn90Days;
    private ToggleGroup periodToggleGroup;

    // ---------------------------------------------------------------------
    // FXML : graphiques & tableaux
    // ---------------------------------------------------------------------

    @FXML private HBox recentStripContainer;
    @FXML private AreaChart<String, Number> chartLoans;
    @FXML private BarChart<String, Number>  chartGenresBar;
    @FXML private BarChart<String, Number>  chartPeakHours;

    @FXML private TableView<DashboardViewModel.ActiveBorrower> tableAdherentsActifs;
    @FXML private TableColumn<DashboardViewModel.ActiveBorrower, String> colAdherentNom;
    @FXML private TableColumn<DashboardViewModel.ActiveBorrower, Number> colAdherentEmprunts;

    @FXML private TableView<DashboardViewModel.OverdueItem> tableOverdue;
    @FXML private TableColumn<DashboardViewModel.OverdueItem, String> colOverdueTitle;
    @FXML private TableColumn<DashboardViewModel.OverdueItem, String> colOverdueBorrower;
    @FXML private TableColumn<DashboardViewModel.OverdueItem, Number> colOverdueDays;
    @FXML private TableColumn<DashboardViewModel.OverdueItem, Number> colOverduePenalty;

    /** Conteneur principal pour le snapshot PDF. */
    @FXML private VBox dashboardContent;

    // ---------------------------------------------------------------------
    // Modèle
    // ---------------------------------------------------------------------

    private final DashboardViewModel viewModel;

    // ---------------------------------------------------------------------
    // Constantes UI
    // ---------------------------------------------------------------------

    private static final int    MAX_RECENT           = 5;
    private static final double CARD_WIDTH           = 175;
    private static final double CARD_HEIGHT          = 240;
    private static final double CARD_SPACING         = 12;
    private static final double CARD_COVER_HEIGHT    = 240;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm:ss");
    private static final DateTimeFormatter PDF_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ---------------------------------------------------------------------
    // Constructeur
    // ---------------------------------------------------------------------

    public DashboardController(DashboardViewModel viewModel) {
        this.viewModel = viewModel;
    }

    // ---------------------------------------------------------------------
    // Initialisation FXML
    // ---------------------------------------------------------------------

    @FXML
    private void initialize() {
        setupPeriodFilters(); // Correction ici pour utiliser les filtres
        setupKpiBindings();
        setupChartBindings();
        setupTableBindings();
        setupCarousel();
        setupExportPDF();

        // Rebuild du carrousel lorsqu’on a de nouveaux emprunts récents
        viewModel.recentLoans().addListener(
                (javafx.collections.ListChangeListener<DashboardViewModel.RecentLoan>) c -> rebuildCarousel()
        );
        
        // Premier refresh (par défaut 30 jours, ou selon le bouton sélectionné)
        applySelectedFilter(); 
    }

    // ---------------------------------------------------------------------
    // Filtres de période (CORRIGÉ)
    // ---------------------------------------------------------------------

    private void setupPeriodFilters() {
        periodToggleGroup = new ToggleGroup();
        btn7Days.setToggleGroup(periodToggleGroup);
        btn30Days.setToggleGroup(periodToggleGroup);
        btn90Days.setToggleGroup(periodToggleGroup);

        // Définir les "UserData" pour savoir quel bouton correspond à quelle valeur
        btn7Days.setUserData(7);
        btn30Days.setUserData(30);
        btn90Days.setUserData(90);

        // Sélection par défaut
        btn30Days.setSelected(true); 

        // Listener : Quand on change de bouton, on recharge
        periodToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                applySelectedFilter();
            } else {
                // Empêcher la désélection totale (comportement radio bouton)
                // Si l'utilisateur clique sur le bouton déjà actif, on le réactive
                if (oldVal != null) {
                    periodToggleGroup.selectToggle(oldVal); 
                }
            }
        });
    }

    /**
     * Applique le filtre en fonction du bouton sélectionné.
     */
    private void applySelectedFilter() {
        Toggle selected = periodToggleGroup.getSelectedToggle();
        int days = 30; // Valeur par défaut
        
        if (selected != null && selected.getUserData() != null) {
            days = (int) selected.getUserData();
        }
        
        System.out.println("Application du filtre : " + days + " jours.");
        viewModel.refresh(days);
    }

    // ---------------------------------------------------------------------
    // KPI & état UI
    // ---------------------------------------------------------------------

    private void setupKpiBindings() {
        lblDocs.textProperty().bind(viewModel.totalDocumentsProperty().asString());
        lblAdherents.textProperty().bind(viewModel.totalAdherentsProperty().asString());
        lblEmprunts.textProperty().bind(viewModel.totalEmpruntsProperty().asString());
        lblPenalites.textProperty().bind(Bindings.format("%.2f €", viewModel.penalitesEnCoursProperty()));
        lblRetards.textProperty().bind(viewModel.totalRetardsProperty().asString());

        lblTauxRotation.textProperty().bind(Bindings.format("%.1f",  viewModel.tauxRotationProperty()));
        lblTauxOccupation.textProperty().bind(Bindings.format("%.1f%%", viewModel.tauxOccupationProperty()));
        lblTauxUtilisationFonds.textProperty().bind(Bindings.format("%.1f%%", viewModel.tauxUtilisationFondsProperty()));
        lblTauxReemprunt.textProperty().bind(Bindings.format("%.1f%%", viewModel.tauxReempruntProperty()));
        lblDureeMoyenneEmprunts.textProperty().bind(Bindings.format("%.1f j", viewModel.dureeMoyenneEmpruntsProperty()));
        lblTauxRetard.textProperty().bind(Bindings.format("%.1f%%", viewModel.tauxRetardProperty()));
        lblTauxEngagementAdherents.textProperty().bind(Bindings.format("%.1f%%", viewModel.tauxEngagementAdherentsProperty()));

        lblLastUpdated.textProperty().bind(
                Bindings.createStringBinding(
                        () -> viewModel.lastUpdatedProperty().get() == null
                                ? "—"
                                : DATE_TIME_FORMATTER.format(viewModel.lastUpdatedProperty().get()),
                        viewModel.lastUpdatedProperty()
                )
        );

        // Erreur globale
        lblError.textProperty().bind(viewModel.errorProperty());
        lblError.visibleProperty().bind(viewModel.errorProperty().isNotEmpty());
        lblError.managedProperty().bind(lblError.visibleProperty());

        // Spinner + boutons suivent l'état "loading"
        progress.visibleProperty().bind(viewModel.loadingProperty());
        progress.managedProperty().bind(progress.visibleProperty());

        btnRefresh.disableProperty().bind(viewModel.loadingProperty());
        btnExportPDF.disableProperty().bind(viewModel.loadingProperty());

        // Le bouton refresh recharge avec le filtre actuel
        btnRefresh.setOnAction(e -> applySelectedFilter());
    }

    // ---------------------------------------------------------------------
    // Graphiques
    // ---------------------------------------------------------------------

    private void setupChartBindings() {
        // Emprunts par mois (Données filtrées provenant du ViewModel)
        if (chartLoans != null) {
            chartLoans.getData().setAll(List.of(viewModel.loansByMonthSeries()));
        }

        // Répartition par genre - Top 10 Empruntés (CORRIGÉ AXE Y)
        if (chartGenresBar != null) {
            chartGenresBar.getData().clear();

            XYChart.Series<String, Number> series = viewModel.docsByGenreSeries();
            chartGenresBar.getData().add(series);

            NumberAxis yAxis = (NumberAxis) chartGenresBar.getYAxis();
            
            // 1. AUTO RANGING doit être TRUE pour s'adapter à 2 ou 200 documents
            yAxis.setAutoRanging(true);
            
            // 2. Formatteur pour n'afficher que des ENTIERS (pas de 0.5, 1.5)
            yAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
                @Override
                public String toString(Number object) {
                    if (object.doubleValue() % 1 == 0) {
                        return String.format("%.0f", object.doubleValue()); 
                    }
                    return ""; 
                }

                @Override
                public Number fromString(String string) {
                    return Double.valueOf(string);
                }
            });
            
            // Optionnel : Forcer le pas minimum à 1 pour éviter trop de graduations si peu de données
            // Mais attention, si autoRanging est true, tickUnit est souvent ignoré ou recalculé.
            // Le formatter ci-dessus est la solution la plus robuste.
        }

        // Documents par type (Stock global)
        if (chartPeakHours != null) {
            chartPeakHours.getXAxis().setLabel("Type de document");
            chartPeakHours.getYAxis().setLabel("Quantité en stock");
            chartPeakHours.getData().setAll(List.of(viewModel.docsByTypeSeries()));
        }
    }

    // ---------------------------------------------------------------------
    // Tables
    // ---------------------------------------------------------------------

    private void setupTableBindings() {
        // Top adhérents (Filtré)
        colAdherentNom.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name()));
        colAdherentEmprunts.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().loanCount()));
        tableAdherentsActifs.setItems(viewModel.getActiveBorrowers());

        // Retards (Global - État actuel)
        colOverdueTitle.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().title()));
        colOverdueBorrower.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().borrower()));
        colOverdueDays.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().daysOverdue()));
        colOverduePenalty.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().penalty()));

        colOverdueDays.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-alignment: CENTER;");
                }
            }
        });

        colOverduePenalty.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f €", item.doubleValue()));
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                }
            }
        });

        tableOverdue.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(DashboardViewModel.OverdueItem it, boolean empty) {
                super.updateItem(it, empty);
                getStyleClass().remove("row-overdue");
                if (!empty && it != null && it.daysOverdue() > 0) {
                    getStyleClass().add("row-overdue");
                }
            }
        });

        tableOverdue.setItems(viewModel.getOverdueItems());
    }

    // ---------------------------------------------------------------------
    // Carrousel des emprunts récents (Filtré)
    // ---------------------------------------------------------------------

    private void setupCarousel() {
        if (recentStripContainer != null) {
            recentStripContainer.setSpacing(CARD_SPACING);
        }
        rebuildCarousel();
    }

    private void rebuildCarousel() {
        if (recentStripContainer == null) return;

        Platform.runLater(() -> {
            recentStripContainer.getChildren().clear();
            List<DashboardViewModel.RecentLoan> loans = viewModel.recentLoans();

            if (loans == null || loans.isEmpty()) {
                for (int i = 0; i < MAX_RECENT; i++) {
                    recentStripContainer.getChildren().add(createEmptyBookCard());
                }
                return;
            }

            int count = Math.min(loans.size(), MAX_RECENT);
            for (int i = 0; i < count; i++) {
                recentStripContainer.getChildren().add(createBookCard(loans.get(i)));
            }
        });
    }

    /** Carte placeholder lorsqu'il n'y a pas d'emprunts récents. */
    private VBox createEmptyBookCard() {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("product-card", "product-card-empty");
        card.setPrefWidth(CARD_WIDTH);
        card.setMaxWidth(CARD_WIDTH);
        card.setPrefHeight(CARD_HEIGHT);
        card.setMaxHeight(CARD_HEIGHT);
        card.setAlignment(Pos.TOP_CENTER);

        StackPane coverPane = new StackPane();
        coverPane.setPrefWidth(CARD_WIDTH);
        coverPane.setMaxWidth(CARD_WIDTH);
        coverPane.setPrefHeight(CARD_COVER_HEIGHT);
        coverPane.setMinHeight(CARD_COVER_HEIGHT);
        coverPane.setMaxHeight(CARD_COVER_HEIGHT);
        coverPane.setAlignment(Pos.CENTER);
        coverPane.getStyleClass().add("product-cover-placeholder");

        VBox textContent = new VBox(4);
        textContent.setPadding(new Insets(8));
        textContent.setAlignment(Pos.CENTER_LEFT);

        Label placeholder = new Label("Aucun emprunt récent");
        placeholder.getStyleClass().add("product-placeholder-label");
        placeholder.setWrapText(true);
        placeholder.setMaxWidth(CARD_WIDTH - 16);

        textContent.getChildren().add(placeholder);
        card.getChildren().addAll(coverPane, textContent);

        return card;
    }

    /** Carte pour un emprunt récent. */
    private VBox createBookCard(DashboardViewModel.RecentLoan item) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("product-card", "product-card--compact");
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        card.setPickOnBounds(true);
        card.setAlignment(Pos.TOP_CENTER);

        StackPane coverPane = new StackPane();
        coverPane.setPrefHeight(CARD_COVER_HEIGHT);
        coverPane.setMinHeight(CARD_COVER_HEIGHT);
        coverPane.setMaxHeight(CARD_COVER_HEIGHT);
        coverPane.setAlignment(Pos.CENTER);
        coverPane.setStyle("-fx-background-color: #eeeeee;");

        String coverRef = item.coverRef();
        if (coverRef != null && !coverRef.isBlank()) {
            try {
                File imageFile = new File(coverRef);
                if (imageFile.exists() && imageFile.isFile()) {
                    String imageUrl = imageFile.toURI().toString();
                    Image img = new Image(imageUrl, true);

                    img.errorProperty().addListener((obs, oldError, newError) -> {
                        if (newError) {
                            System.err.println("Erreur chargement image carte (dashboard) : " + imageUrl);
                            if (img.getException() != null) {
                                img.getException().printStackTrace();
                            }
                        }
                    });

                    if (!img.isError()) {
                        ImageView imgView = new ImageView(img);
                        imgView.setFitWidth(CARD_WIDTH);
                        imgView.setFitHeight(CARD_COVER_HEIGHT);
                        imgView.setPreserveRatio(false);
                        imgView.setSmooth(true);
                        coverPane.getChildren().add(imgView);
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur création URL/Image pour carte dashboard : "
                        + coverRef + " - " + e.getMessage());
            }
        }

        VBox textContent = new VBox(4);
        textContent.setPadding(new Insets(8));
        textContent.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(item.title());
        title.getStyleClass().add("product-title");
        title.setWrapText(true);
        title.setMaxWidth(CARD_WIDTH - 16);

        Label meta = new Label(item.borrower() + " • " + item.when());
        meta.getStyleClass().add("product-meta");

        textContent.getChildren().addAll(title, meta);
        card.getChildren().addAll(coverPane, textContent);

        return card;
    }

    // ---------------------------------------------------------------------
    // Export PDF (snapshot du dashboard)
    // ---------------------------------------------------------------------

    private void setupExportPDF() {
        btnExportPDF.setOnAction(e -> exportToPDF());
    }

    private void exportToPDF() {
        if (dashboardContent == null || dashboardContent.getScene() == null) {
            showErrorAlert("Erreur", "Export impossible", "Le contenu du dashboard n'est pas disponible.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le rapport en PDF");
        fileChooser.setInitialFileName("rapport_dashboard_" +
                LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".pdf");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("PDF", "*.pdf"));

        File file = fileChooser.showSaveDialog(dashboardContent.getScene().getWindow());
        if (file == null) {
            return;
        }

        WritableImage snapshot = dashboardContent.snapshot(new SnapshotParameters(), null);

        viewModel.loadingProperty().set(true);

        CompletableFuture
                .runAsync(() -> {
                    try {
                        generatePDFReportFromImage(file, snapshot);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .whenComplete((res, ex) -> Platform.runLater(() -> {
                    try {
                        if (ex != null) {
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            showErrorAlert(
                                    "Erreur d'exportation",
                                    "La génération du PDF a échoué.",
                                    cause.getMessage() != null ? cause.getMessage() : cause.toString()
                            );
                        } else {
                            showSuccessAlert(
                                    "Exportation réussie",
                                    "Le rapport PDF a été généré avec succès."
                            );
                        }
                    } finally {
                        viewModel.loadingProperty().set(false);
                    }
                }));
    }

    private void generatePDFReportFromImage(File file, WritableImage image) throws IOException {
        try (PDDocument document = new PDDocument()) {
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            PDImageXObject pdImage = PDImageXObject.createFromByteArray(
                    document, imageBytes, "dashboard-snapshot"
            );

            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            document.addPage(page);

            float margin = 50f;
            float y = pageSize.getHeight() - margin;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                String title = "Rapport du tableau de bord";
                String date  = "Généré le : " + LocalDate.now().format(PDF_DATE_FORMATTER);

                // Polices standard
                PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font dateFont  = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                // Titre
                content.beginText();
                content.setFont(titleFont, 16);
                content.newLineAtOffset(margin, y);
                content.showText(title);
                content.endText();
                y -= 16 * 1.2f;

                // Date
                content.beginText();
                content.setFont(dateFont, 10);
                content.newLineAtOffset(margin, y);
                content.showText(date);
                content.endText();
                y -= 10 * 1.2f;
                y -= 20; 

                float usableWidth       = pageSize.getWidth() - 2 * margin;
                float imageUsableHeight = y - margin;

                float imgWidth  = pdImage.getWidth();
                float imgHeight = pdImage.getHeight();

                float scaleX = usableWidth / imgWidth;
                float scaleY = imageUsableHeight / imgHeight;
                float scale  = Math.min(scaleX, scaleY);

                float scaledWidth  = imgWidth * scale;
                float scaledHeight = imgHeight * scale;

                float x = margin + (usableWidth - scaledWidth) / 2;
                float imageDrawY = y - scaledHeight;

                content.drawImage(pdImage, x, imageDrawY, scaledWidth, scaledHeight);
            }

            document.save(file);
        }
    }

    // ---------------------------------------------------------------------
    // Alertes
    // ---------------------------------------------------------------------

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content != null ? content : "");
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content != null ? content : "");
        alert.showAndWait();
    }
}