package application.domain.service;

import application.domain.core.emprunt.Penalite;
import application.domain.core.emprunt.PenaliteStatus;
import application.domain.ports.repository.PenaliteRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Implémentation du service de gestion des pénalités.
 */
public class DefaultPenaliteService implements PenaliteService {

    private final PenaliteRepository penaliteRepository;

    public DefaultPenaliteService(PenaliteRepository penaliteRepository) {
        this.penaliteRepository = Objects.requireNonNull(penaliteRepository, "penaliteRepository");
    }

    @Override
    public Penalite reglerPenalite(long penaliteId) {
        // 1. Trouver la pénalité
        Penalite penalite = penaliteRepository.findById(penaliteId)
            .orElseThrow(() -> new RuntimeException("Pénalité non trouvée (ID: " + penaliteId + ")"));

        // 2. Vérifier le statut métier
        if (penalite.getStatus() == PenaliteStatus.CANCELLED) {
            throw new IllegalStateException("Impossible de régler une pénalité annulée (ID: " + penaliteId + ").");
        }

        // 3. Idempotence : si déjà payée, on ne refait rien
        if (penalite.estPayee()) {
            return penalite;
        }

        // 4. Appliquer la logique métier (marquer comme payée AUJOURD'HUI)
        penalite.marquerCommePayee(LocalDate.now());

        // 5. Sauvegarder la modification
        return penaliteRepository.save(penalite);
    }

    /**
     * Implémentation de la logique pour "Tout régler".
     * Retourne le nombre de lignes impactées.
     */
    @Override
    public int reglerToutesPenalites(long adherentId) {
        // Délègue au repository qui applique un UPDATE massif
        return penaliteRepository.reglerToutesPourAdherent(adherentId, LocalDate.now());
    }

    /**
     * Workflow "demander paiement" côté adhérent.
     *
     * Ici on ne touche pas directement au statut si ton modèle
     * ne propose pas encore d’API dédiée ; on centralise quand même
     * la logique ici, et tu pourras la compléter plus tard (mise à jour
     * d’un statut, envoi de mail, etc.).
     */
    @Override
    public void demanderPaiement(long penaliteId) {
        Penalite penalite = penaliteRepository.findById(penaliteId)
            .orElseThrow(() -> new RuntimeException("Pénalité non trouvée (ID: " + penaliteId + ")"));

        penalite.demanderPaiement();
        penaliteRepository.save(penalite);
    }

    /**
     * Retourne les pénalités de l’adhérent.
     * Pour l’instant : on expose les pénalités actives (côté adhérent).
     */
    @Override
    public List<Penalite> getPenalitesPourAdherent(long adherentId) {
        // On utilise la méthode que tu as déjà dans ton code :
        // penaliteRepository.findActivesByAdherentId(...)
        return penaliteRepository.findActivesByAdherentId(adherentId);
    }
}