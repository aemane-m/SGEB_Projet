package application.ui.viewmodel;

import application.domain.core.document.Document;
import application.domain.core.emprunt.Emprunt;
import application.domain.core.personne.Adherent;
import application.domain.ports.repository.AdherentRepository;
import application.domain.ports.repository.DocumentRepository;
import application.domain.ports.repository.EmpruntRepository;
import application.domain.ports.repository.PenaliteRepository;
import application.domain.service.DashboardService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle; // Ajouté
import java.util.Comparator; // Ajouté
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ViewModel du Dashboard.
 * CORRIGÉ : Connecté aux services et repositories réels.
 * CORRIGE : Le carrousel "RecentLoan" inclut le coverRef.
 */
public class DashboardViewModel {

    // --- Services & Repositories ---
    private final DashboardService dashboardService;
    private final DocumentRepository documentRepository;
    private final AdherentRepository adherentRepository;
    private final EmpruntRepository empruntRepository;

    private final String username;
    private final String role;

    // --- KPIs ---
    private final IntegerProperty totalDocuments = new SimpleIntegerProperty(0);
    private final IntegerProperty totalAdherents = new SimpleIntegerProperty(0);
    private final IntegerProperty totalEmprunts = new SimpleIntegerProperty(0);
    private final DoubleProperty penalitesEnCours = new SimpleDoubleProperty(0.0);
    private final IntegerProperty totalRetards = new SimpleIntegerProperty(0);
    private final DoubleProperty tauxRotation = new SimpleDoubleProperty(0.0);
    private final DoubleProperty tauxOccupation = new SimpleDoubleProperty(0.0);

    // --- UI State ---
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty("");
    private final ObjectProperty<LocalDateTime> lastUpdated = new SimpleObjectProperty<>(null);

    // --- Charts Data ---
    private final XYChart.Series<String, Number> loansByMonth = new XYChart.Series<>();
    private final ObservableList<PieChart.Data> docsByGenre = FXCollections.observableArrayList();
    private final XYChart.Series<String, Number> docsByType = new XYChart.Series<>();

    // --- Dynamic Lists Data (DTOs) ---

    // CORRIGÉ : Ajout de coverRef
    public record RecentLoan(String title, String borrower, String when, String coverRef) {}
    private final ObservableList<RecentLoan> recentLoans = FXCollections.observableArrayList();
    
    public record ActiveBorrower(String name, int loanCount) {}
    private final ObservableList<ActiveBorrower> activeBorrowers = FXCollections.observableArrayList();
    
    public record OverdueItem(String title, String borrower, int daysOverdue, double penalty) {}
    private final ObservableList<OverdueItem> overdueItems = FXCollections.observableArrayList();


    /**
     * Constructeur principal (production).
     */
    public DashboardViewModel(DashboardService dashboardService,
                              DocumentRepository documentRepository,
                              AdherentRepository adherentRepository,
                              EmpruntRepository empruntRepository,
                              PenaliteRepository penaliteRepository, // Reçu mais géré par DashboardService
                              String username,
                              String role) {
        this.dashboardService = Objects.requireNonNull(dashboardService, "dashboardService");
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository");
        this.adherentRepository = Objects.requireNonNull(adherentRepository, "adherentRepository");
        this.empruntRepository = Objects.requireNonNull(empruntRepository, "empruntRepository");
        this.username = username;
        this.role = role;
        
        loansByMonth.setName("Emprunts/Mois");
        docsByType.setName("Quantité");
        
        refresh();
    }
    
    // Le constructeur MOCK est supprimé, on utilise les vraies données.

