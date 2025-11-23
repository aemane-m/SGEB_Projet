package application.ui.viewmodel;

import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Penalite;
import application.domain.ports.repository.EmpruntRepository;
import application.domain.service.PenaliteService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ViewModel pour "Mes pénalités" côté adhérent.
 *
 * - Charge les pénalités d'un adhérent donné.
 * - Attache les Emprunts (et donc les Documents) pour l'affichage.
 * - Expose une commande demanderPaiement(penaliteId).
 */
public final class MesPenalitesViewModel {

    private final PenaliteService penaliteService;
    /** Utilisé pour retrouver les emprunts (et donc les documents) associés aux pénalités. */
    private final EmpruntRepository empruntRepository;

    /** Id de l'adhérent courant. */
    private long adherentId;

    private final ObservableList<Penalite> penalites = FXCollections.observableArrayList();

    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty  error   = new SimpleStringProperty("");
    private final StringProperty  success = new SimpleStringProperty("");

    // ------------------------------------------------------------------------
    // Constructeurs
    // ------------------------------------------------------------------------

    /** Constructeur fallback (tests / Scene Builder). */
    public MesPenalitesViewModel(PenaliteService penaliteService) {
        this(penaliteService, null, null);
    }

    /**
     * Constructeur complet utilisé par FxNavigator.
     *
     * @param penaliteService   service domaine des pénalités (obligatoire)
     * @param empruntRepository repository des emprunts (peut être null, mais recommandé pour afficher les documents)
     * @param adherentId        id de l'adhérent (peut être null si non encore connu)
     */
    public MesPenalitesViewModel(PenaliteService penaliteService,
                                 EmpruntRepository empruntRepository,
                                 Long adherentId) {

        this.penaliteService = Objects.requireNonNull(penaliteService, "penaliteService");
        this.empruntRepository = empruntRepository;

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

    // ------------------------------------------------------------------------
    // Chargement / rafraîchissement
    // ------------------------------------------------------------------------

    /**
     * Recharge les pénalités pour l'adhérent courant et attache les Emprunts.
     */
    public void refresh() {
        if (adherentId <= 0) {
            return; // pas encore de session adhérent
        }

        setLoading(true);
        setError("");
        setSuccess("");

        CompletableFuture
            .supplyAsync(() -> penaliteService.getPenalitesPourAdherent(adherentId))
            .whenComplete((list, ex) -> {
                if (ex != null) {
                    Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
                    Platform.runLater(() -> {
                        setError(cause.getMessage() != null ? cause.getMessage() : cause.toString());
                        penalites.clear();
                        setLoading(false);
                    });
                    return;
                }

                List<Penalite> safeList = (list != null) ? list : List.of();

                // Attache les Emprunts pour avoir Document / Emprunteur dans l'UI
                if (empruntRepository != null && !safeList.isEmpty()) {
                    List<Emprunt> allEmprunts = empruntRepository.findAll();
                    Map<Long, Emprunt> empruntMap = allEmprunts.stream()
                            .collect(Collectors.toMap(Emprunt::getId, e -> e));

                    for (Penalite p : safeList) {
                        Long empruntId = p.getEmpruntId();
                        if (empruntId != null) {
                            Emprunt e = empruntMap.get(empruntId);
                            if (e != null) {
                                p.setEmprunt(e);
                            }
                        }
                    }
                }

                Platform.runLater(() -> {
                    penalites.setAll(safeList);
                    if (safeList.isEmpty()) {
                        setSuccess("Aucune pénalité enregistrée.");
                    }
                    setLoading(false);
                });
            });
    }

    // ------------------------------------------------------------------------
    // Demande de paiement
    // ------------------------------------------------------------------------

    /**
     * Demande de paiement pour une pénalité donnée.
     * Côté domaine, PenaliteService.demanderPaiement(...) met à jour le statut
     * (par ex. DUE -> PAYMENT_REQUESTED) que l’admin verra dans son écran.
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
                        // On recharge pour voir le statut PAYMENT_REQUESTED
                        refresh();
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