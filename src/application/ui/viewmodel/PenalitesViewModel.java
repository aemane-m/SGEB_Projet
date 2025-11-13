package application.ui.viewmodel;

import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Money;
import application.domain.core.emprunt.Penalite;
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
 * ViewModel pour la vue de gestion des pénalités (Maître-Détail).
 * CORRIGÉ : Ajout de la logique pour "Tout Régler".
 */
public class PenalitesViewModel {

    // --- Repositories & Services ---
    private final PenaliteRepository penaliteRepository;
    private final EmpruntRepository empruntRepository;
    private final PenaliteService penaliteService;

    // --- État UI ---
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty("");
    private final StringProperty success = new SimpleStringProperty("");

    // --- Données Table Maître (DTO) ---
    public record AdherentPenaliteSummary(Adherent adherent, int count, Money total) {}
    private final ObservableList<AdherentPenaliteSummary> summaries = FXCollections.observableArrayList();
    
    // --- Données Table Détail ---
    private final ObservableList<Penalite> details = FXCollections.observableArrayList();
    private final ObjectProperty<AdherentPenaliteSummary> selectedSummary = new SimpleObjectProperty<>();
    
    // Cache interne des données
    private List<Penalite> allActivePenalties;

    public PenalitesViewModel(PenaliteRepository penaliteRepository, 
                              EmpruntRepository empruntRepository, 
                              PenaliteService penaliteService) {
        this.penaliteRepository = Objects.requireNonNull(penaliteRepository);
        this.empruntRepository = Objects.requireNonNull(empruntRepository);
        this.penaliteService = Objects.requireNonNull(penaliteService);
        
        selectedSummary.addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                details.clear();
            } else {
                loadDetailsFor(newVal.adherent());
            }
        });
        refresh();
    }

    /**
     * Charge toutes les pénalités actives et les regroupe par adhérent.
     */
    public void refresh() {
        runSafely(() -> {
            // 1. Récupère TOUTES les pénalités actives
            allActivePenalties = penaliteRepository.findAllActives();
            
            // 2. Récupère TOUS les emprunts (pour les détails Adhérent/Document)
            Map<Long, Emprunt> empruntMap = empruntRepository.findAll().stream()
                    .collect(Collectors.toMap(Emprunt::getId, e -> e));

            // 3. Attache les détails (titre, nom) aux pénalités
            for (Penalite p : allActivePenalties) {
                Emprunt e = empruntMap.get(p.getEmpruntId());
                if (e != null) {
                    p.setEmprunt(e); // Attache l'emprunt (avec ses proxys Adherent/Document)
                }
            }

            // 4. Regroupe par Adhérent pour la table Maître
            Map<Adherent, List<Penalite>> groupedByAdherent = allActivePenalties.stream()
                .filter(p -> p.getEmprunt() != null && p.getEmprunt().getEmprunteur() != null)
                .collect(Collectors.groupingBy(p -> p.getEmprunt().getEmprunteur()));

            // 5. Calcule les totaux
            List<AdherentPenaliteSummary> summaryList = groupedByAdherent.entrySet().stream()
                .map(entry -> {
                    Adherent adherent = entry.getKey();
                    List<Penalite> penalties = entry.getValue();
                    int count = penalties.size();
                    BigDecimal total = penalties.stream()
                        .map(p -> p.getMontant().amount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return new AdherentPenaliteSummary(adherent, count, Money.euros(total));
                })
                .sorted((a, b) -> b.total().amount().compareTo(a.total().amount()))
                .toList();

            // 6. Met à jour l'UI sur le thread FX
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
     * Charge la table Détail en filtrant la liste complète.
     */
    private void loadDetailsFor(Adherent adherent) {
        if (adherent == null || allActivePenalties == null) {
            details.clear();
            return;
        }
        
        List<Penalite> adherentPenalties = allActivePenalties.stream()
            .filter(p -> p.getEmprunt() != null && p.getEmprunt().getEmprunteur() != null && p.getEmprunt().getEmprunteur().getId().equals(adherent.getId()))
            .toList();
            
        details.setAll(adherentPenalties);
    }

    /**
     * Appelle le service pour régler une pénalité spécifique.
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
                refresh(); // Recharge tout
            });
        });
    }

    /**
     * NOUVEAU : Règle toutes les pénalités pour l'adhérent sélectionné.
     */
    public void reglerToutesPenalitesSelectionnees() {
        AdherentPenaliteSummary summary = selectedSummary.get();
        if (summary == null || summary.adherent() == null) {
            setError("Aucun adhérent sélectionné.");
            return;
        }
        
        runSafely(() -> {
            int count = penaliteService.reglerToutesPenalites(summary.adherent().getId());
            
            Platform.runLater(() -> {
                setSuccess(count + " pénalité(s) réglée(s) pour " + summary.adherent().getNomComplet() + ".");
                refresh(); // Recharge tout
            });
        });
    }


    // ... (runSafely, setLoading, setError, setSuccess) ...
    private void runSafely(Runnable op) { setLoading(true); setError(""); setSuccess(""); CompletableFuture.runAsync(op) .whenComplete((res, ex) -> { Platform.runLater(() -> { if (ex != null) { Throwable cause = ex.getCause() != null ? ex.getCause() : ex; setError(cause.getMessage() != null ? cause.getMessage() : cause.toString()); } setLoading(false); }); }); }
    private void setLoading(boolean v) { Platform.runLater(() -> loading.set(v)); }
    private void setError(String msg) { Platform.runLater(() -> error.set(msg == null ? "" : msg)); }
    private void setSuccess(String msg) { Platform.runLater(() -> success.set(msg == null ? "" : msg)); }

    // --- Getters pour les Observables ---
    public ObservableList<AdherentPenaliteSummary> getAdherentSummaries() { return summaries; }
    public ObservableList<Penalite> getSelectedAdherentPenalties() { return details; }
    public ObjectProperty<AdherentPenaliteSummary> selectedSummaryProperty() { return selectedSummary; }
    public ReadOnlyBooleanProperty loadingProperty() { return loading; }
    public ReadOnlyStringProperty errorProperty() { return error; }
    public ReadOnlyStringProperty successProperty() { return success; }
}