    /**
     * Rafraîchit toutes les données du ViewModel en appelant les services/repositories.
     */
    public void refresh() {
        runSafely(() -> { // S'exécute sur un thread d'arrière-plan

            // 1. KPIs de base (via le service)
            int docs = dashboardService.countDocuments();
            int adherents = dashboardService.countAdherents();
            int empruntsActifs = dashboardService.countEmprunts();
            double penalites = dashboardService.sumPenalites();
            int retards = dashboardService.countRetards();

            // 2. Calcul des KPIs dérivés
            double rotation = docs > 0 ? (empruntsActifs * 12.0) / docs : 0.0;
            double occupation = docs > 0 ? (empruntsActifs * 100.0) / docs : 0.0;
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = LocalDate.now();

            // 3. Graphe Emprunts par mois (Données réelles)
            List<Emprunt> allEmprunts = empruntRepository.findAll();
            Map<String, Long> loansByMonthMap = allEmprunts.stream()
                .filter(e -> e.getDateEmprunt().getYear() == today.getYear())
                .collect(Collectors.groupingBy(
                    e -> e.getDateEmprunt().getMonth().toString(),
                    Collectors.counting()
                ));
            var loansData = loansByMonthMap.entrySet().stream()
                .map(e -> new XYChart.Data<String, Number>(e.getKey().substring(0, 3), e.getValue()))
                .toList();

            // 4. Graphe Répartition par genre (Données réelles)
            List<Document> allDocuments = documentRepository.findAll(); // Récupère une fois
            Map<String, Long> genreCountMap = allDocuments.stream()
                .collect(Collectors.groupingBy(
                    d -> (d.getGenre() == null || d.getGenre().isBlank()) ? "Inconnu" : d.getGenre(),
                    Collectors.counting()
                ));
            var genreData = genreCountMap.entrySet().stream()
                .map(e -> new PieChart.Data(e.getKey(), e.getValue()))
                .toList();

            // 5. Carrousel Récents (Données réelles)
            // CORRIGÉ : Ajout de e.getDocument().getCoverRef()
            var recentData = allEmprunts.stream()
                .sorted(Comparator.comparing(Emprunt::getDateEmprunt).reversed())
                .limit(5)
                .map(e -> new RecentLoan(
                    e.getDocument().getTitre(),
                    e.getEmprunteur().getNomComplet(),
                    e.getDateEmprunt().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)),
                    e.getDocument().getCoverRef() // <-- Le chemin de l'image est ajouté ici
                ))
                .toList();

            // 6. Table Adhérents Actifs (Données réelles)
            List<Emprunt> activeEmprunts = empruntRepository.findActiveEmprunts();
            Map<Adherent, Long> activeBorrowerMap = activeEmprunts.stream()
                 .collect(Collectors.groupingBy(Emprunt::getEmprunteur, Collectors.counting()));
            var activeData = activeBorrowerMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(5)
                .map(e -> new ActiveBorrower(
                    e.getKey().getNomComplet(),
                    e.getValue().intValue()
                ))
                .toList();

            // 7. Table Retards (Données réelles)
            var overdueData = empruntRepository.findLateEmprunts(today).stream()
                .map(e -> new OverdueItem(
                    e.getDocument().getTitre(),
                    e.getEmprunteur().getNomComplet(),
                    (int) e.joursDeRetard(today),
                    e.penaliteCourante(today).amount().doubleValue()
                ))
                .toList();

            // 8. Graphe "Type de Document" (Données réelles)
            Map<String, Long> docTypeMap = allDocuments.stream()
                .collect(Collectors.groupingBy(
                    d -> (d.getDtype() == null || d.getDtype().isBlank()) ? "Autre" : d.getDtype(),
                    Collectors.counting()
                ));
            var docTypeData = docTypeMap.entrySet().stream()
                 .map(e -> new XYChart.Data<String, Number>(e.getKey(), e.getValue()))
                 .toList();

            // 9. Mise à jour de l'UI (Retour sur le Thread JavaFX)
            Platform.runLater(() -> {
                totalDocuments.set(docs);
                totalAdherents.set(adherents);
                totalEmprunts.set(empruntsActifs);
                penalitesEnCours.set(penalites);
                totalRetards.set(retards);
                tauxRotation.set(rotation);
                tauxOccupation.set(occupation);
                lastUpdated.set(now);

                loansByMonth.getData().setAll(loansData);
                docsByGenre.setAll(genreData);
                recentLoans.setAll(recentData);
                activeBorrowers.setAll(activeData);
                overdueItems.setAll(overdueData);
                docsByType.getData().setAll(docTypeData);
            });
        });
    }

    // --- Utilitaires ---
    private void runSafely(Runnable op) {
        setLoading(true);
        setError("");
        CompletableFuture.runAsync(op)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        setError(cause.getMessage() != null ? cause.getMessage() : cause.toString());
                    }
                    setLoading(false);
                });
    }

    private void setLoading(boolean v) {
        if (Platform.isFxApplicationThread()) loading.set(v);
        else Platform.runLater(() -> loading.set(v));
    }

    private void setError(String msg) {
        if (Platform.isFxApplicationThread()) error.set(msg);
        else Platform.runLater(() -> error.set(msg));
    }

    // --- Exposants UI (Getters pour les propriétés) ---
    public ReadOnlyIntegerProperty totalDocumentsProperty() { return totalDocuments; }
    public ReadOnlyIntegerProperty totalAdherentsProperty() { return totalAdherents; }
    public ReadOnlyIntegerProperty totalEmpruntsProperty() { return totalEmprunts; }
    public ReadOnlyDoubleProperty penalitesEnCoursProperty() { return penalitesEnCours; }
    public ReadOnlyIntegerProperty totalRetardsProperty() { return totalRetards; }
    public ReadOnlyDoubleProperty tauxRotationProperty() { return tauxRotation; }
    public ReadOnlyDoubleProperty tauxOccupationProperty() { return tauxOccupation; }
    public BooleanProperty loadingProperty() { return loading; }
    public StringProperty errorProperty() { return error; }
    public ObjectProperty<LocalDateTime> lastUpdatedProperty() { return lastUpdated; }
    public XYChart.Series<String, Number> loansByMonthSeries() { return loansByMonth; }
    public ObservableList<PieChart.Data> docsByGenre() { return docsByGenre; }
    public XYChart.Series<String, Number> docsByTypeSeries() { return docsByType; }
    public ObservableList<RecentLoan> recentLoans() { return recentLoans; }
    public ObservableList<ActiveBorrower> getActiveBorrowers() { return activeBorrowers; }
    public ObservableList<OverdueItem> getOverdueItems() { return overdueItems; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
}

