package application.ui.viewmodel;

import application.domain.core.emprunt.Emprunt;
import application.domain.ports.repository.AdherentRepository;
import application.domain.ports.repository.DocumentRepository;
import application.domain.service.EmpruntService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * ViewModel pour la vue "Mes emprunts" côté adhérent.
 *
 * - Filtre systématiquement sur l'adhérent courant (adherentId).
 * - Permet de basculer entre emprunts actifs et historique complet.
 */
public final class MesEmpruntsViewModel {

    private final EmpruntService empruntService;
    private final AdherentRepository adherentRepository;
    private final DocumentRepository documentRepository;

    /** Id de l'adhérent courant (session). */
    private long adherentId;

    /** Liste affichée dans la TableView. */
    private final ObservableList<Emprunt> emprunts = FXCollections.observableArrayList();

    /** État UI. */
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty  error   = new SimpleStringProperty("");

    /** Filtre : true = uniquement actifs, false = historique complet. */
    private final BooleanProperty actifsOnly = new SimpleBooleanProperty(true);

    /**
     * Constructeur minimal (sans repos explicites).
     * Utile si tu n'as pas besoin des repositories ici.
     */
    public MesEmpruntsViewModel(EmpruntService empruntService) {
        this(empruntService, null, null, null);
    }

    /**
     * Constructeur complet utilisé par FxNavigator.
     *
     * @param empruntService     service domaine des emprunts
     * @param adherentRepository repository adhérents (optionnel)
     * @param documentRepository repository documents (optionnel)
     * @param adherentId         id de l'adhérent courant (peut être null)
     */
    public MesEmpruntsViewModel(EmpruntService empruntService,
                                AdherentRepository adherentRepository,
                                DocumentRepository documentRepository,
                                Long adherentId) {

        this.empruntService = Objects.requireNonNull(empruntService, "empruntService");
        this.adherentRepository = adherentRepository;     // éventuellement null pour l’instant
        this.documentRepository = documentRepository;     // idem

        // Quand on change le filtre, on recharge
        actifsOnly.addListener((obs, ov, nv) -> refresh());

        if (adherentId != null && adherentId > 0) {
            this.adherentId = adherentId;
            refresh();
        }
    }

    /**
     * Initialise le ViewModel pour un adhérent.
     * Doit être appelé une fois qu'on connaît l'id de l'adhérent (depuis la session).
     */
    public void loadForAdherent(long adherentId) {
        this.adherentId = adherentId;
        refresh();
    }

    /**
     * Recharge la liste des emprunts pour l'adhérent courant.
     */
    public void refresh() {
        if (adherentId <= 0) {
            // Pas encore initialisé
            return;
        }

        setLoading(true);
        setError("");

        CompletableFuture
            .<List<Emprunt>>supplyAsync(() -> {   // <-- forcer le type ici
                if (actifsOnly.get()) {
                    // Méthode dédiée par adhérent (actifs)
                    return empruntService.findActiveByAdherentId(adherentId);
                } else {
                    // Historique complet
                    return empruntService.findByAdherentId(adherentId);
                }
            })
            .whenComplete((list, ex) -> {
                Platform.runLater(() -> {
                    if (ex != null) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        setError(cause.getMessage() != null ? cause.getMessage() : cause.toString());
                        emprunts.clear(); // <-- au lieu de setAll()
                    } else {
                        var safeList = (list != null) ? list : List.<Emprunt>of();
                        emprunts.setAll(safeList); // <-- plus d’erreur de type
                    }
                    setLoading(false);
                });
            });
    }

    // =========================================================
    // =============== Exposition (bindings UI) ================
    // =========================================================

    public ObservableList<Emprunt> getEmprunts() {
        return emprunts;
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

    public void setActifsOnly(boolean v) {
        actifsOnly.set(v);
    }

    // =========================================================
    // ================= Helpers internes ======================
    // =========================================================

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
}