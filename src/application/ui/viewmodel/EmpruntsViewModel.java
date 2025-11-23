package application.ui.viewmodel;

import application.domain.core.document.Document;
import application.domain.core.personne.Adherent;
import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Penalite;
import application.domain.ports.repository.AdherentRepository;
import application.domain.ports.repository.DocumentRepository;
import application.domain.service.EmpruntService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ViewModel pour l'écran des Emprunts.
 * Gère les opérations de manière asynchrone, charge les listes
 * pour les ComboBox et gère la logique d'emprunt/retour.
 */
public final class EmpruntsViewModel {

    // --- Services et Repositories ---
    private final EmpruntService empruntService;
    private final AdherentRepository adherentRepository;
    private final DocumentRepository documentRepository;

    // --- Listes de Données ---
    /** Liste brute complète (cache local des emprunts chargés) */
    private final ObservableList<Emprunt> sourceEmprunts = FXCollections.observableArrayList();
    /** Liste filtrée, liée à la TableView */
    private final ObservableList<Emprunt> filteredEmprunts = FXCollections.observableArrayList();

    // Listes pour les ComboBox du formulaire
    private final ObservableList<Adherent> adherents = FXCollections.observableArrayList();
    private final ObservableList<Document> availableDocuments = FXCollections.observableArrayList();

    // --- État UI ---
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty  error   = new SimpleStringProperty("");
    private final StringProperty  success = new SimpleStringProperty("");
    private final ObjectProperty<Emprunt> selected = new SimpleObjectProperty<>(null);

    // --- Filtres ---
    private final BooleanProperty actifsOnly  = new SimpleBooleanProperty(true);
    private final BooleanProperty overdueOnly = new SimpleBooleanProperty(false);

    /**
     * Constructeur principal pour l'injection de dépendances.
     */
    public EmpruntsViewModel(EmpruntService empruntService,
                             AdherentRepository adherentRepository,
                             DocumentRepository documentRepository) {
        this.empruntService     = Objects.requireNonNull(empruntService, "empruntService");
        this.adherentRepository = Objects.requireNonNull(adherentRepository, "adherentRepository");
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository");

        // Rafraîchir la liste principale quand les filtres changent
        actifsOnly.addListener((o, ov, nv) -> refresh());
        overdueOnly.addListener((o, ov, nv) -> applyFilters());

        // Charge les données initiales
        loadInitialData();
    }

    // =======================================================
    // Chargement / rafraîchissement
    // =======================================================

    /** Charge les listes de base (Emprunts, Adhérents, Documents). */
    public void loadInitialData() {
        runSafely(() -> {
            // Liste des adhérents
            List<Adherent> adhList = adherentRepository.findAll();

            // Documents non empruntés
            List<Document> docList = documentRepository.findAll().stream()
                    .filter(doc -> doc.getId() != null && !empruntService.isDocumentEmprunte(doc.getId()))
                    .collect(Collectors.toList());

            // Liste principale des emprunts
            List<Emprunt> empList = isActifsOnly()
                    ? empruntService.findActive()
                    : empruntService.findAll();

            Platform.runLater(() -> {
                adherents.setAll(adhList != null ? adhList : List.of());
                availableDocuments.setAll(docList != null ? docList : List.of());
                setSource(empList != null ? empList : List.of());
                applyFilters();
                setSuccess("");
            });
        });
    }

    /** Rafraîchit uniquement la liste des emprunts (et des documents dispo). */
    public void refresh() {
        runSafely(() -> {
            List<Emprunt> list = isActifsOnly()
                    ? empruntService.findActive()
                    : empruntService.findAll();

            List<Document> docList = documentRepository.findAll().stream()
                    .filter(doc -> doc.getId() != null && !empruntService.isDocumentEmprunte(doc.getId()))
                    .collect(Collectors.toList());

            Platform.runLater(() -> {
                setSource(list != null ? list : List.of());
                availableDocuments.setAll(docList != null ? docList : List.of());
                applyFilters();
                setSuccess("");
            });
        });
    }

    /** Applique les filtres (retards) sur la liste 'source'. */
    private void applyFilters() {
        List<Emprunt> filtered = sourceEmprunts.stream()
                .filter(e -> {
                    if (e == null) return false;
                    if (overdueOnly.get()) {
                        return e.estEnRetard(LocalDate.now());
                    }
                    return true;
                })
                .collect(Collectors.toList());

        setEmprunts(filtered);
    }

    // =======================================================
    // Emprunts / Retours
    // =======================================================

    /** Tente de créer un nouvel emprunt. */
    public void borrow(long adherentId, long documentId, LocalDate dateEmprunt) {
        runSafely(() -> {
            LocalDate d = (dateEmprunt != null) ? dateEmprunt : LocalDate.now();
            Emprunt created = empruntService.borrow(adherentId, documentId, d);

            Platform.runLater(() -> {
                if (created != null && created.getId() != null) {
                    setSuccess("Emprunt enregistré (ID: " + created.getId() + ")");
                    refresh();
                } else {
                    setError("Échec de la création de l'emprunt.");
                }
            });
        });
    }

    /** Tente de retourner l'emprunt sélectionné (avec la date du jour). */
    public void returnSelected() {
        Emprunt e = selected.get();
        if (e != null && e.getId() != null) {
            returnEmprunt(e.getId(), LocalDate.now());
        } else {
            setError("Aucun emprunt sélectionné.");
        }
    }

