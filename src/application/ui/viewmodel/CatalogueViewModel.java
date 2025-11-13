package application.ui.viewmodel;

import application.domain.core.document.*;
import application.domain.ports.external.BookInfoProvider;
import application.domain.ports.external.BookMeta;
import application.domain.service.CatalogueService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.nio.file.Paths;    // <-- Ajouté
import java.util.List;
import java.util.Objects;     // <-- Ajouté (pour Objects.requireNonNull)
import java.util.concurrent.CompletableFuture;

/**
 * ViewModel Catalogue : documents + recherche + filtres + CRUD (scalable).
 * Gère l'état de l'UI du catalogue, la recherche (locale et API), les opérations CRUD,
 * et la gestion des couvertures (URL et fichier local).
 */
public class CatalogueViewModel {

    // ===== Services =====
    private final CatalogueService catalogueService;
    private final BookInfoProvider bookProvider;

    // ===== État UI =====
    private final ObservableList<Document> documents = FXCollections.observableArrayList();
    private final StringProperty query = new SimpleStringProperty("");
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty("");
    private final ObjectProperty<Document> selected = new SimpleObjectProperty<>(null);

    // ===== Callback API =====
    @FunctionalInterface
    public interface BookInfoCallback {
        void onResult(boolean success, BookMeta meta, String errorMsg);
    }

