package application.ui.viewmodel;

import application.domain.core.emprunt.Penalite;
import application.domain.ports.repository.EmpruntRepository;
import application.domain.ports.repository.PenaliteRepository;
import application.domain.service.PenaliteService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * ViewModel pour "Mes pénalités" côté adhérent.
 *
 * - Charge les pénalités d'un adhérent donné.
 * - Expose une commande demanderPaiement(penaliteId).
 */
public final class MesPenalitesViewModel {

    private final PenaliteService penaliteService;
    private EmpruntRepository empruntRepository;      // optionnel pour évolutions futures
    private PenaliteRepository penaliteRepository;    // idem

    /** Id de l'adhérent courant. */
    private long adherentId;

    private final ObservableList<Penalite> penalites = FXCollections.observableArrayList();

    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty  error   = new SimpleStringProperty("");
    private final StringProperty  success = new SimpleStringProperty("");

    public MesPenalitesViewModel(PenaliteService penaliteService) {
        this.penaliteService = Objects.requireNonNull(penaliteService, "penaliteService");
    }

    /**
     * Constructeur complet utilisé par FxNavigator.
     *
     * @param penaliteService   service domaine des pénalités
     * @param empruntRepository repository des emprunts (optionnel)
     * @param penaliteRepository repository des pénalités (optionnel)
     * @param adherentId        id de l'adhérent (peut être null)
     */
    public MesPenalitesViewModel(PenaliteService penaliteService,
                                 EmpruntRepository empruntRepository,
                                 PenaliteRepository penaliteRepository,
                                 Long adherentId) {

        this(penaliteService);
        this.empruntRepository = empruntRepository;
        this.penaliteRepository = penaliteRepository;

        if (adherentId != null && adherentId > 0) {
            this.adherentId = adherentId;
            refresh();
        }
    }

    /**
     * Initialise pour un adhérent (session).
     */
    public void loadForAdherent(long adherentId) {
        this.adherentId = adherentId;
        refresh();
    }

    /**
     * Recharge les pénalités pour l'adhérent courant.
     */
    public void refresh() {
        if (adherentId <= 0) {
            return;
        }

        setLoading(true);
        setError("");
        setSuccess("");

        CompletableFuture
            .supplyAsync(() -> penaliteService.getPenalitesPourAdherent(adherentId))
            .whenComplete((list, ex) -> {
                Platform.runLater(() -> {
                    if (ex != null) {
                        Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
                        setError(cause.getMessage() != null ? cause.getMessage() : cause.toString());
                        penalites.clear(); // <-- au lieu de setAll()
                    } else {
                        // list est maintenant List<Penalite>
                        var safeList = (list != null) ? list : List.<Penalite>of();
                        penalites.setAll(safeList); // <-- plus d'erreur de type
                    }
                    setLoading(false);
                });
            });
    }

    /**
     * Demande de paiement pour une pénalité donnée.
     */
    public void demanderPaiement(long penaliteId) {
        if (penaliteId <= 0) {
            setError("Pénalité invalide.");
            return;
        }

        setLoading(true);
        setError("");
        setSuccess("");

        CompletableFuture
            .runAsync(() -> penaliteService.demanderPaiement(penaliteId))
            .whenComplete((res, ex) -> {
                Platform.runLater(() -> {
                    if (ex != null) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        setError(cause.getMessage() != null ? cause.getMessage() : cause.toString());
                    } else {
                        setSuccess("Demande de paiement envoyée.");
                        refresh(); // Recharge les statuts
                    }
                    setLoading(false);
                });
            });
    }

    // ================== Bindings UI ==================

    public ObservableList<Penalite> getPenalites() {
        return penalites;
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

    // ================== Helpers ==================

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
}