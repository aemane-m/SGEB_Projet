package application.ui.viewmodel;

import application.domain.core.document.Document;
import application.domain.ports.external.BookInfoProvider;
import application.domain.ports.external.BookMeta;
import application.domain.service.CatalogueService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * ViewModel Catalogue : documents + recherche + filtres + CRUD.
 *
 * - Gère l'état de l'UI du catalogue
 * - Recherche texte (BDD) + recherche externe par ISBN (API)
 * - Filtres en mémoire (type, genre, éditeur, années)
 * - CRUD Documents avec gestion de couverture (URL ou fichier local)
 */
public final class CatalogueViewModel {

    // =========================================================================
    // Services domaine
    // =========================================================================

    private final CatalogueService catalogueService;
    private final BookInfoProvider bookProvider; // peut être null

    // =========================================================================
    // État UI
    // =========================================================================

    private final ObservableList<Document> documents =
            FXCollections.observableArrayList();

    private final StringProperty query = new SimpleStringProperty("");
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty("");
    private final ObjectProperty<Document> selected =
            new SimpleObjectProperty<>(null);

    // =========================================================================
    // Callback API externe
    // =========================================================================

    @FunctionalInterface
    public interface BookInfoCallback {
        void onResult(boolean success, BookMeta meta, String errorMsg);
    }

    // =========================================================================
    // Filtres
    // =========================================================================

    public static final class FilterParams {

        public enum Type { TOUS, LIVRE, MAGAZINE, CD }

        private Type type = Type.TOUS;
        private String genre;
        private String editeur;
        private Integer anneeMin;
        private Integer anneeMax;
        private boolean disponibleUniquement;

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = (type == null ? Type.TOUS : type);
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }

        public String getEditeur() {
            return editeur;
        }

        public void setEditeur(String editeur) {
            this.editeur = editeur;
        }

        public Integer getAnneeMin() {
            return anneeMin;
        }

        public void setAnneeMin(Integer anneeMin) {
            this.anneeMin = anneeMin;
        }

        public Integer getAnneeMax() {
            return anneeMax;
        }

        public void setAnneeMax(Integer anneeMax) {
            this.anneeMax = anneeMax;
        }

        public boolean isDisponibleUniquement() {
            return disponibleUniquement;
        }

