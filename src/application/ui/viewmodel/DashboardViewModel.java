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
 * ViewModel du Dashboard.
 *
 * Rôle :
 * - centraliser le calcul des KPI et données de graphiques,
 * - exposer des propriétés JavaFX pour la vue,
 * - exécuter les chargements en asynchrone (runSafely + CompletableFuture).
 */
public class DashboardViewModel {

    // =========================================================
    // Dépendances
    // =========================================================

    private final DashboardService dashboardService;
    private final DocumentRepository documentRepository;
    private final AdherentRepository adherentRepository;  // potentiellement utile plus tard
    private final EmpruntRepository empruntRepository;
    private final PenaliteRepository penaliteRepository;   // potentiellement utile plus tard

    private final String username;
    private final String role;

    // =========================================================
    // KPIs simples
    // =========================================================

    private final IntegerProperty totalDocuments  = new SimpleIntegerProperty(0);
    private final IntegerProperty totalAdherents  = new SimpleIntegerProperty(0);
    private final IntegerProperty totalEmprunts   = new SimpleIntegerProperty(0);
    private final DoubleProperty penalitesEnCours = new SimpleDoubleProperty(0.0);
    private final IntegerProperty totalRetards    = new SimpleIntegerProperty(0);
    private final DoubleProperty tauxRotation     = new SimpleDoubleProperty(0.0);
    private final DoubleProperty tauxOccupation   = new SimpleDoubleProperty(0.0);

    // =========================================================
    // KPIs avancés
    // =========================================================

    private final DoubleProperty tauxUtilisationFonds    = new SimpleDoubleProperty(0.0);
    private final DoubleProperty tauxReemprunt           = new SimpleDoubleProperty(0.0);
    private final DoubleProperty dureeMoyenneEmprunts    = new SimpleDoubleProperty(0.0);
    private final DoubleProperty tauxRetard              = new SimpleDoubleProperty(0.0);
    private final DoubleProperty tauxEngagementAdherents = new SimpleDoubleProperty(0.0);

    // =========================================================
    // État UI
    // =========================================================

    private final BooleanProperty loading       = new SimpleBooleanProperty(false);
    private final StringProperty  error         = new SimpleStringProperty("");
    private final ObjectProperty<LocalDateTime> lastUpdated =
            new SimpleObjectProperty<>(null);

    // =========================================================
    // Graphiques
    // =========================================================

    // Évolution des emprunts par mois
    private final XYChart.Series<String, Number> loansByMonth = new XYChart.Series<>();

    // Répartition par genre (support PieChart, même si tu n’affiches plus de camembert,
    // ça reste utile pour la logique de regroupement)
    private final ObservableList<PieChart.Data> docsByGenre =
            FXCollections.observableArrayList();

    // Documents par type (BarChart)
    private final XYChart.Series<String, Number> docsByType = new XYChart.Series<>();

    // Top 10 genres les plus empruntés (BarChart)
    private final XYChart.Series<String, Number> docsByGenreBar = new XYChart.Series<>();

    // =========================================================
    // Listes dynamiques affichées dans la vue
    // =========================================================

    public record RecentLoan(String title, String borrower, String when, String coverRef) {}
    private final ObservableList<RecentLoan> recentLoans =
            FXCollections.observableArrayList();

    public record ActiveBorrower(String name, int loanCount) {}
    private final ObservableList<ActiveBorrower> activeBorrowers =
            FXCollections.observableArrayList();

    public record OverdueItem(String title, String borrower, int daysOverdue, double penalty) {}
    private final ObservableList<OverdueItem> overdueItems =
            FXCollections.observableArrayList();

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
        this.username           = Objects.requireNonNull(username);
        this.role               = Objects.requireNonNull(role);

        loansByMonth.setName("Emprunts / mois");
        docsByType.setName("Documents par type");
        docsByGenreBar.setName("Top 10 genres");

