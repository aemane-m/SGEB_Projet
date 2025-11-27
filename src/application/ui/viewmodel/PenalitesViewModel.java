package application.ui.viewmodel;

import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Money;
import application.domain.core.emprunt.Penalite;
import application.domain.core.emprunt.PenaliteStatus;
import application.domain.core.personne.Adherent;
import application.domain.ports.repository.EmpruntRepository;
import application.domain.ports.repository.PenaliteRepository;
import application.domain.service.PenaliteService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ViewModel pour la vue de gestion des pénalités.
 *
 * - Table maître : un résumé par adhérent (nombre de pénalités, total dû,
 *   nombre de pénalités avec demande de paiement).
 * - Table détail : la liste des pénalités de l’adhérent sélectionné.
 * - Commandes domaine :
 *   - régler une pénalité
 *   - régler toutes les pénalités d’un adhérent
 */
public final class PenalitesViewModel {

    // --- Repositories & Services ---
    private final PenaliteRepository penaliteRepository;
    private final EmpruntRepository empruntRepository;
    private final PenaliteService penaliteService;

    // --- État UI global ---
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty error   = new SimpleStringProperty("");
    private final StringProperty success = new SimpleStringProperty("");

    /**
     * Résumé maître : un adhérent, le nombre de pénalités actives,
     * le total dû, et le nombre de pénalités avec demande de paiement.
     */
    public record AdherentPenaliteSummary(
            Adherent adherent,
            int count,
            Money total,
            int requestedCount
    ) { }

    // --- Données Table Maître ---
    private final ObservableList<AdherentPenaliteSummary> summaries =
            FXCollections.observableArrayList();

    // --- Données Table Détail ---
    private final ObservableList<Penalite> details =
            FXCollections.observableArrayList();

    private final ObjectProperty<AdherentPenaliteSummary> selectedSummary =
            new SimpleObjectProperty<>();

    // Cache interne des pénalités actives (date_fin IS NULL)
    private List<Penalite> allActivePenalties = List.of();