        public void setDisponibleUniquement(boolean disponibleUniquement) {
            this.disponibleUniquement = disponibleUniquement;
        }
    }

    private final ObjectProperty<FilterParams> filters =
            new SimpleObjectProperty<>(new FilterParams());

    public ObjectProperty<FilterParams> filtersProperty() {
        return filters;
    }

    public FilterParams getFilters() {
        return filters.get();
    }

    public void setFilters(FilterParams f) {
        filters.set(f != null ? f : new FilterParams());
    }

    // =========================================================================
    // Constructeur
    // =========================================================================

    public CatalogueViewModel(CatalogueService service, BookInfoProvider bookProvider) {
        this.catalogueService = Objects.requireNonNull(service, "CatalogueService ne peut être null");
        this.bookProvider = bookProvider; // nullable si non configuré
    }

    // =========================================================================
    // Recherche API externe (ISBN)
    // =========================================================================

    public void fetchBookInfoByIsbn(String isbn, BookInfoCallback callback) {
        if (bookProvider == null) {
            Platform.runLater(() ->
                    callback.onResult(false, null, "Le service de recherche API n'est pas configuré.")
            );
            return;
        }
        if (isbn == null || isbn.isBlank()) {
            Platform.runLater(() ->
                    callback.onResult(false, null, "ISBN vide ou invalide.")
            );
            return;
        }

        setLoading(true);
        setError("");

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return bookProvider.findBookByIsbn(isbn.trim());
                    } catch (Exception e) {
                        throw new RuntimeException("Erreur lors de l'appel API", e);
                    }
                })
                .whenComplete((resultOptional, throwable) -> {
                    setLoading(false);
                    Platform.runLater(() -> {
                        if (throwable != null) {
                            String msg = getRootCauseMessage(throwable);
                            setError("Erreur API : " + msg);
                            callback.onResult(false, null, msg);
                        } else {
                            Optional<BookMeta> opt = resultOptional;
                            if (opt != null && opt.isPresent()) {
                                callback.onResult(true, opt.get(), null);
                            } else {
                                callback.onResult(false, null, "Aucun document trouvé pour cet ISBN.");
                            }
                        }
                    });
                });
    }

    // =========================================================================
    // Chargement / Recherche BDD
    // =========================================================================

    /** Charge tous les documents sans filtre. */
    public void loadAllDocuments() {
        runSafely(() -> {
            List<Document> all = catalogueService.findAll();
            setDocuments(all);
        });
    }

    /** Alias plus expressif. */
    public void refresh() {
        loadAllDocuments();
    }

    /** Recherche selon queryProperty(). */
    public void search() {
        String q = getQuery();
        if (q == null || q.isBlank()) {
            loadAllDocuments();
        } else {
            search(q);
        }
    }

    /** Recherche texte dans la BDD (via service). */
    public void search(String q) {
        final String trimmed = (q == null) ? "" : q.trim();
        if (trimmed.isBlank()) {
            loadAllDocuments();
            return;
        }

        runSafely(() -> {
            List<Document> found = catalogueService.search(trimmed);
            setDocuments(found);
        });
    }

    /** Efface la recherche et recharge tout. */
    public void clearSearch() {
        query.set("");
        loadAllDocuments();
    }

    /**
     * Recherche + application des filtres en mémoire.
     * Charge d'abord soit tous les docs, soit le résultat de la recherche texte,
     * puis applique les filtres.
     */
    public void searchWithFilters() {
        runSafely(() -> {
            String q = (getQuery() == null ? "" : getQuery().trim());
            List<Document> base = q.isEmpty()
                    ? catalogueService.findAll()
                    : catalogueService.search(q);

            List<Document> filtered = applyInMemoryFilters(base, getFilters());
            setDocuments(filtered);
        });
    }

    /** Alias lisible. */
    public void refreshWithFilters() {
        searchWithFilters();
    }

    /**
     * Filtres en mémoire (type, genre, éditeur, intervalle années).
     */
    private static List<Document> applyInMemoryFilters(List<Document> src, FilterParams fp) {
        if (src == null || src.isEmpty()) {
            return List.of();
        }
        if (fp == null) {
            return List.copyOf(src);
        }

        final String filterGenre = (fp.getGenre() == null ? "" : fp.getGenre().trim().toLowerCase());
        final String filterEditeur = (fp.getEditeur() == null ? "" : fp.getEditeur().trim().toLowerCase());

        return src.stream()
                // Type
                .filter(d -> {
                    FilterParams.Type type = fp.getType();
                    if (type == null || type == FilterParams.Type.TOUS) {
                        return true;
                    }
                    String dtype = d.getDtype() != null ? d.getDtype() : "";
                    return switch (type) {
                        case LIVRE -> "Livre".equalsIgnoreCase(dtype);
                        case MAGAZINE -> "Magazine".equalsIgnoreCase(dtype);
                        case CD -> "CD".equalsIgnoreCase(dtype);
                        case TOUS -> true;
                    };
                })
                // Genre
                .filter(d -> {
                    if (filterGenre.isEmpty()) return true;
                    String g = d.getGenre();
                    return g != null && g.toLowerCase().contains(filterGenre);
                })
                // Éditeur
                .filter(d -> {
                    if (filterEditeur.isEmpty()) return true;
                    String e = d.getEditeur();
                    return e != null && e.toLowerCase().contains(filterEditeur);
                })
                // Année min/max
                .filter(d -> {
                    Integer y = d.getAnneePublication();
                    Integer min = fp.getAnneeMin();
                    Integer max = fp.getAnneeMax();
                    if (y == null) {
                        return min == null && max == null;
                    }
                    if (min != null && y < min) return false;
                    if (max != null && y > max) return false;
                    return true;
                })
                // disponibleUniquement : non appliqué ici (voir remarque ci-dessus)
                .toList();
    }

    // =========================================================================
    // CRUD
    // =========================================================================

    /** Crée un document (potentiellement avec couverture URL). */
    public void create(Document doc, String coverUrlToDownload) {
        if (doc == null) return;
        runSafely(() -> {
            catalogueService.create(doc, coverUrlToDownload);
            refreshListAfterCrud();
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

    /** Surcharge : création sans couverture distante. */
    public void create(Document doc) {
        create(doc, null);
    }

    /** Surcharge : update sans couverture distante. */
    public void update(Document doc) {
        update(doc, null);
    }

    /** Crée un document avec couverture depuis un fichier local. */
    public void createWithLocalCover(Document doc, String localFilePath) {
        if (doc == null || localFilePath == null || localFilePath.isBlank()) return;

        runSafely(() -> {
            try {
                catalogueService.createWithLocalCover(doc, Paths.get(localFilePath));
            } catch (Exception e) {
                setError("Erreur sauvegarde couverture : " + getRootCauseMessage(e));
            } finally {
                refreshListAfterCrud();
            }
        });
    }

    /** Met à jour un document avec couverture depuis un fichier local. */
    public void updateWithLocalCover(Document doc, String localFilePath) {
        if (doc == null || doc.getId() == null
                || localFilePath == null || localFilePath.isBlank()) {
            return;
        }

        runSafely(() -> {
            try {
                catalogueService.updateWithLocalCover(doc, Paths.get(localFilePath));
            } catch (Exception e) {
                setError("Erreur MAJ couverture : " + getRootCauseMessage(e));
            } finally {
                refreshListAfterCrud();
            }
        });
    }

    // --- Suppression ---

    /** Vérifie côté domaine si le document peut être supprimé. */
    public boolean canDelete(long id) {
        try {
            return catalogueService.canDelete(id);
        } catch (Exception ex) {
            setError(getRootCauseMessage(ex));
            return false;
        }
    }

    /** Supprime un document. */
    public void delete(long id) {
        runSafely(() -> {
            catalogueService.delete(id);
            refreshListAfterCrud();
        });
    }

    /** Recharge la liste après un CRUD. */
    private void refreshListAfterCrud() {
        List<Document> all = catalogueService.findAll();
        setDocuments(all);
    }

    // =========================================================================
    // Threading / gestion d'erreur générique
    // =========================================================================

    /** Exécute une opération en arrière-plan et gère loading + erreur. */
    private void runSafely(Runnable op) {
        setLoading(true);
        setError("");

        CompletableFuture
                .runAsync(op)
                .whenComplete((res, ex) ->
                        Platform.runLater(() -> {
                            if (ex != null) {
                                String msg = getRootCauseMessage(ex);
                                setError("Opération échouée : " + msg);
                            }
                            setLoading(false);
                        })
                );
    }

    private static String getRootCauseMessage(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable ne doit pas être null");
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        return (msg != null && !msg.isBlank()) ? msg : root.toString();
    }

    // --- Helpers FX-safe pour màj UI ---

    private void setLoading(boolean v) {
        if (Platform.isFxApplicationThread()) {
            loading.set(v);
        } else {
            Platform.runLater(() -> loading.set(v));
        }
    }

    private void setError(String msg) {
        String safe = (msg == null) ? "" : msg;
        if (Platform.isFxApplicationThread()) {
            error.set(safe);
        } else {
            Platform.runLater(() -> error.set(safe));
        }
    }

    private void setDocuments(List<Document> items) {
        List<Document> safe = (items == null) ? List.of() : items;
        if (Platform.isFxApplicationThread()) {
            documents.setAll(safe);
        } else {
            Platform.runLater(() -> documents.setAll(safe));
        }
    }

    // =========================================================================
    // Propriétés exposées (binding UI)
    // =========================================================================

    public ObservableList<Document> getDocuments() {
        return documents;
    }

    public StringProperty queryProperty() {
        return query;
    }

    public String getQuery() {
        return query.get();
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public StringProperty errorProperty() {
        return error;
    }

    public ObjectProperty<Document> selectedProperty() {
        return selected;
    }

    public Document getSelected() {
        return selected.get();
    }

    public void setSelected(Document d) {
        selected.set(d);
    }
}