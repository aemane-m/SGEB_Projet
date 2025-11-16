package application.ui.viewmodel;

import application.domain.core.document.Document;
import application.domain.core.emprunt.Emprunt;
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
import java.util.concurrent.CompletableFuture;

/**
 * ViewModel pour la vue "Mes emprunts" côté adhérent.
 *
 * - Filtre systématiquement sur l'adhérent courant (adherentId).
 * - Permet de basculer entre emprunts actifs et historique complet.
 * - Expose la liste des documents pour que l'adhérent puisse emprunter
 *   (les règles métier restent dans EmpruntService).
 */
public final class MesEmpruntsViewModel {

    private final EmpruntService empruntService;
    private final AdherentRepository adherentRepository;   // éventuellement non utilisé
    private final DocumentRepository documentRepository;

    /** Id de l'adhérent courant (session). */
    private long adherentId;

    /** Liste affichée dans la TableView. */
    private final ObservableList<Emprunt> emprunts =
            FXCollections.observableArrayList();

    /** Liste des documents que l'on propose à l'adhérent (tous ou filtrés). */
    private final ObservableList<Document> availableDocuments =
            FXCollections.observableArrayList();

    /** État UI. */
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty  error   = new SimpleStringProperty("");

    /** Filtre : true = uniquement actifs, false = historique complet. */
    private final BooleanProperty actifsOnly = new SimpleBooleanProperty(true);

    // ------------------------------------------------------------------------
    // Constructeurs
    // ------------------------------------------------------------------------

    /** Constructeur minimal. */
    public MesEmpruntsViewModel(EmpruntService empruntService) {
        this(empruntService, null, null, null);
    }

    /**
     * Constructeur complet utilisé par l'injection / Navigator.
     */
    public MesEmpruntsViewModel(EmpruntService empruntService,
                                AdherentRepository adherentRepository,
                                DocumentRepository documentRepository,
                                Long adherentId) {

        this.empruntService = Objects.requireNonNull(empruntService, "empruntService");
        this.adherentRepository = adherentRepository;
        this.documentRepository = documentRepository;

        // Quand on change le filtre, on recharge la liste
        actifsOnly.addListener((obs, ov, nv) -> refresh());

        if (adherentId != null && adherentId > 0) {
            this.adherentId = adherentId;
            refresh();
            refreshAvailableDocuments();
        }
    }

    /**
     * À appeler une fois que l'on connaît l'id de l'adhérent courant (session).
     */
    public void loadForAdherent(long adherentId) {
        if (adherentId <= 0) {
            throw new IllegalArgumentException("adherentId invalide : " + adherentId);
        }
        this.adherentId = adherentId;
        refresh();
        refreshAvailableDocuments();
    }

    // ------------------------------------------------------------------------
    // Chargement des emprunts
    // ------------------------------------------------------------------------

    /** Recharge la liste des emprunts pour l'adhérent courant. */
    public void refresh() {
        if (adherentId <= 0) {
            // Pas encore initialisé
            return;
        }

        setLoading(true);
        setError("");

        CompletableFuture
                .supplyAsync(() -> {
                    // ⚠️ IMPORTANT : on retourne toujours quelque chose
                    if (actifsOnly.get()) {
                        return empruntService.findActiveByAdherentId(adherentId);
                    } else {
                        return empruntService.findByAdherentId(adherentId);
                    }
                })
                .whenComplete((list, ex) -> {
                    Platform.runLater(() -> {
                        if (ex != null) {
                            setError(getRootCauseMessage(ex));
                            emprunts.clear();
                        } else {
                            List<Emprunt> safeList = (list != null) ? list : List.of();
                            emprunts.setAll(safeList);
                        }
                        setLoading(false);
                    });
                });
    }

    // ------------------------------------------------------------------------
    // Chargement des documents affichés à l'adhérent pour emprunt
    // ------------------------------------------------------------------------

    /**
     * Recharge la liste des documents à afficher dans la ComboBox côté adhérent.
     *
     * Ici, on utilise simplement `findAll()`. Les règles métier (disponibilité,
     * quotas, pénalités, etc.) sont vérifiées dans EmpruntService.borrow().
     */
    public void refreshAvailableDocuments() {
        if (documentRepository == null) {
            availableDocuments.clear();
            return;
        }

        setLoading(true);
        setError("");

        CompletableFuture
                .supplyAsync(() -> {
                    // ⚠️ IMPORTANT : on retourne une liste
                    return documentRepository.findAll();
                })
                .whenComplete((list, ex) -> {
                    Platform.runLater(() -> {
                        if (ex != null) {
                            setError(getRootCauseMessage(ex));
                            availableDocuments.clear();
                        } else {
                            List<Document> safeList = (list != null) ? list : List.of();
                            availableDocuments.setAll(safeList);
                        }
                        setLoading(false);
                    });
                });
    }

    /**
     * Demande d'emprunt pour l'adhérent courant.
     * Appelée par le contrôleur lorsqu'il clique sur "Emprunter".
     */
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

        CompletableFuture
                .runAsync(() -> {
                    // 👉 Les règles métier sont implémentées dans le service.
                    empruntService.borrow(adherentId, document.getId(), dateEmprunt);
                })
                .whenComplete((ignored, ex) -> {
                    Platform.runLater(() -> {
                        if (ex != null) {
                            setError(getRootCauseMessage(ex));
                        }
                        // On rafraîchit la liste et les documents proposés
                        refresh();
                        refreshAvailableDocuments();
                    });
                });
    }

    // ------------------------------------------------------------------------
    // Exposition (bindings UI)
    // ------------------------------------------------------------------------

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

    // ------------------------------------------------------------------------
    // Helpers internes
    // ------------------------------------------------------------------------

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

    private static String getRootCauseMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        return (msg != null && !msg.isBlank()) ? msg : root.toString();
    }
}