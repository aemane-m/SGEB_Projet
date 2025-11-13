package application.ui.viewmodel.controller;

import application.ui.viewmodel.DashboardViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

//--- Imports pour PDFBox (requis) ---
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
//--- IMPORTS AJOUTÉS POUR LE TEXTE PDF ---
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
//--- Fin Imports PDFBox ---

// --- IMPORTS AJOUTÉS POUR LA CAPTURE D'ÉCRAN ---
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
// --- FIN IMPORTS AJOUTÉS ---

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur du Dashboard.
 * CORRIGÉ : Export PDF basé sur une capture (snapshot) de la scène.
 */
public class DashboardController {

    // --- (Champs FXML) ---
    @FXML private Label lblDocs;
    @FXML private Label lblAdherents;
    @FXML private Label lblEmprunts;
    @FXML private Label lblPenalites;
    @FXML private Label lblRetards;
    @FXML private Label lblTauxRotation;
    @FXML private Label lblTauxOccupation;
    @FXML private Label lblLastUpdated;
    @FXML private Label lblError;
    @FXML private ProgressIndicator progress;
    @FXML private Button btnRefresh;
    @FXML private Button btnExportPDF;
    @FXML private ToggleButton btn7Days;
    @FXML private ToggleButton btn30Days;
    @FXML private ToggleButton btn90Days;
    private ToggleGroup periodToggleGroup;
    @FXML private FlowPane recentStripContainer;
    @FXML private AreaChart<String, Number> chartLoans;
    @FXML private PieChart chartGenres;
    @FXML private BarChart<String, Number> chartPeakHours;
    @FXML private TableView<DashboardViewModel.ActiveBorrower> tableAdherentsActifs;
    @FXML private TableColumn<DashboardViewModel.ActiveBorrower, String> colAdherentNom;
    @FXML private TableColumn<DashboardViewModel.ActiveBorrower, Number> colAdherentEmprunts;
    @FXML private TableView<DashboardViewModel.OverdueItem> tableOverdue;
    @FXML private TableColumn<DashboardViewModel.OverdueItem, String> colOverdueTitle;
    @FXML private TableColumn<DashboardViewModel.OverdueItem, String> colOverdueBorrower;
    @FXML private TableColumn<DashboardViewModel.OverdueItem, Number> colOverdueDays;
    @FXML private TableColumn<DashboardViewModel.OverdueItem, Number> colOverduePenalty;

    // --- CHAMP AJOUTÉ POUR LA CAPTURE ---
    @FXML private VBox dashboardContent; // Le conteneur VBox principal dans le ScrollPane

    private final DashboardViewModel viewModel;
    
    // --- (Constantes inchangées) ---
    private static final double CARD_WIDTH   = 175;
    private static final double CARD_HEIGHT  = 240;
    private static final double CARD_SPACING = 12;
    private static final double CARD_COVER_HEIGHT = 240;
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm:ss");
    private static final DateTimeFormatter PDF_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");