        // Chargement initial
        refresh();
    }

    // =========================================================
    // Rafraîchissement global
    // =========================================================

    /**
     * Rafraîchit tous les KPI et données graphiques en asynchrone.
     */
    public void refresh() {
        runSafely(() -> {

            LocalDateTime now  = LocalDateTime.now();
            LocalDate today    = LocalDate.now();

            // --- Récupération des compteurs de base via le service ---
            int docs      = dashboardService.countDocuments();
            int adherents = dashboardService.countAdherents();
            int emprunts  = dashboardService.countEmprunts();
            double penal  = dashboardService.sumPenalites();
            int retards   = dashboardService.countRetards();

            // --- Chargement des entités nécessaires pour les métriques ---
            List<Document> allDocuments   = documentRepository.findAll();
            List<Emprunt>  allEmprunts    = empruntRepository.findAll();
            List<Emprunt>  activeEmprunts = empruntRepository.findActiveEmprunts();
            List<Emprunt>  lateEmprunts   = empruntRepository.findLateEmprunts(today);

            // --- KPIs simples ---
            double rotation   = (docs > 0) ? (emprunts * 12.0) / docs : 0.0;
            double occupation = (docs > 0) ? (emprunts * 100.0) / docs : 0.0;

            // --- Données pour les graphiques ---
            List<XYChart.Data<String, Number>> loansData    = computeLoansByMonth(allEmprunts, today);
            List<PieChart.Data>                genreData    = computeDocsByGenre(allDocuments);
            List<XYChart.Data<String, Number>> docTypeData  = computeDocsByType(allDocuments);
            List<XYChart.Data<String, Number>> genreBarData = computeDocsByGenreBarData(allDocuments);

            Platform.runLater(() -> {
                docsByGenreBar.getData().setAll(genreBarData);
            });
            // --- Listes dynamiques (derniers emprunts, top adhérents, retards) ---
            List<RecentLoan>     recentData  = computeRecentLoans(allEmprunts);
            List<ActiveBorrower> activeData = computeActiveBorrowers(allEmprunts, today);
            List<OverdueItem>    overdueData = computeOverdueItems(lateEmprunts, today);

            // --- KPIs avancés ---
            double kpiUtilisationFonds = computeTauxUtilisationFonds(allDocuments, allEmprunts);
            double kpiReemprunt        = computeTauxReemprunt(allEmprunts);
            double kpiDureeMoyenne     = computeDureeMoyenne(allEmprunts);
            double kpiTauxRetard       = (emprunts > 0) ? (retards * 100.0) / emprunts : 0.0;
            double kpiEngagementAdh    = computeTauxEngagementAdherents(adherents, allEmprunts);

            // --- Mise à jour des propriétés sur le thread JavaFX ---
            Platform.runLater(() -> {
                // KPIs simples
                totalDocuments.set(docs);
                totalAdherents.set(adherents);
                totalEmprunts.set(emprunts);
                penalitesEnCours.set(penal);
                totalRetards.set(retards);
                tauxRotation.set(rotation);
                tauxOccupation.set(occupation);
                lastUpdated.set(now);

                // KPIs avancés
                tauxUtilisationFonds.set(kpiUtilisationFonds);
                tauxReemprunt.set(kpiReemprunt);
                dureeMoyenneEmprunts.set(kpiDureeMoyenne);
                tauxRetard.set(kpiTauxRetard);
                tauxEngagementAdherents.set(kpiEngagementAdh);

                // Graphiques
                loansByMonth.getData().setAll(loansData);
                docsByGenre.setAll(genreData);
                docsByType.getData().setAll(docTypeData);
                docsByGenreBar.getData().setAll(genreBarData);

                // Listes
                recentLoans.setAll(recentData);
                activeBorrowers.setAll(activeData);
                overdueItems.setAll(overdueData);
            });
        });
    }

    // =========================================================
    // Méthodes privées : calculs
    // =========================================================

    private List<XYChart.Data<String, Number>> computeLoansByMonth(List<Emprunt> allEmprunts,
                                                                   LocalDate today) {
        Map<String, Integer> countByMonth = new LinkedHashMap<>();

        for (Emprunt e : allEmprunts) {
            if (e.getDateEmprunt() == null) continue;
            if (e.getDateEmprunt().getYear() != today.getYear()) continue;

            String month = e.getDateEmprunt().getMonth().toString(); // ex: JANUARY
            countByMonth.merge(month, 1, Integer::sum);
        }

        List<XYChart.Data<String, Number>> data = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countByMonth.entrySet()) {
            String label = entry.getKey().substring(0, 3); // "JAN", "FEB", etc.
            data.add(new XYChart.Data<>(label, entry.getValue()));
        }
        return data;
    }

    private List<PieChart.Data> computeDocsByGenre(List<Document> allDocuments) {
        Map<String, Integer> countByGenre = new LinkedHashMap<>();

        for (Document d : allDocuments) {
            String genre = d.getGenre();
            if (genre == null || genre.isBlank()) {
                genre = "Inconnu";
            }
            countByGenre.merge(genre, 1, Integer::sum);
        }

        List<PieChart.Data> data = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countByGenre.entrySet()) {
            data.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
        return data;
    }

    private List<XYChart.Data<String, Number>> computeDocsByType(List<Document> allDocuments) {
        Map<String, Integer> countByType = new LinkedHashMap<>();

        for (Document d : allDocuments) {
            String type = d.getDtype();
            if (type == null || type.isBlank()) {
                type = "Autre";
            }
            countByType.merge(type, 1, Integer::sum);
        }

        List<XYChart.Data<String, Number>> data = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countByType.entrySet()) {
            data.add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        return data;
    }

    /**
     * Construit les données pour le bar chart "Top 10 genres".
     * On part des {@link PieChart.Data} pour éviter de dupliquer la logique
     * de comptage des genres.
     */
    /**
     * Construit la série "Top 10 genres les plus présents au catalogue".
     */
    private List<XYChart.Data<String, Number>> computeDocsByGenreBarData(List<Document> allDocuments) {

        // 1. Compter par genre
        Map<String, Integer> countByGenre = new HashMap<>();

        for (Document d : allDocuments) {
            String genre = (d.getGenre() == null || d.getGenre().isBlank())
                    ? "Inconnu"
                    : d.getGenre();
            countByGenre.merge(genre, 1, Integer::sum);
        }

        // 2. Trier décroissant et garder TOP 10
        return countByGenre.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(e -> new XYChart.Data<String, Number>(e.getKey(), e.getValue()))
                .toList();
    }

    private List<RecentLoan> computeRecentLoans(List<Emprunt> allEmprunts) {
        List<Emprunt> copy = new ArrayList<>(allEmprunts);

        // On retire les emprunts incomplets
        copy.removeIf(e -> e.getDateEmprunt() == null ||
                           e.getDocument() == null ||
                           e.getEmprunteur() == null);

        // Plus récents d'abord
        copy.sort((e1, e2) -> e2.getDateEmprunt().compareTo(e1.getDateEmprunt()));

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

    private List<ActiveBorrower> computeActiveBorrowers(List<Emprunt> allEmprunts, LocalDate today) {
        LocalDate from = today.minusDays(30); // fenêtre des 30 jours

        // idAdherent -> nb emprunts
        Map<Long, Integer> countByAdherentId = new HashMap<>();
        // idAdherent -> nom (pour affichage)
        Map<Long, String> nameById = new HashMap<>();

        for (Emprunt e : allEmprunts) {
            if (e.getDateEmprunt() == null) continue;
            if (e.getDateEmprunt().isBefore(from)) continue; // on ignore les emprunts trop anciens

            Adherent adherent = e.getEmprunteur();
            if (adherent == null || adherent.getId() == null) continue;

            Long id = adherent.getId();

            // Compte + nom
            countByAdherentId.merge(id, 1, Integer::sum);
            nameById.putIfAbsent(id, adherent.getNomComplet());
        }

        // tri décroissant, limite 10
        return countByAdherentId.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .map(e -> new ActiveBorrower(
                        nameById.get(e.getKey()),
                        e.getValue()
                ))
                .toList();
    }
    private List<OverdueItem> computeOverdueItems(List<Emprunt> lateEmprunts, LocalDate today) {
        List<OverdueItem> result = new ArrayList<>();

        for (Emprunt e : lateEmprunts) {
            if (e.getDocument() == null || e.getEmprunteur() == null) continue;

            int jours   = (int) e.joursDeRetard(today);
            double pen  = e.penaliteCourante(today).amount().doubleValue();

            result.add(new OverdueItem(
                    e.getDocument().getTitre(),
                    e.getEmprunteur().getNomComplet(),
                    jours,
                    pen
            ));
        }
        return result;
    }

    // =========================================================
    // KPIs avancés
    // =========================================================

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
        Map<Document, Integer> countByDoc = new HashMap<>();

        for (Emprunt e : allEmprunts) {
            Document d = e.getDocument();
            if (d == null) continue;
            countByDoc.merge(d, 1, Integer::sum);
        }

        if (countByDoc.isEmpty()) return 0.0;

        int docsTotal = countByDoc.size();
        int docsReemprunt = 0;
        for (int count : countByDoc.values()) {
            if (count >= 2) docsReemprunt++;
        }

        return (docsReemprunt * 100.0) / docsTotal;
    }

    private double computeDureeMoyenne(List<Emprunt> allEmprunts) {
        long totalJours = 0;
        int count       = 0;

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

    private double computeTauxEngagementAdherents(int totalAdherents,
                                                  List<Emprunt> allEmprunts) {
        if (totalAdherents == 0) return 0.0;

        Set<Adherent> distinctAdherents = new HashSet<>();
        for (Emprunt e : allEmprunts) {
            if (e.getEmprunteur() != null) {
                distinctAdherents.add(e.getEmprunteur());
            }
        }
        return (distinctAdherents.size() * 100.0) / totalAdherents;
    }

    // =========================================================
    // Utilitaires threading
    // =========================================================

    private void runSafely(Runnable op) {
        setLoading(true);
        setError("");
        CompletableFuture.runAsync(op).whenComplete((res, ex) -> {
            if (ex != null) {
                Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
                String msg = (cause.getMessage() != null) ? cause.getMessage() : cause.toString();
                setError(msg);
            }
            setLoading(false);
        });
    }

    private void setLoading(boolean value) {
        if (Platform.isFxApplicationThread()) {
            loading.set(value);
        } else {
            Platform.runLater(() -> loading.set(value));
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
    // Getters exposés à la vue
    // =========================================================

    public ReadOnlyIntegerProperty totalDocumentsProperty()           { return totalDocuments; }
    public ReadOnlyIntegerProperty totalAdherentsProperty()           { return totalAdherents; }
    public ReadOnlyIntegerProperty totalEmpruntsProperty()            { return totalEmprunts; }
    public ReadOnlyDoubleProperty penalitesEnCoursProperty()          { return penalitesEnCours; }
    public ReadOnlyIntegerProperty totalRetardsProperty()             { return totalRetards; }
    public ReadOnlyDoubleProperty tauxRotationProperty()              { return tauxRotation; }
    public ReadOnlyDoubleProperty tauxOccupationProperty()            { return tauxOccupation; }

    public ReadOnlyDoubleProperty tauxUtilisationFondsProperty()      { return tauxUtilisationFonds; }
    public ReadOnlyDoubleProperty tauxReempruntProperty()             { return tauxReemprunt; }
    public ReadOnlyDoubleProperty dureeMoyenneEmpruntsProperty()      { return dureeMoyenneEmprunts; }
    public ReadOnlyDoubleProperty tauxRetardProperty()                { return tauxRetard; }
    public ReadOnlyDoubleProperty tauxEngagementAdherentsProperty()   { return tauxEngagementAdherents; }

    public BooleanProperty          loadingProperty()      { return loading; }
    public StringProperty           errorProperty()        { return error; }
    public ObjectProperty<LocalDateTime> lastUpdatedProperty() { return lastUpdated; }

    public XYChart.Series<String, Number> loansByMonthSeries()  { return loansByMonth; }
    public ObservableList<PieChart.Data>  docsByGenre()         { return docsByGenre; }
    public XYChart.Series<String, Number> docsByTypeSeries()    { return docsByType; }

    /** Série utilisée par le BarChart "Top 10 genres" dans le Dashboard. */
    public XYChart.Series<String, Number> docsByGenreSeries()   { return docsByGenreBar; }

    public ObservableList<RecentLoan>      recentLoans()        { return recentLoans; }
    public ObservableList<ActiveBorrower>  getActiveBorrowers() { return activeBorrowers; }
    public ObservableList<OverdueItem>     getOverdueItems()    { return overdueItems; }

    public String getUsername() { return username; }
    public String getRole()     { return role; }
}