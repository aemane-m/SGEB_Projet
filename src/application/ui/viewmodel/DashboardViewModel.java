package application.ui.viewmodel;

import application.domain.core.document.Document;
import application.domain.core.emprunt.Emprunt;
import application.domain.ports.repository.DocumentRepository;
import application.domain.ports.repository.EmpruntRepository;
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
 * Version Corrigée : Prend en compte le filtre de période pour les graphiques.
 */
public final class DashboardViewModel {

    // =========================================================
    // Dépendances
    // =========================================================
    private final DashboardService dashboardService;
    private final DocumentRepository documentRepository;
    private final EmpruntRepository empruntRepository;

    private final String username;
    private final String role;

    // =========================================================
    // KPIs (Propriétés pour la vue)
    // =========================================================
    private final IntegerProperty totalDocuments  = new SimpleIntegerProperty(0);
    private final IntegerProperty totalAdherents  = new SimpleIntegerProperty(0);
    private final IntegerProperty totalEmprunts   = new SimpleIntegerProperty(0); // Emprunts sur la période
    private final DoubleProperty penalitesEnCours = new SimpleDoubleProperty(0.0);
    private final IntegerProperty totalRetards    = new SimpleIntegerProperty(0);
    private final DoubleProperty tauxRotation     = new SimpleDoubleProperty(0.0);
    private final DoubleProperty tauxOccupation   = new SimpleDoubleProperty(0.0);

    // KPIs avancés
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
    private final ObjectProperty<LocalDateTime> lastUpdated = new SimpleObjectProperty<>(null);

    // =========================================================
    // Données Graphiques
    // =========================================================
    // Évolution des emprunts (Filtré)
    private final XYChart.Series<String, Number> loansByMonth = new XYChart.Series<>();

    // Répartition du STOCK par genre (Global)
    private final ObservableList<PieChart.Data> docsByGenre = FXCollections.observableArrayList();

    // Documents par type (Global)
    private final XYChart.Series<String, Number> docsByType = new XYChart.Series<>();

    // Top 10 genres les plus EMPRUNTÉS (Filtré sur la période)
    private final XYChart.Series<String, Number> docsByGenreBar = new XYChart.Series<>();