    // --- Constructeur ---
    public PenalitesViewModel(PenaliteRepository penaliteRepository,
                              EmpruntRepository empruntRepository,
                              PenaliteService penaliteService) {

        this.penaliteRepository = Objects.requireNonNull(
                penaliteRepository, "penaliteRepository ne peut pas être null"
        );
        this.empruntRepository = Objects.requireNonNull(
                empruntRepository, "empruntRepository ne peut pas être null"
        );
        this.penaliteService = Objects.requireNonNull(
                penaliteService, "penaliteService ne peut pas être null"
        );

        // Quand on change d’adhérent sélectionné, on recharge les détails
        selectedSummary.addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                details.clear();
            } else {
                loadDetailsFor(newVal.adherent());
            }
        });

        // Chargement initial
        refresh();
    }

    // =====================================================================
    // ===================== Chargement / Agrégation ========================
    // =====================================================================

    /**
     * Charge toutes les pénalités actives et les regroupe par adhérent.
     * Utilise Penalite.getStatus() pour compter les demandes de paiement.
     */
    public void refresh() {
        runSafely(() -> {
            // 1. Récupère TOUTES les pénalités actives (date_fin IS NULL)
            List<Penalite> actives = penaliteRepository.findAllActives();
            allActivePenalties = (actives != null) ? actives : List.of();

            // 2. Récupère tous les emprunts pour attacher les détails (doc + adhérent)
            Map<Long, Emprunt> empruntMap = empruntRepository.findAll().stream()
                    .collect(Collectors.toMap(Emprunt::getId, e -> e));

            // 3. Attache l’emprunt à chaque pénalité (pour la table détails + affichage)
            for (Penalite p : allActivePenalties) {
                Emprunt e = empruntMap.get(p.getEmpruntId());
                if (e != null) {
                    p.setEmprunt(e);
                }
            }

            // 4. Regroupe par adhérent (en s’assurant que l’emprunteur existe)
            Map<Adherent, List<Penalite>> groupedByAdherent = allActivePenalties.stream()
                    .filter(p -> p.getEmprunt() != null
                            && p.getEmprunt().getEmprunteur() != null)
                    .collect(Collectors.groupingBy(
                            p -> p.getEmprunt().getEmprunteur()
                    ));

            // 5. Construit la liste des résumés
            List<AdherentPenaliteSummary> summaryList = groupedByAdherent.entrySet().stream()
                    .map(entry -> {
                        Adherent adherent = entry.getKey();
                        List<Penalite> penalties = entry.getValue();

                        int count = penalties.size();

                        BigDecimal total = penalties.stream()
                                .map(p -> p.getMontant().amount())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        int requested = (int) penalties.stream()
                                .filter(PenalitesViewModel::isPaymentRequested)
                                .count();

                        return new AdherentPenaliteSummary(
                                adherent,
                                count,
                                Money.euros(total),
                                requested
                        );
                    })
                    // Trier par total dû décroissant
                    .sorted((a, b) -> b.total().amount().compareTo(a.total().amount()))
                    .toList();

            // 6. Mise à jour UI sur le thread JavaFX
            Platform.runLater(() -> {
                summaries.setAll(summaryList);
                details.clear();
                if (summaries.isEmpty()) {
                    setSuccess("Aucune pénalité active trouvée.");
                }
            });
        });
    }

    /**
     * Charge les pénalités de l’adhérent donné dans la table détail,
     * en filtrant le cache allActivePenalties.
     */
    private void loadDetailsFor(Adherent adherent) {
        if (adherent == null || allActivePenalties == null || allActivePenalties.isEmpty()) {
            details.clear();
            return;
        }

        Long adherentId = adherent.getId();
        if (adherentId == null) {
            details.clear();
            return;
        }

        List<Penalite> adherentPenalties = allActivePenalties.stream()
                .filter(p -> p.getEmprunt() != null
                        && p.getEmprunt().getEmprunteur() != null
                        && adherentId.equals(p.getEmprunt().getEmprunteur().getId()))
                .toList();

        details.setAll(adherentPenalties);
    }

    // =====================================================================
    // =================== Actions (commandes domaine) ======================
    // =====================================================================

    /**
     * Règle une pénalité spécifique (appelle PenaliteService).
     */
    public void reglerPenalite(Penalite penalite) {
        if (penalite == null) {
            setError("Aucune pénalité sélectionnée.");
            return;
        }

        runSafely(() -> {
            Penalite updated = penaliteService.reglerPenalite(penalite.getId());
            Platform.runLater(() -> {
                setSuccess("Pénalité (ID: " + updated.getId() + ") réglée.");
                refresh();
            });
        });
    }

    /**
     * Règle toutes les pénalités actives pour l’adhérent actuellement sélectionné.
     */
    public void reglerToutesPenalitesSelectionnees() {
        AdherentPenaliteSummary summary = selectedSummary.get();
        if (summary == null || summary.adherent() == null || summary.adherent().getId() == null) {
            setError("Aucun adhérent sélectionné.");
            return;
        }

        long adherentId = summary.adherent().getId();

        runSafely(() -> {
            int count = penaliteService.reglerToutesPenalites(adherentId);
            Platform.runLater(() -> {
                setSuccess(count + " pénalité(s) réglée(s) pour "
                        + summary.adherent().getNomComplet() + ".");
                refresh();
            });
        });
    }

    // =====================================================================
    // ============================ Helpers =================================
    // =====================================================================

    /**
     * True si la pénalité est dans un état "demande de paiement".
     */
    private static boolean isPaymentRequested(Penalite p) {
        if (p == null || p.getStatus() == null) {
            return false;
        }
        PenaliteStatus st = p.getStatus();
        return st == PenaliteStatus.PAYMENT_REQUESTED;
    }

    private void runSafely(Runnable op) {
        setLoading(true);
        setError("");
        setSuccess("");

        CompletableFuture
                .runAsync(op)
                .whenComplete((res, ex) -> Platform.runLater(() -> {
                    if (ex != null) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        setError(cause.getMessage() != null ? cause.getMessage() : cause.toString());
                    }
                    setLoading(false);
                }));
    }

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

    private void setSuccess(String msg) {
        String safe = (msg == null) ? "" : msg;
        if (Platform.isFxApplicationThread()) {
            success.set(safe);
        } else {
            Platform.runLater(() -> success.set(safe));
        }
    }

    // =====================================================================
    // ======================== Getters / Bindings ==========================
    // =====================================================================

    public ObservableList<AdherentPenaliteSummary> getAdherentSummaries() {
        return summaries;
    }

    public ObservableList<Penalite> getSelectedAdherentPenalties() {
        return details;
    }

    public ObjectProperty<AdherentPenaliteSummary> selectedSummaryProperty() {
        return selectedSummary;
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
}