    // ===== Filtres =====
    public static final class FilterParams {
        public enum Type { TOUS, LIVRE, MAGAZINE, CD }
        private Type type = Type.TOUS;
        private String genre;
        private String editeur;
        private Integer anneeMin;
        private Integer anneeMax;
        private boolean disponibleUniquement;
        // Getters/Setters
        public Type getType() { return type; }
        public void setType(Type type) { this.type = (type == null ? Type.TOUS : type); }
        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }
        public String getEditeur() { return editeur; }
        public void setEditeur(String editeur) { this.editeur = editeur; }
        public Integer getAnneeMin() { return anneeMin; }
        public void setAnneeMin(Integer anneeMin) { this.anneeMin = anneeMin; }
        public Integer getAnneeMax() { return anneeMax; }
        public void setAnneeMax(Integer anneeMax) { this.anneeMax = anneeMax; }
        public boolean isDisponibleUniquement() { return disponibleUniquement; }
        public void setDisponibleUniquement(boolean d) { this.disponibleUniquement = d; }
    }
    private final ObjectProperty<FilterParams> filters = new SimpleObjectProperty<>(new FilterParams());
    public ObjectProperty<FilterParams> filtersProperty() { return filters; }
    public FilterParams getFilters() { return filters.get(); }
    public void setFilters(FilterParams f) { filters.set(f != null ? f : new FilterParams()); }

    // ===== Constructeurs =====
    public CatalogueViewModel(CatalogueService service, BookInfoProvider bookProvider) {
        // Utilise Objects.requireNonNull pour échouer tôt si une dépendance manque
        this.catalogueService = Objects.requireNonNull(service, "CatalogueService ne peut être null");
        this.bookProvider = bookProvider; // Peut être null si non configuré
    }
    // Supprimé les constructeurs fallback pour forcer l'injection correcte

    // ===== Recherche API =====
    public void fetchBookInfoByIsbn(String isbn, BookInfoCallback callback) {
        if (bookProvider == null) {
            Platform.runLater(() -> callback.onResult(false, null, "Le service de recherche API n'est pas configuré."));
            return;
        }
        setLoading(true);
        setError(""); // Efface les erreurs précédentes
        CompletableFuture.supplyAsync(() -> {
            try {
                // S'assure que findBookByIsbn renvoie Optional et gère les erreurs internes
                return bookProvider.findBookByIsbn(isbn);
            } catch (Exception e) {
                 // Capture les exceptions non gérées par le provider
                 throw new RuntimeException("Erreur lors de l'appel API: " + e.getMessage(), e);
            }
        }).whenComplete((resultOptional, error) -> {
            setLoading(false);
            Platform.runLater(() -> { // Assure que le callback est sur le thread FX
                if (error != null) {
                    String msg = error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
                    setError("Erreur API: " + msg); // Affiche l'erreur dans l'UI
                    callback.onResult(false, null, msg);
                } else if (resultOptional.isPresent()) {
                    callback.onResult(true, resultOptional.get(), null);
                } else {
                    // Pas une erreur, juste pas de résultat
                    callback.onResult(false, null, "Aucun document trouvé pour cet ISBN.");
                }
            });
        });
    }

    // ===== Chargement / Recherche BDD =====
    public void loadAllDocuments() { /* ... (code existant) ... */ runSafely(() -> { List<Document> all = catalogueService.findAll(); setDocuments(all != null ? all : List.of()); }); }
    public void refresh() { loadAllDocuments(); }
    public void search() { /* ... (code existant) ... */ String q = getQuery(); if (q == null || q.isBlank()) { loadAllDocuments(); } else { search(q); } }
    public void search(String q) { /* ... (code existant) ... */ final String trimmed = (q == null) ? "" : q.trim(); if (trimmed.isBlank()) { loadAllDocuments(); return; } runSafely(() -> { List<Document> found = catalogueService.search(trimmed); setDocuments(found != null ? found : List.of()); }); }
    public void clearSearch() { query.set(""); loadAllDocuments(); }
    public void searchWithFilters() { /* ... (code existant) ... */ runSafely(() -> { final String q = (getQuery() == null ? "" : getQuery().trim()); List<Document> base = q.isEmpty() ? catalogueService.findAll() : catalogueService.search(q); List<Document> filtered = applyInMemoryFilters(base, getFilters()); setDocuments(filtered); }); }
    public void refreshWithFilters() { searchWithFilters(); }
    private static List<Document> applyInMemoryFilters(List<Document> src, FilterParams fp) { /* ... (code existant) ... */ if (src == null) return List.of(); final String filterGenre = (fp.getGenre() == null ? "" : fp.getGenre().trim().toLowerCase()); final String filterEditeur = (fp.getEditeur() == null ? "" : fp.getEditeur().trim().toLowerCase()); return src.stream() .filter(d -> { if (fp.getType() == FilterParams.Type.TOUS) return true; String dtype = d.getDtype() != null ? d.getDtype() : ""; return switch (fp.getType()) { case LIVRE -> "Livre".equalsIgnoreCase(dtype); case MAGAZINE -> "Magazine".equalsIgnoreCase(dtype); case CD -> "CD".equalsIgnoreCase(dtype); default -> true; }; }) .filter(d -> { if (filterGenre.isEmpty()) return true; String g = d.getGenre(); return g != null && g.toLowerCase().contains(filterGenre); }) .filter(d -> { if (filterEditeur.isEmpty()) return true; String e = d.getEditeur(); return e != null && e.toLowerCase().contains(filterEditeur); }) .filter(d -> { Integer y = d.getAnneePublication(); if (y == null) { return fp.getAnneeMin() == null && fp.getAnneeMax() == null; } if (fp.getAnneeMin() != null && y < fp.getAnneeMin()) return false; if (fp.getAnneeMax() != null && y > fp.getAnneeMax()) return false; return true; }) .toList(); }

    // ===== CRUD =====

    /** Crée un document (potentiellement avec couverture URL). */
    public void create(Document doc, String coverUrlToDownload) {
        if (doc == null) return;
        runSafely(() -> {
            catalogueService.create(doc, coverUrlToDownload);
            refreshListAfterCrud(); // Rafraîchit la liste affichée
        });
    }

    /** Met à jour un document (potentiellement avec couverture URL). */
    public void update(Document doc, String coverUrlToDownload) {
        if (doc == null || doc.getId() == null) return;
        runSafely(() -> {
            catalogueService.update(doc, coverUrlToDownload);
            refreshListAfterCrud();
        });
    }

    /** Crée un document avec couverture depuis un fichier local. */
    public void createWithLocalCover(Document doc, String localFilePath) {
        if (doc == null || localFilePath == null || localFilePath.isBlank()) return;
        runSafely(() -> {
            try {
                // Convertit String en Path et appelle le service
                catalogueService.createWithLocalCover(doc, Paths.get(localFilePath));
            } catch (Exception e) { // Attrape IOException et autres erreurs potentielles
                // Affiche l'erreur dans l'UI via la propriété 'error'
                setError("Erreur sauvegarde couverture: " + e.getMessage());
            } finally {
                refreshListAfterCrud(); // Rafraîchit même en cas d'erreur de couverture
            }
        });
    }

    /** Met à jour un document avec couverture depuis un fichier local. */
    public void updateWithLocalCover(Document doc, String localFilePath) {
        if (doc == null || doc.getId() == null || localFilePath == null || localFilePath.isBlank()) return;
        runSafely(() -> {
             try {
                catalogueService.updateWithLocalCover(doc, Paths.get(localFilePath));
            } catch (Exception e) {
                setError("Erreur MAJ couverture: " + e.getMessage());
            } finally {
                refreshListAfterCrud();
            }
        });
    }

    // --- Surcharges (compatibilité) ---
    // Ces méthodes ne sont plus strictement nécessaires si tout le code utilise les nouvelles
    public void create(Document doc) { create(doc, null); }
    public void update(Document doc) { update(doc, null); }

    // --- Suppression ---
    public boolean canDelete(long id) { /* ... (code existant) ... */ try { return catalogueService.canDelete(id); } catch (Exception ex) { String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString(); setError(msg); return false; } }
    public void delete(long id) { /* ... (code existant) ... */ runSafely(() -> { catalogueService.delete(id); refreshListAfterCrud(); }); }

    // ===== Utilitaires =====
    /** Rafraîchit la liste de documents affichée après une opération CRUD. */
    private void refreshListAfterCrud() {
        // Recharge tout pour être sûr d'avoir les dernières données
        List<Document> all = catalogueService.findAll();
        setDocuments(all != null ? all : List.of());
    }

    /** Exécute une opération sur un thread d'arrière-plan. */
    private void runSafely(Runnable op) {
        setLoading(true);
        setError(""); // Efface les erreurs précédentes
        CompletableFuture.runAsync(op) // Exécute l'opération (qui appelle le service)
            .whenComplete((res, ex) -> {
                // S'exécute à la fin (succès ou échec) sur le thread worker
                Platform.runLater(() -> { // Revient sur le thread FX pour màj UI
                    if (ex != null) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        String msg = cause.getMessage() != null ? cause.getMessage() : cause.toString();
                        // Affiche l'erreur dans l'UI
                        setError("Opération échouée: " + msg);
                        // Optionnel : logger l'exception complète
                        // cause.printStackTrace();
                    }
                    setLoading(false); // Arrête l'indicateur de chargement
                });
            });
    }

    // --- Helpers thread-safe pour màj UI ---
    private void setLoading(boolean v) { Platform.runLater(() -> loading.set(v)); }
    private void setError(String msg) { Platform.runLater(() -> error.set(msg == null ? "" : msg)); }
    private void setDocuments(List<Document> items) { Platform.runLater(() -> documents.setAll(items == null ? List.of() : items)); }

    // ===== Propriétés Exposées =====
    public ObservableList<Document> getDocuments() { return documents; }
    public StringProperty queryProperty() { return query; }
    public String getQuery() { return query.get(); }
    public BooleanProperty loadingProperty() { return loading; }
    public StringProperty errorProperty() { return error; }
    public ObjectProperty<Document> selectedProperty() { return selected; }
    public Document getSelected() { return selected.get(); }
}