    public DashboardController(DashboardViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    private void initialize() {
        setupPeriodFilters();
        setupKpiBindings();
        setupChartBindings();
        setupTableBindings();
        setupCarousel();
        setupExportPDF();
        
        viewModel.recentLoans().addListener((javafx.collections.ListChangeListener<DashboardViewModel.RecentLoan>) c -> {
            rebuildCarousel();
        });
    }

    // ... (setupPeriodFilters - inchangé) ...
    private void setupPeriodFilters() {
        periodToggleGroup = new ToggleGroup();
        btn7Days.setToggleGroup(periodToggleGroup);
        btn30Days.setToggleGroup(periodToggleGroup);
        btn90Days.setToggleGroup(periodToggleGroup);

        // Valeur par défaut : 7 jours (ou 30 si btn7Days est null)
        if (btn7Days != null) btn7Days.setSelected(true);
        else if (btn30Days != null) btn30Days.setSelected(true);
        
        periodToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // CORRIGÉ : Suppression des variables inutilisées.
                // Le ViewModel est rafraîchi, et il (hypothétiquement)
                // connaît le filtre actif via d'autres moyens ou utilise une valeur par défaut.
                viewModel.refresh();
            }
        });
    }

    // ... (setupKpiBindings - inchangé) ...
    private void setupKpiBindings() {
        lblDocs.textProperty().bind(viewModel.totalDocumentsProperty().asString());
        lblAdherents.textProperty().bind(viewModel.totalAdherentsProperty().asString());
        lblEmprunts.textProperty().bind(viewModel.totalEmpruntsProperty().asString());
        lblPenalites.textProperty().bind(Bindings.format("%.2f €", viewModel.penalitesEnCoursProperty()));
        lblRetards.textProperty().bind(viewModel.totalRetardsProperty().asString());
        lblTauxRotation.textProperty().bind(Bindings.format("%.1f", viewModel.tauxRotationProperty()));
        lblTauxOccupation.textProperty().bind(Bindings.format("%.1f%%", viewModel.tauxOccupationProperty()));

        lblLastUpdated.textProperty().bind(
                Bindings.createStringBinding(
                        () -> viewModel.lastUpdatedProperty().get() == null
                                ? "—"
                                : DATE_TIME_FORMATTER.format(viewModel.lastUpdatedProperty().get()),
                        viewModel.lastUpdatedProperty()
                )
        );

        lblError.textProperty().bind(viewModel.errorProperty());
        lblError.visibleProperty().bind(viewModel.errorProperty().isNotEmpty());
        lblError.managedProperty().bind(lblError.visibleProperty());

        // CORRIGÉ : Ne pas lier 'visibleProperty' si on veut le contrôler manuellement.
        // progress.visibleProperty().bind(viewModel.loadingProperty());
        progress.managedProperty().bind(progress.visibleProperty());
        
        // On peut toujours lier la *visibilité* au *loading*, MAIS
        // lier aussi la *désactivation* des boutons au loading.
        progress.visibleProperty().bind(viewModel.loadingProperty());
        btnRefresh.disableProperty().bind(viewModel.loadingProperty());
        
        // Le bouton Export sera géré manuellement car il démarre un thread
        // btnExportPDF.disableProperty().bind(viewModel.loadingProperty()); 

        btnRefresh.setOnAction(e -> {
            viewModel.refresh();
        });
    }


    // ... (setupChartBindings - CORRIGÉ : ne charge plus de données fictives) ...
    private void setupChartBindings() {
        // AreaChart : emprunts par mois (lié au VM)
        if (chartLoans != null) {
            // CORRIGÉ : Utilise List.of() pour éviter l'avertissement varargs
            chartLoans.getData().setAll(List.of(viewModel.loansByMonthSeries()));
        }

        // PieChart : docs par genre (lié au VM)
        if (chartGenres != null) {
            chartGenres.setData(viewModel.docsByGenre()); // setData est déjà correct
        }

        // CORRIGÉ : BarChart : "Peak Hours" FXML est réutilisé pour "Docs By Type"
        if (chartPeakHours != null) {
            chartPeakHours.getXAxis().setLabel("Type de Document");
            chartPeakHours.getYAxis().setLabel("Quantité");
            // CORRIGÉ : Utilise List.of() pour éviter l'avertissement varargs
            chartPeakHours.getData().setAll(List.of(viewModel.docsByTypeSeries()));
        }
    }

    // ... (setupTableBindings - inchangé, c'est déjà bien lié) ...
    private void setupTableBindings() {
        // Table: Adhérents actifs
        colAdherentNom.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name()));
        colAdherentEmprunts.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().loanCount()));
        tableAdherentsActifs.setItems(viewModel.getActiveBorrowers());

        // Table: Retards
        colOverdueTitle.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().title()));
        colOverdueBorrower.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().borrower()));
        colOverdueDays.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().daysOverdue()));
        colOverduePenalty.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().penalty()));

        // ... (Cell factories pour le style des retards - inchangées) ...
        colOverdueDays.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item.toString());
                    setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-alignment: CENTER;");
                }
            }
        });
        colOverduePenalty.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
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

    // -------------------------------
    // Carrousel (CORRIGÉ)
    // -------------------------------
    
    /** CORRIGÉ : Applique l'espacement au FlowPane */
   
    /** CORRIGÉ : Applique l'espacement au HBox */
    private void setupCarousel() {
        if (recentStripContainer != null) {
            recentStripContainer.setHgap(CARD_SPACING); // Espacement horizontal
            recentStripContainer.setVgap(CARD_SPACING); // Espacement vertical (si wrap)
        }
    }

    private void rebuildCarousel() {
        if (recentStripContainer == null) return;
        Platform.runLater(() -> {
            recentStripContainer.getChildren().clear();
            List<DashboardViewModel.RecentLoan> loans = viewModel.recentLoans();
            if (loans == null || loans.isEmpty()) {
                recentStripContainer.getChildren().add(new Label("Aucun emprunt récent."));
                return;
            }
            for (DashboardViewModel.RecentLoan item : loans) {
                VBox card = createBookCard(item);
                recentStripContainer.getChildren().add(card);
            }
        });
    }

    /** CORRIGÉ : Utilise les nouvelles constantes de taille */
    /** CORRIGÉ : Affiche l'image de couverture si elle existe. */
    private VBox createBookCard(DashboardViewModel.RecentLoan item) {
        VBox card = new VBox(6);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(CARD_WIDTH);
        card.setPrefHeight(CARD_HEIGHT); // Taille fixe
        card.setMaxHeight(CARD_HEIGHT);
        card.setAlignment(Pos.TOP_CENTER);

        StackPane coverPane = new StackPane();
        coverPane.setPrefHeight(CARD_COVER_HEIGHT);
        coverPane.setMinHeight(CARD_COVER_HEIGHT);
        coverPane.setMaxHeight(CARD_COVER_HEIGHT);
        coverPane.setAlignment(Pos.CENTER);
        coverPane.setStyle("-fx-background-color: #eeeeee;"); // Fond gris

        // --- CORRIGÉ : Logique de chargement d'image ---
        String coverRef = item.coverRef();
        if (coverRef != null && !coverRef.isBlank()) {
            try {
                File imageFile = new File(coverRef);
                if (imageFile.exists() && imageFile.isFile()) {
                    String imageUrl = imageFile.toURI().toString();
                    Image img = new Image(imageUrl, true); 
                    img.errorProperty().addListener((obs, old, err) -> {
                        if(err) System.err.println("Dashboard: Image non chargée " + imageUrl);
                    });
                    
                    if (!img.isError()) {
                        ImageView imgView = new ImageView(img);
                        imgView.setFitWidth(CARD_WIDTH);
                        imgView.setFitHeight(CARD_COVER_HEIGHT);
                        imgView.setPreserveRatio(false); // Remplit le cadre
                        imgView.setSmooth(true);
                        coverPane.getChildren().add(imgView);
                    }
                }
            } catch (Exception e) {
                System.err.println("Dashboard: Erreur chargement image " + coverRef);
            }
        }
        // --- Fin de la logique d'image ---
        
        // Zone de texte
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

 // -------------------------------
    // Export PDF (CORRIGÉ - Snapshot)
    // -------------------------------
    
    private void setupExportPDF() {
        btnExportPDF.setOnAction(e -> exportToPDF());
    }

    /**
     * CORRIGÉ : Prend un snapshot de la scène AVANT de lancer le thread d'export.
     */
    private void exportToPDF() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exporter le rapport en PDF");
            fileChooser.setInitialFileName("rapport_dashboard_" +
                    LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".pdf");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF", "*.pdf")
            );

            File file = fileChooser.showSaveDialog(btnExportPDF.getScene().getWindow());
            if (file != null) {
                viewModel.loadingProperty().set(true);
                btnExportPDF.setDisable(true);
                
                // NOUVEAU : Prendre la capture sur le thread JavaFX
                // Nous capturons le VBox "dashboardContent"
                final WritableImage snapshot = dashboardContent.snapshot(new SnapshotParameters(), null);

                new Thread(() -> {
                    try {
                        // NOUVEAU : Appeler la nouvelle méthode de génération
                        generatePDFReportFromImage(file, snapshot); 
                        
                        Platform.runLater(() -> {
                            viewModel.loadingProperty().set(false); // Libère le VM
                            btnExportPDF.setDisable(false);
                            showSuccessAlert("Exportation réussie",
                                    "Le rapport PDF a été généré avec succès.");
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            viewModel.loadingProperty().set(false); // Libère le VM
                            btnExportPDF.setDisable(false);
                            showErrorAlert("Erreur d'exportation",
                                    "La génération du PDF a échoué.", ex.getMessage());
                            ex.printStackTrace();
                        });
                    }
                }).start();
            }
        } catch (Exception ex) {
            showErrorAlert("Erreur", "Impossible d'ouvrir la boîte de dialogue.", ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    
    /**
     * NOUVEAU : Génère un PDF en insérant une image capturée de la scène.
     * CORRIGÉ : Ajout d'un titre et d'une date au-dessus de l'image.
     * @param file Le fichier où sauvegarder.
     * @param image L'image WritableImage capturée depuis la scène JavaFX.
     * @throws IOException
     */
    private void generatePDFReportFromImage(File file, WritableImage image) throws IOException {
        
        try (PDDocument document = new PDDocument()) {
            
            // 1. Convertir l'image JavaFX en image AWT/Bytes
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Utiliser PNG pour garder la transparence (si nécessaire) et la qualité
            ImageIO.write(bufferedImage, "png", baos); 
            byte[] imageBytes = baos.toByteArray();
            
            // 2. Créer un objet image PDFBox
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes, "dashboard-snapshot");

            // 3. Préparer la page A4 et les marges
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            document.addPage(page);

            float margin = 50; // Marge de 50 points (environ 1.76cm)
            
            // NOUVEAU: Coordonnée Y de départ (en haut de la page)
            float y = pageSize.getHeight() - margin;

            // 4. Écrire le texte (Titre et Date)
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                
                // Définir les textes
                String title = "Rapport du Tableau de Bord";
                String date = "Généré le : " + LocalDate.now().format(PDF_DATE_FORMATTER);

                // Définir les polices
                PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font dateFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                
                // Écrire le titre
                content.beginText();
                content.setFont(titleFont, 16);
                content.newLineAtOffset(margin, y);
                content.showText(title);
                content.endText();
                y -= 16 * 1.2f; // Décalage pour la ligne suivante

                // Écrire la date
                content.beginText();
                content.setFont(dateFont, 10);
                content.newLineAtOffset(margin, y);
                content.showText(date);
                content.endText();
                y -= 10 * 1.2f;

                // Ajouter un espace avant l'image
                y -= 20;

                // 5. Calculer la zone restante pour l'image
                float usableWidth = pageSize.getWidth() - 2 * margin;
                float imageUsableHeight = y - margin; // Hauteur restante (de y jusqu'à la marge du bas)
                
                float imgWidth = pdImage.getWidth();
                float imgHeight = pdImage.getHeight();
                
                // 6. Calculer le redimensionnement (en gardant les proportions)
                float scaleX = usableWidth / imgWidth;
                float scaleY = imageUsableHeight / imgHeight;
                float scale = Math.min(scaleX, scaleY); 
                
                float scaledWidth = imgWidth * scale;
                float scaledHeight = imgHeight * scale;
                
                // 7. Centrer l'image dans l'espace restant
                float x = margin + (usableWidth - scaledWidth) / 2;
                // Le Y de dessin est le bas de l'image. On le place en haut de la zone dispo (y)
                // moins sa propre hauteur.
                float imageDrawY = y - scaledHeight; 
                
                // 8. Dessiner l'image
                content.drawImage(pdImage, x, imageDrawY, scaledWidth, scaledHeight);
            
            } // Le content stream se ferme ici
            
            // 9. Sauvegarder le document
            document.save(file);
            
        } // Le document.close() est appelé automatiquement ici
    }
    
    // --- (Méthodes d'alerte inchangées) ---

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
