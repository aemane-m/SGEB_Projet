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
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * ViewModel du Dashboard - version simplifiée.
 *
 * Idée :
 *  - une seule méthode refresh() qui appelle des méthodes privées
 *    plus simples et compréhensibles.
 */
public class DashboardViewModel {

    // --- Services & Repos ---
    private final DashboardService dashboardService;
    private final DocumentRepository documentRepository;
    private final AdherentRepository adherentRepository;
    private final EmpruntRepository empruntRepository;
    private final PenaliteRepository penaliteRepository;
    private final String username;
    private final String role;

    // --- KPIs simples ---
    private final IntegerProperty totalDocuments     = new SimpleIntegerProperty(0);
    private final IntegerProperty totalAdherents     = new SimpleIntegerProperty(0);
    private final IntegerProperty totalEmprunts      = new SimpleIntegerProperty(0);
    private final DoubleProperty penalitesEnCours    = new SimpleDoubleProperty(0.0);
    private final IntegerProperty totalRetards       = new SimpleIntegerProperty(0);
    private final DoubleProperty tauxRotation        = new SimpleDoubleProperty(0.0);
    private final DoubleProperty tauxOccupation      = new SimpleDoubleProperty(0.0);

    // --- KPIs avancés ---
    private final DoubleProperty tauxUtilisationFonds   = new SimpleDoubleProperty(0.0);
    private final DoubleProperty tauxReemprunt          = new SimpleDoubleProperty(0.0);
    private final DoubleProperty dureeMoyenneEmprunts   = new SimpleDoubleProperty(0.0);
    private final DoubleProperty tauxRetard             = new SimpleDoubleProperty(0.0);
    private final DoubleProperty tauxEngagementAdherents= new SimpleDoubleProperty(0.0);

    // --- État UI ---
    private final BooleanProperty loading       = new SimpleBooleanProperty(false);
    private final StringProperty  error         = new SimpleStringProperty("");
    private final ObjectProperty<LocalDateTime> lastUpdated = new SimpleObjectProperty<>(null);

    // --- Graphiques ---
    private final XYChart.Series<String, Number> loansByMonth = new XYChart.Series<>();
    private final ObservableList<PieChart.Data> docsByGenre   = FXCollections.observableArrayList();
    private final XYChart.Series<String, Number> docsByType   = new XYChart.Series<>();

    // --- Listes dynamiques ---

    public record RecentLoan(String title, String borrower, String when, String coverRef) {}
    private final ObservableList<RecentLoan> recentLoans = FXCollections.observableArrayList();

    public record ActiveBorrower(String name, int loanCount) {}
    private final ObservableList<ActiveBorrower> activeBorrowers = FXCollections.observableArrayList();

    public record OverdueItem(String title, String borrower, int daysOverdue, double penalty) {}
    private final ObservableList<OverdueItem> overdueItems = FXCollections.observableArrayList();


    // =========================================================
    // Constructeur
    // =========================================================

    public DashboardViewModel(DashboardService dashboardService,
                              DocumentRepository documentRepository,
                              AdherentRepository adherentRepository,
                              EmpruntRepository empruntRepository,
                              PenaliteRepository penaliteRepository,
                              String username,
                              String role) {

        this.dashboardService   = Objects.requireNonNull(dashboardService);
        this.documentRepository = Objects.requireNonNull(documentRepository);
        this.adherentRepository = Objects.requireNonNull(adherentRepository);
        this.empruntRepository  = Objects.requireNonNull(empruntRepository);
        this.penaliteRepository = Objects.requireNonNull(penaliteRepository);
        this.username = username;
        this.role     = role;

        loansByMonth.setName("Emprunts/Mois");
        docsByType.setName("Quantité");

        refresh();
    }

    // =========================================================
    // Rafraîchissement global
    // =========================================================

