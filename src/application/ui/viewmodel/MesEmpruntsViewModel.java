package application.ui.viewmodel;

import application.domain.core.document.Document;
import application.domain.core.emprunt.Emprunt;
import application.domain.ports.repository.DocumentRepository;
import application.domain.service.EmpruntService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ViewModel pour la vue "Mes emprunts" côté adhérent.
 *
 * - Filtre systématiquement sur l'adhérent courant (adherentId).
 * - Permet de basculer entre emprunts actifs et historique complet.
 * - N'affiche que les documents disponibles (non empruntés) pour l'emprunt.
 * - Fait remonter les messages d'erreur (quota, pénalités, etc.) et de succès.
 */
public final class MesEmpruntsViewModel {

    // ------------------ Dépendances ------------------

    private final EmpruntService empruntService;
    private final DocumentRepository documentRepository;

    // ------------------ État domaine / UI ------------------

    private long adherentId;

    private final ObservableList<Emprunt> emprunts =
            FXCollections.observableArrayList();

    private final ObservableList<Document> availableDocuments =
            FXCollections.observableArrayList();

    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty  error   = new SimpleStringProperty("");
    private final StringProperty  success = new SimpleStringProperty("");

    private final BooleanProperty actifsOnly = new SimpleBooleanProperty(true);

    // ------------------ Constructeurs ------------------

    /** Constructeur fallback (SceneBuilder). */
    public MesEmpruntsViewModel(EmpruntService empruntService) {
        this(empruntService, null, null);
    }

    public MesEmpruntsViewModel(EmpruntService empruntService,
                                DocumentRepository documentRepository,
                                Long adherentId) {

        this.empruntService = Objects.requireNonNull(empruntService, "empruntService");
        this.documentRepository = documentRepository; // peut être null en mode fallback

        // Quand on change le filtre actifsOnly, on relit les emprunts
        actifsOnly.addListener((obs, ov, nv) -> refresh());

        if (adherentId != null && adherentId > 0) {
            this.adherentId = adherentId;
            refresh();
            refreshAvailableDocuments();
        }
    }

    public void loadForAdherent(long adherentId) {
        if (adherentId <= 0) {
            throw new IllegalArgumentException("adherentId invalide : " + adherentId);
        }
        this.adherentId = adherentId;
        refresh();
        refreshAvailableDocuments();
    }

    // ------------------ Refresh emprunts ------------------

    public void refresh() {
        if (adherentId <= 0) {
            return;
        }

        runSafely(() -> {
            List<Emprunt> list = actifsOnly.get()
                    ? empruntService.findActiveByAdherentId(adherentId)
                    : empruntService.findByAdherentId(adherentId);

            List<Emprunt> safeList = (list != null) ? list : List.of();
            Platform.runLater(() -> emprunts.setAll(safeList));
        });
    }

    // ------------------ Documents disponibles ------------------

    public void refreshAvailableDocuments() {
        if (documentRepository == null) {
            availableDocuments.clear();
            return;
        }

        runSafely(() -> {
            List<Document> allDocs = documentRepository.findAll();
            List<Document> disponibles = (allDocs != null ? allDocs : List.<Document>of())
                    .stream()
                    .filter(doc -> doc.getId() != null
                            && !empruntService.isDocumentEmprunte(doc.getId()))
                    .collect(Collectors.toList());

            Platform.runLater(() -> availableDocuments.setAll(disponibles));
        });
    }

    // ------------------ Emprunt côté adhérent ------------------

    public void borrowForCurrentAdherent(Document document, LocalDate dateEmprunt) {
        if (adherentId <= 0) {
            reportError("Session adhérent inconnue : impossible de créer un emprunt.");
            return;
        }
        if (document == null || document.getId() == null) {
            reportError("Document invalide pour l'emprunt.");
            return;
        }
        if (dateEmprunt == null) {
            reportError("Veuillez sélectionner une date d'emprunt.");
            return;
        }

        setError("");
        setSuccess("");

        runSafely(() -> {
            Emprunt created = empruntService.borrow(adherentId, document.getId(), dateEmprunt);

            Platform.runLater(() -> {
                if (created != null && created.getId() != null) {
                    setSuccess("Emprunt enregistré (ID: " + created.getId() + ").");
                } else {
                    setError("Échec de la création de l'emprunt.");
                }
                refresh();
                refreshAvailableDocuments();
            });
        });
    }

    // ------------------ Bindings exposés ------------------

    public ObservableList<Emprunt> getEmprunts() {
        return emprunts;
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

    public BooleanProperty actifsOnlyProperty() {
        return actifsOnly;
    }

    public boolean isActifsOnly() {
        return actifsOnly.get();
    }

    public void setActifsOnly(boolean value) {
        actifsOnly.set(value);
    }

    public void reportError(String message) {
        setError(message);
    }

    // ------------------ Helpers async / erreurs ------------------

    private void runSafely(Runnable op) {
        setLoading(true);
        setError("");
        setSuccess("");

        CompletableFuture
                .runAsync(() -> {
                    // Mode fallback : si le service n’est pas dispo, on évite un NPE
                    if (empruntService == null) {
                        Platform.runLater(() -> setError("Service emprunt indisponible (mode fallback)."));
                        return;
                    }
                    op.run();
                })
                .whenComplete((res, ex) -> Platform.runLater(() -> {
                    if (ex != null) {
                        setError(getRootCauseMessage(ex));
                    }
                    setLoading(false);
                }));
    }

    private void setLoading(boolean value) {
        if (Platform.isFxApplicationThread()) {
            loading.set(value);
        } else {
            Platform.runLater(() -> loading.set(value));
        }
    }

    private void setError(String message) {
        String safe = (message == null) ? "" : message;
        if (Platform.isFxApplicationThread()) {
            error.set(safe);
        } else {
            Platform.runLater(() -> error.set(safe));
        }
    }

    private void setSuccess(String message) {
        String safe = (message == null) ? "" : message;
        if (Platform.isFxApplicationThread()) {
            success.set(safe);
        } else {
            Platform.runLater(() -> success.set(safe));
        }
    }

    private static String getRootCauseMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        return (msg != null && !msg.isBlank()) ? msg : root.toString();
    }
}