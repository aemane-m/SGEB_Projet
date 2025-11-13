package application.domain.service;

import application.domain.core.emprunt.Penalite;
import application.domain.ports.repository.PenaliteRepository;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Implémentation du service de gestion des pénalités.
 */
public class DefaultPenaliteService implements PenaliteService {

    private final PenaliteRepository penaliteRepository;

    public DefaultPenaliteService(PenaliteRepository penaliteRepository) {
        this.penaliteRepository = Objects.requireNonNull(penaliteRepository);
    }

    @Override
    public Penalite reglerPenalite(long penaliteId) {
        // 1. Trouver la pénalité
        Penalite penalite = penaliteRepository.findById(penaliteId)
            .orElseThrow(() -> new RuntimeException("Pénalité non trouvée (ID: " + penaliteId + ")"));

        // 2. Vérifier si elle est déjà payée (idempotence)
        if (penalite.estPayee()) {
            // Optionnel : on peut lever une exception ou juste la retourner
            // throw new RuntimeException("Cette pénalité est déjà réglée.");
            return penalite; // Déjà payée, rien à faire
        }

        // 3. Appliquer la logique métier (marquer comme payée AUJOURD'HUI)
        penalite.marquerCommePayee(LocalDate.now());

        // 4. Sauvegarder la modification
        return penaliteRepository.save(penalite);
    }
    
    /**
     * NOUVEAU : Implémentation de la logique pour "Tout Régler".
     */
    
    @Override
    public int reglerToutesPenalites(long adherentId) {
        // Appelle directement la requête UPDATE du repository
        return penaliteRepository.reglerToutesPourAdherent(adherentId, LocalDate.now());
    }
}