    public void refresh() {
        runSafely(() -> {

            LocalDateTime now = LocalDateTime.now();
            LocalDate today   = LocalDate.now();

            // Récupération de base
            int docs       = dashboardService.countDocuments();
            int adherents  = dashboardService.countAdherents();
            int emprunts   = dashboardService.countEmprunts();
            double penal   = dashboardService.sumPenalites();
            int retards    = dashboardService.countRetards();

            List<Document> allDocuments = documentRepository.findAll();
            List<Emprunt>  allEmprunts  = empruntRepository.findAll();
            List<Emprunt>  activeEmprunts = empruntRepository.findActiveEmprunts();
            List<Emprunt>  lateEmprunts   = empruntRepository.findLateEmprunts(today);

            // KPIs simples
            double rotation   = (docs > 0) ? (emprunts * 12.0) / docs : 0.0;
            double occupation = (docs > 0) ? (emprunts * 100.0) / docs : 0.0;

            // Graphiques
            List<XYChart.Data<String, Number>> loansData   = computeLoansByMonth(allEmprunts, today);
            List<PieChart.Data> genreData                  = computeDocsByGenre(allDocuments);
            List<XYChart.Data<String, Number>> docTypeData = computeDocsByType(allDocuments);

            // Listes dynamiques
            List<RecentLoan>     recentData  = computeRecentLoans(allEmprunts);
            List<ActiveBorrower> activeData  = computeActiveBorrowers(activeEmprunts);
            List<OverdueItem>    overdueData = computeOverdueItems(lateEmprunts, today);

            // KPIs avancés
            double kpiUtilisationFonds   = computeTauxUtilisationFonds(allDocuments, allEmprunts);
            double kpiReemprunt          = computeTauxReemprunt(allEmprunts);
            double kpiDureeMoyenne       = computeDureeMoyenne(allEmprunts);
            double kpiTauxRetard         = (emprunts > 0) ? (retards * 100.0) / emprunts : 0.0;
            double kpiEngagementAdh      = computeTauxEngagementAdherents(adherents, allEmprunts);

            // Mise à jour UI
            Platform.runLater(() -> {
                totalDocuments.set(docs);
                totalAdherents.set(adherents);
                totalEmprunts.set(emprunts);
                penalitesEnCours.set(penal);
                totalRetards.set(retards);
                tauxRotation.set(rotation);
                tauxOccupation.set(occupation);
                lastUpdated.set(now);

                tauxUtilisationFonds.set(kpiUtilisationFonds);
                tauxReemprunt.set(kpiReemprunt);
                dureeMoyenneEmprunts.set(kpiDureeMoyenne);
                tauxRetard.set(kpiTauxRetard);
                tauxEngagementAdherents.set(kpiEngagementAdh);

                loansByMonth.getData().setAll(loansData);
                docsByGenre.setAll(genreData);
                docsByType.getData().setAll(docTypeData);

                recentLoans.setAll(recentData);
                activeBorrowers.setAll(activeData);
                overdueItems.setAll(overdueData);
            });
        });
    }

    // =========================================================
    // Méthodes privées : calculs simples, lisibles
    // =========================================================