    /** Logique de retour d'emprunt (appelée par le Controller). */
    public void returnEmprunt(long empruntId, LocalDate dateRetour) {
        if (dateRetour == null) {
            setError("Veuillez sélectionner une date de retour.");
            return;
        }

        runSafely(() -> {
            Penalite penalite = empruntService.returnEmprunt(empruntId, dateRetour);

            Platform.runLater(() -> {
                if (penalite != null) {
                    setSuccess("Retour enregistré. Pénalité générée : " + penalite.getMontant());
                } else {
                    setSuccess("Retour enregistré. Pas de pénalité.");
                }
                refresh();
            });
        });
    }

    /**
     * Retourne un emprunt basé sur l'ID du document (utilisé par un combo de documents, par ex.).
     */
    public void returnByDocumentId(long documentId, LocalDate dateRetour) {
        if (dateRetour == null) {
            setError("Veuillez sélectionner une date de retour.");
            return;
        }

        runSafely(() -> {
            // 1) Cherche dans la source locale
            Optional<Emprunt> empruntOpt = sourceEmprunts.stream()
                    .filter(e ->
                            e != null &&
                            e.getDocument() != null &&
                            e.getDocument().getId() != null &&
                            e.getDocument().getId().equals(documentId)
                    )
                    .findFirst();

            // 2) Si non trouvé, rechargement des emprunts actifs en mémoire
            if (empruntOpt.isEmpty()) {
                List<Emprunt> actives = empruntService.findActive();
                setSource(actives != null ? actives : List.of());

                empruntOpt = (actives != null ? actives : List.<Emprunt>of()).stream()
                        .filter(e ->
                                e != null &&
                                e.getDocument() != null &&
                                e.getDocument().getId() != null &&
                                e.getDocument().getId().equals(documentId)
                        )
                        .findFirst();

                if (empruntOpt.isEmpty()) {
                    throw new IllegalStateException("Aucun emprunt actif trouvé pour le document ID: " + documentId);
                }
            }

            // 3) Retour de l'emprunt trouvé
            Emprunt empruntARetourner = empruntOpt.get();
            Penalite penalite = empruntService.returnEmprunt(empruntARetourner.getId(), dateRetour);

            Platform.runLater(() -> {
                if (penalite != null) {
                    setSuccess("Retour (Doc ID: " + documentId + ") enregistré. Pénalité : " + penalite.getMontant());
                } else {
                    setSuccess("Retour (Doc ID: " + documentId + ") enregistré. Pas de pénalité.");
                }
                refresh();
            });
        });
    }

    // =======================================================
    // Utilitaires asynchrones / erreurs
    // =======================================================

    private void runSafely(Runnable op) {
        setLoading(true);
        setError("");
        setSuccess("");

        CompletableFuture
                .runAsync(op)
                .whenComplete((res, ex) -> Platform.runLater(() -> {
                    if (ex != null) {
                        setError(getRootCauseMessage(ex));
                    }
                    setLoading(false);
                }));
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

    // --- Helpers thread-safe pour màj UI ---
    private void setLoading(boolean v) { Platform.runLater(() -> loading.set(v)); }

    private void setError(String msg) {
        String safe = (msg == null) ? "" : msg;
        Platform.runLater(() -> error.set(safe));
    }

    private void setSuccess(String msg) {
        String safe = (msg == null) ? "" : msg;
        Platform.runLater(() -> success.set(safe));
    }

    private void setSource(List<Emprunt> items) {
        List<Emprunt> safeList = (items != null) ? items : List.of();
        if (Platform.isFxApplicationThread()) {
            sourceEmprunts.setAll(safeList);
        } else {
            Platform.runLater(() -> sourceEmprunts.setAll(safeList));
        }
    }

    private void setEmprunts(List<Emprunt> items) {
        List<Emprunt> safeList = (items != null) ? items : List.of();
        if (Platform.isFxApplicationThread()) {
            filteredEmprunts.setAll(safeList);
        } else {
            Platform.runLater(() -> filteredEmprunts.setAll(safeList));
        }
    }

    // =======================================================
    // Bindings exposés à la vue
    // =======================================================

    /** Liste filtrée pour la TableView */
    public ObservableList<Emprunt> getEmprunts() {
        return filteredEmprunts;
    }

    /** Liste source (utilisable pour un ComboBox d'emprunts actifs, par ex.) */
    public ObservableList<Emprunt> getActiveEmpruntsList() {
        return sourceEmprunts;
    }

    public ObservableList<Adherent> getAdherents() {
        return adherents;
    }

    public ObservableList<Document> getAvailableDocuments() {
        return availableDocuments;
    }

    public ReadOnlyBooleanProperty loadingProperty() {
        return loading;
    }

    public ReadOnlyStringProperty errorProperty() {
        return error;
    }

    public ReadOnlyStringProperty successProperty() {
        return success;
    }

    public ObjectProperty<Emprunt> selectedProperty() {
        return selected;
    }

    public Emprunt getSelected() {
        return selected.get();
    }

    public void setSelected(Emprunt e) {
        selected.set(e);
    }

    public BooleanProperty actifsOnlyProperty() {
        return actifsOnly;
    }

    public boolean isActifsOnly() {
        return actifsOnly.get();
    }

    public void setActifsOnly(boolean v) {
        actifsOnly.set(v);
    }

    public BooleanProperty overdueOnlyProperty() {
        return overdueOnly;
    }

    public boolean isOverdueOnly() {
        return overdueOnly.get();
    }

    public void setOverdueOnly(boolean v) {
        overdueOnly.set(v);
    }

    // Helper pour le contrôleur
    public void reportError(String message) {
        setError(message);
    }
}