    // =========================================================
    // Listes
    // =========================================================
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
                              EmpruntRepository empruntRepository,
                              String username,
                              String role) {

        this.dashboardService   = Objects.requireNonNull(dashboardService);
        this.documentRepository = Objects.requireNonNull(documentRepository);
        this.empruntRepository  = Objects.requireNonNull(empruntRepository);
        this.username = username;
        this.role     = role;

        loansByMonth.setName("Emprunts / mois");
        docsByType.setName("Documents par type");
        docsByGenreBar.setName("Top 10 Genres Empruntés");

        // Chargement initial (par défaut sur 365 jours ou "Tout")
        refresh(365);
    }

    // =========================================================
    // Rafraîchissement avec Filtre
    // =========================================================

    /**
     * Rafraîchit les données selon la période demandée.
     * @param joursHistorique Nombre de jours en arrière (ex: 7, 30, 90).
     */
    public void refresh(int joursHistorique) {
        runSafely(() -> {
            LocalDateTime now  = LocalDateTime.now();
            LocalDate today    = LocalDate.now();
            // Calcul de la date limite (Aujourd'hui - X jours)
            LocalDate dateLimite = today.minusDays(joursHistorique);

            // 1. Récupération des données BRUTES (Tout)
            List<Document> allDocuments = documentRepository.findAll();
            List<Emprunt>  allEmprunts  = empruntRepository.findAll();
            List<Emprunt>  lateEmprunts = empruntRepository.findLateEmprunts(today);

            // --- CORRECTION MAJEURE : Création du Dictionnaire des Documents ---
            // Permet de retrouver le Genre même si l'objet Emprunt est incomplet
            Map<Long, Document> dictionnaireDocuments = new HashMap<>();
            for (Document d : allDocuments) {
                if (d.getId() != null) {
                    dictionnaireDocuments.put(d.getId(), d);
                }
            }

            // 2. FILTRAGE MANUEL : On ne garde que les emprunts de la période
            List<Emprunt> empruntsFiltres = new ArrayList<>();
            for (Emprunt e : allEmprunts) {
                if (e.getDateEmprunt() != null &&
                   (e.getDateEmprunt().isAfter(dateLimite) || e.getDateEmprunt().isEqual(dateLimite))) {
                    empruntsFiltres.add(e);
                }
            }

            // --- Calcul des KPIs ---
            int docsCount      = allDocuments.size();
            int adherentsCount = dashboardService.countAdherents();
            int empruntsCount  = empruntsFiltres.size(); // Volume sur la période
            double penalites   = dashboardService.sumPenalites();
            int retardsCount   = dashboardService.countRetards();

            double rotation   = (docsCount > 0) ? (empruntsCount * 12.0) / docsCount : 0.0;
            double occupation = (docsCount > 0) ? (dashboardService.countEmprunts() * 100.0) / docsCount : 0.0;

            // --- Calculs Graphiques (Avec la liste FILTRÉE) ---
            
            // CORRECTION : Calcul manquant ajouté ici
            var graphEmpruntsMois = computeLoansByMonth(empruntsFiltres); 
            
            // CORRECTION : Utilisation de la méthode avec le dictionnaire
            var graphTopGenres    = computeTopGenresEmpruntes(empruntsFiltres, dictionnaireDocuments); 
            
            var listeActiveUsers  = computeActiveBorrowers(empruntsFiltres);

            // --- Calculs Graphiques (Données Globales de stock) ---
            var graphPieStock     = computeDocsByGenreStock(allDocuments);
            var graphTypeStock    = computeDocsByType(allDocuments);

            // --- Autres listes ---
            var listeRecents = computeRecentLoans(empruntsFiltres);
            var listeRetards = computeOverdueItems(lateEmprunts, today);

            // --- KPIs Avancés (Mixte) ---
            double kpiUtilisation = computeTauxUtilisationFonds(allDocuments, empruntsFiltres);
            double kpiReemprunt   = computeTauxReemprunt(empruntsFiltres);
            double kpiDureeMoy    = computeDureeMoyenne(empruntsFiltres);
            double kpiTauxRetard  = (empruntsCount > 0) ? (retardsCount * 100.0) / empruntsCount : 0.0;
            double kpiEngagement  = computeTauxEngagementAdherents(adherentsCount, empruntsFiltres);

            // 3. Mise à jour de l'interface (Thread JavaFX)
            Platform.runLater(() -> {
                totalDocuments.set(docsCount);
                totalAdherents.set(adherentsCount);
                totalEmprunts.set(empruntsCount);
                penalitesEnCours.set(penalites);
                totalRetards.set(retardsCount);
                tauxRotation.set(rotation);
                tauxOccupation.set(occupation);
                lastUpdated.set(now);

                tauxUtilisationFonds.set(kpiUtilisation);
                tauxReemprunt.set(kpiReemprunt);
                dureeMoyenneEmprunts.set(kpiDureeMoy);
                tauxRetard.set(kpiTauxRetard);
                tauxEngagementAdherents.set(kpiEngagement);

                // Mise à jour des listes observables pour les graphes
                loansByMonth.getData().setAll(graphEmpruntsMois);
                docsByGenre.setAll(graphPieStock);
                docsByType.getData().setAll(graphTypeStock);
                docsByGenreBar.getData().setAll(graphTopGenres); 

                recentLoans.setAll(listeRecents);
                activeBorrowers.setAll(listeActiveUsers);
                overdueItems.setAll(listeRetards);
            });
        });
    }

    // =========================================================
    // Méthodes de Calcul (Simplifiées)
    // =========================================================

    /**
     * Top 10 des genres les plus empruntés (sur la liste filtrée).
     */
    private List<XYChart.Data<String, Number>> computeTopGenresEmpruntes(List<Emprunt> emprunts, Map<Long, Document> docMap) {
        Map<String, Integer> compteur = new HashMap<>();

        for (Emprunt e : emprunts) {
            Long docId = (e.getDocument() != null) ? e.getDocument().getId() : null;
            
            // On cherche dans la Map complète pour avoir le vrai Genre
            if (docId != null && docMap.containsKey(docId)) {
                Document realDoc = docMap.get(docId);
                String genre = (realDoc.getGenre() == null || realDoc.getGenre().isBlank()) ? "Inconnu" : realDoc.getGenre();
                compteur.merge(genre, 1, Integer::sum);
            }
        }
        
        // ... Tri et conversion en XYChart.Data (code identique à avant) ...
        List<Map.Entry<String, Integer>> listeTriee = new ArrayList<>(compteur.entrySet());
        listeTriee.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<XYChart.Data<String, Number>> result = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, Integer> entry : listeTriee) {
            result.add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            count++;
            if (count >= 10) break;
        }
        return result;
    }

    private List<ActiveBorrower> computeActiveBorrowers(List<Emprunt> emprunts) {
        Map<Long, Integer> countById = new HashMap<>();
        Map<Long, String> nameById = new HashMap<>();

        for (Emprunt e : emprunts) {
            if (e.getEmprunteur() != null && e.getEmprunteur().getId() != null) {
                Long id = e.getEmprunteur().getId();
                countById.merge(id, 1, Integer::sum);
                nameById.putIfAbsent(id, e.getEmprunteur().getNomComplet());
            }
        }

        // Tri descendant
        return countById.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .map(e -> new ActiveBorrower(nameById.get(e.getKey()), e.getValue()))
                .toList();
    }

    private List<XYChart.Data<String, Number>> computeLoansByMonth(List<Emprunt> emprunts) {
        Map<String, Integer> countByMonth = new LinkedHashMap<>();

        // On regroupe par "Mois-Année" pour ne pas mélanger Janvier 2024 et Janvier 2025
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yy");

        for (Emprunt e : emprunts) {
            if (e.getDateEmprunt() != null) {
                String label = e.getDateEmprunt().format(fmt);
                countByMonth.merge(label, 1, Integer::sum);
            }
        }

        List<XYChart.Data<String, Number>> data = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countByMonth.entrySet()) {
            data.add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        // Petit tri chronologique simple (optionnel si LinkedHashMap ne suffit pas)
        data.sort(Comparator.comparing(d -> d.getXValue())); 
        
        return data;
    }

    // Pie Chart : Stock global (On garde allDocuments)
    private List<PieChart.Data> computeDocsByGenreStock(List<Document> documents) {
        Map<String, Integer> countByGenre = new HashMap<>();
        for (Document d : documents) {
            String g = (d.getGenre() == null || d.getGenre().isBlank()) ? "Inconnu" : d.getGenre();
            countByGenre.merge(g, 1, Integer::sum);
        }
        List<PieChart.Data> result = new ArrayList<>();
        for (var entry : countByGenre.entrySet()) {
            result.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private List<XYChart.Data<String, Number>> computeDocsByType(List<Document> documents) {
        Map<String, Integer> countByType = new HashMap<>();
        for (Document d : documents) {
            String t = (d.getDtype() == null) ? "Autre" : d.getDtype();
            countByType.merge(t, 1, Integer::sum);
        }
        List<XYChart.Data<String, Number>> result = new ArrayList<>();
        for (var entry : countByType.entrySet()) {
            result.add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private List<RecentLoan> computeRecentLoans(List<Emprunt> emprunts) {
        // Copie pour trier sans toucher à l'original
        List<Emprunt> copy = new ArrayList<>(emprunts);
        // Tri : date décroissante (plus récent en premier)
        copy.sort((e1, e2) -> e2.getDateEmprunt().compareTo(e1.getDateEmprunt()));

        List<RecentLoan> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);

        int max = Math.min(5, copy.size());
        for (int i = 0; i < max; i++) {
            Emprunt e = copy.get(i);
            if (e.getDocument() != null && e.getEmprunteur() != null) {
                result.add(new RecentLoan(
                        e.getDocument().getTitre(),
                        e.getEmprunteur().getNomComplet(),
                        e.getDateEmprunt().format(fmt),
                        e.getDocument().getCoverRef()
                ));
            }
        }
        return result;
    }

    private List<OverdueItem> computeOverdueItems(List<Emprunt> lateEmprunts, LocalDate today) {
        List<OverdueItem> list = new ArrayList<>();
        for (Emprunt e : lateEmprunts) {
            if (e.getDocument() != null && e.getEmprunteur() != null) {
                list.add(new OverdueItem(
                        e.getDocument().getTitre(),
                        e.getEmprunteur().getNomComplet(),
                        (int) e.joursDeRetard(today),
                        e.penaliteCourante(today).amount().doubleValue()
                ));
            }
        }
        return list;
    }

    // =========================================================
    // Calculs KPIs Avancés (Utilisant des boucles simples)
    // =========================================================

    private double computeTauxUtilisationFonds(List<Document> allDocs, List<Emprunt> empruntsFiltres) {
        if (allDocs.isEmpty()) return 0.0;
        
        Set<Long> idsLivresEmpruntes = new HashSet<>();
        for (Emprunt e : empruntsFiltres) {
            if (e.getDocument() != null) {
                idsLivresEmpruntes.add(e.getDocument().getId());
            }
        }
        return (idsLivresEmpruntes.size() * 100.0) / allDocs.size();
    }

    private double computeTauxReemprunt(List<Emprunt> empruntsFiltres) {
        if (empruntsFiltres.isEmpty()) return 0.0;
        
        Map<Long, Integer> countByDocId = new HashMap<>();
        for (Emprunt e : empruntsFiltres) {
            if (e.getDocument() != null) {
                countByDocId.merge(e.getDocument().getId(), 1, Integer::sum);
            }
        }
        
        int docsEmpruntesPlusDUneFois = 0;
        for (Integer count : countByDocId.values()) {
            if (count > 1) docsEmpruntesPlusDUneFois++;
        }
        
        return (docsEmpruntesPlusDUneFois * 100.0) / countByDocId.size();
    }

    private double computeDureeMoyenne(List<Emprunt> empruntsFiltres) {
        long totalJours = 0;
        int count = 0;
        for (Emprunt e : empruntsFiltres) {
            if (e.getDateRetourEffective() != null) {
                totalJours += ChronoUnit.DAYS.between(e.getDateEmprunt(), e.getDateRetourEffective());
                count++;
            }
        }
        return (count == 0) ? 0.0 : (double) totalJours / count;
    }

    private double computeTauxEngagementAdherents(int totalAdherents, List<Emprunt> empruntsFiltres) {
        if (totalAdherents == 0) return 0.0;
        
        Set<Long> idsAdherentsActifs = new HashSet<>();
        for (Emprunt e : empruntsFiltres) {
            if (e.getEmprunteur() != null) {
                idsAdherentsActifs.add(e.getEmprunteur().getId());
            }
        }
        return (idsAdherentsActifs.size() * 100.0) / totalAdherents;
    }

    // =========================================================
    // Utilitaires
    // =========================================================

    private void runSafely(Runnable op) {
        setLoading(true);
        setError("");
        CompletableFuture.runAsync(op).whenComplete((res, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
                setError(ex.getMessage());
            }
            setLoading(false);
        });
    }

    private void setLoading(boolean v) {
        if (Platform.isFxApplicationThread()) loading.set(v);
        else Platform.runLater(() -> loading.set(v));
    }

    private void setError(String v) {
        if (Platform.isFxApplicationThread()) error.set(v);
        else Platform.runLater(() -> error.set(v));
    }

    // =========================================================
    // Getters
    // =========================================================
    public ReadOnlyIntegerProperty totalDocumentsProperty() { return totalDocuments; }
    public ReadOnlyIntegerProperty totalAdherentsProperty() { return totalAdherents; }
    public ReadOnlyIntegerProperty totalEmpruntsProperty() { return totalEmprunts; }
    public ReadOnlyDoubleProperty penalitesEnCoursProperty() { return penalitesEnCours; }
    public ReadOnlyIntegerProperty totalRetardsProperty() { return totalRetards; }
    public ReadOnlyDoubleProperty tauxRotationProperty() { return tauxRotation; }
    public ReadOnlyDoubleProperty tauxOccupationProperty() { return tauxOccupation; }
    public ReadOnlyDoubleProperty tauxUtilisationFondsProperty() { return tauxUtilisationFonds; }
    public ReadOnlyDoubleProperty tauxReempruntProperty() { return tauxReemprunt; }
    public ReadOnlyDoubleProperty dureeMoyenneEmpruntsProperty() { return dureeMoyenneEmprunts; }
    public ReadOnlyDoubleProperty tauxRetardProperty() { return tauxRetard; }
    public ReadOnlyDoubleProperty tauxEngagementAdherentsProperty() { return tauxEngagementAdherents; }
    public BooleanProperty loadingProperty() { return loading; }
    public StringProperty errorProperty() { return error; }
    public ObjectProperty<LocalDateTime> lastUpdatedProperty() { return lastUpdated; }

    public XYChart.Series<String, Number> loansByMonthSeries() { return loansByMonth; }
    public ObservableList<PieChart.Data> docsByGenre() { return docsByGenre; }
    public XYChart.Series<String, Number> docsByTypeSeries() { return docsByType; }
    public XYChart.Series<String, Number> docsByGenreSeries() { return docsByGenreBar; }
    public ObservableList<RecentLoan> recentLoans() { return recentLoans; }
    public ObservableList<ActiveBorrower> getActiveBorrowers() { return activeBorrowers; }
    public ObservableList<OverdueItem> getOverdueItems() { return overdueItems; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
}