    private List<XYChart.Data<String, Number>> computeLoansByMonth(List<Emprunt> allEmprunts, LocalDate today) {
        Map<String, Integer> map = new LinkedHashMap<>();

        for (Emprunt e : allEmprunts) {
            if (e.getDateEmprunt() == null) continue;
            if (e.getDateEmprunt().getYear() != today.getYear()) continue;

            String month = e.getDateEmprunt().getMonth().toString(); // JANUARY, FEBRUARY...
            map.put(month, map.getOrDefault(month, 0) + 1);
        }

        List<XYChart.Data<String, Number>> data = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String label = entry.getKey().substring(0, 3); // JAN -> JAN
            data.add(new XYChart.Data<>(label, entry.getValue()));
        }
        return data;
    }

    private List<PieChart.Data> computeDocsByGenre(List<Document> allDocuments) {
        Map<String, Integer> map = new LinkedHashMap<>();

        for (Document d : allDocuments) {
            String g = d.getGenre();
            if (g == null || g.isBlank()) g = "Inconnu";
            map.put(g, map.getOrDefault(g, 0) + 1);
        }

        List<PieChart.Data> data = new ArrayList<>();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            data.add(new PieChart.Data(e.getKey(), e.getValue()));
        }
        return data;
    }

    private List<XYChart.Data<String, Number>> computeDocsByType(List<Document> allDocuments) {
        Map<String, Integer> map = new LinkedHashMap<>();

        for (Document d : allDocuments) {
            String t = d.getDtype();
            if (t == null || t.isBlank()) t = "Autre";
            map.put(t, map.getOrDefault(t, 0) + 1);
        }

        List<XYChart.Data<String, Number>> data = new ArrayList<>();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            data.add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        return data;
    }

    private List<RecentLoan> computeRecentLoans(List<Emprunt> allEmprunts) {
        // On copie dans une nouvelle liste pour pouvoir trier
        List<Emprunt> copy = new ArrayList<>(allEmprunts);
        copy.removeIf(e -> e.getDateEmprunt() == null ||
                           e.getDocument() == null ||
                           e.getEmprunteur() == null);

        copy.sort((e1, e2) -> e2.getDateEmprunt().compareTo(e1.getDateEmprunt())); // plus récents d'abord

        List<RecentLoan> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);

        int limit = Math.min(5, copy.size());
        for (int i = 0; i < limit; i++) {
            Emprunt e = copy.get(i);
            result.add(new RecentLoan(
                    e.getDocument().getTitre(),
                    e.getEmprunteur().getNomComplet(),
                    e.getDateEmprunt().format(fmt),
                    e.getDocument().getCoverRef()
            ));
        }
        return result;
    }

    private List<ActiveBorrower> computeActiveBorrowers(List<Emprunt> activeEmprunts) {
        Map<Adherent, Integer> map = new HashMap<>();

        for (Emprunt e : activeEmprunts) {
            Adherent a = e.getEmprunteur();
            if (a == null) continue;
            map.put(a, map.getOrDefault(a, 0) + 1);
        }

        List<Map.Entry<Adherent, Integer>> entries = new ArrayList<>(map.entrySet());
        entries.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue())); // desc

        List<ActiveBorrower> result = new ArrayList<>();
        int limit = Math.min(5, entries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<Adherent, Integer> e = entries.get(i);
            result.add(new ActiveBorrower(e.getKey().getNomComplet(), e.getValue()));
        }
        return result;
    }

    private List<OverdueItem> computeOverdueItems(List<Emprunt> lateEmprunts, LocalDate today) {
        List<OverdueItem> result = new ArrayList<>();

        for (Emprunt e : lateEmprunts) {
            if (e.getDocument() == null || e.getEmprunteur() == null) continue;
            int jours = (int) e.joursDeRetard(today);
            double pen = e.penaliteCourante(today).amount().doubleValue();
            result.add(new OverdueItem(
                    e.getDocument().getTitre(),
                    e.getEmprunteur().getNomComplet(),
                    jours,
                    pen
            ));
        }
        return result;
    }

    // === KPIs avancés ===

    private double computeTauxUtilisationFonds(List<Document> docs, List<Emprunt> allEmprunts) {
        if (docs.isEmpty()) return 0.0;

        Set<Document> docsEmpruntes = new HashSet<>();
        for (Emprunt e : allEmprunts) {
            if (e.getDocument() != null) {
                docsEmpruntes.add(e.getDocument());
            }
        }
        return (docsEmpruntes.size() * 100.0) / docs.size();
    }

    private double computeTauxReemprunt(List<Emprunt> allEmprunts) {
        Map<Document, Integer> map = new HashMap<>();

        for (Emprunt e : allEmprunts) {
            Document d = e.getDocument();
            if (d == null) continue;
            map.put(d, map.getOrDefault(d, 0) + 1);
        }

        if (map.isEmpty()) return 0.0;

        int docs1 = map.size();
        int docs2 = 0;
        for (int count : map.values()) {
            if (count >= 2) docs2++;
        }

        return (docs2 * 100.0) / docs1;
    }

    private double computeDureeMoyenne(List<Emprunt> allEmprunts) {
        long totalJours = 0;
        int count = 0;

        for (Emprunt e : allEmprunts) {
            if (e.getDateEmprunt() != null && e.getDateRetourEffective() != null) {
                long jours = ChronoUnit.DAYS.between(e.getDateEmprunt(), e.getDateRetourEffective());
                totalJours += jours;
                count++;
            }
        }

        if (count == 0) return 0.0;
        return (double) totalJours / count;
    }

    private double computeTauxEngagementAdherents(int totalAdherents, List<Emprunt> allEmprunts) {
        if (totalAdherents == 0) return 0.0;

        Set<Adherent> set = new HashSet<>();
        for (Emprunt e : allEmprunts) {
            if (e.getEmprunteur() != null) {
                set.add(e.getEmprunteur());
            }
        }
        return (set.size() * 100.0) / totalAdherents;
    }

    // =========================================================
    // Utilitaires threading
    // =========================================================

    private void runSafely(Runnable op) {
        setLoading(true);
        setError("");
        CompletableFuture.runAsync(op).whenComplete((res, ex) -> {
            if (ex != null) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                setError(cause.getMessage() != null ? cause.getMessage() : cause.toString());
            }
            setLoading(false);
        });
    }

    private void setLoading(boolean v) {
        if (Platform.isFxApplicationThread()) {
            loading.set(v);
        } else {
            Platform.runLater(() -> loading.set(v));
        }
    }

    private void setError(String msg) {
        if (Platform.isFxApplicationThread()) {
            error.set(msg);
        } else {
            Platform.runLater(() -> error.set(msg));
        }
    }

    // =========================================================
    // Getters pour la vue
    // =========================================================

    public ReadOnlyIntegerProperty totalDocumentsProperty()      { return totalDocuments; }
    public ReadOnlyIntegerProperty totalAdherentsProperty()      { return totalAdherents; }
    public ReadOnlyIntegerProperty totalEmpruntsProperty()       { return totalEmprunts; }
    public ReadOnlyDoubleProperty penalitesEnCoursProperty()     { return penalitesEnCours; }
    public ReadOnlyIntegerProperty totalRetardsProperty()        { return totalRetards; }
    public ReadOnlyDoubleProperty tauxRotationProperty()         { return tauxRotation; }
    public ReadOnlyDoubleProperty tauxOccupationProperty()       { return tauxOccupation; }

    public ReadOnlyDoubleProperty tauxUtilisationFondsProperty() { return tauxUtilisationFonds; }
    public ReadOnlyDoubleProperty tauxReempruntProperty()        { return tauxReemprunt; }
    public ReadOnlyDoubleProperty dureeMoyenneEmpruntsProperty() { return dureeMoyenneEmprunts; }
    public ReadOnlyDoubleProperty tauxRetardProperty()           { return tauxRetard; }
    public ReadOnlyDoubleProperty tauxEngagementAdherentsProperty() { return tauxEngagementAdherents; }

    public BooleanProperty loadingProperty() { return loading; }
    public StringProperty errorProperty()    { return error; }
    public ObjectProperty<LocalDateTime> lastUpdatedProperty() { return lastUpdated; }

    public XYChart.Series<String, Number> loansByMonthSeries() { return loansByMonth; }
    public ObservableList<PieChart.Data> docsByGenre()         { return docsByGenre; }
    public XYChart.Series<String, Number> docsByTypeSeries()   { return docsByType; }

    public ObservableList<RecentLoan> recentLoans()            { return recentLoans; }
    public ObservableList<ActiveBorrower> getActiveBorrowers() { return activeBorrowers; }
    public ObservableList<OverdueItem> getOverdueItems()       { return overdueItems; }

    public String getUsername() { return username; }
    public String getRole()     { return role; }
}