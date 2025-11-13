package application.domain.service;

import application.domain.core.emprunt.Penalite;

/**
 * Interface (Port) pour le service de gestion des pénalités.
 */
public interface PenaliteService {

    /**
     * Marque une pénalité spécifique comme étant payée (en définissant sa date_fin).
     * @param penaliteId L'ID de la pénalité à régler.
     * @return La pénalité mise à jour.
     * @throws RuntimeException si la pénalité n'est pas trouvée ou est déjà payée.
     */
    Penalite reglerPenalite(long penaliteId);

    /**
     * NOUVEAU : Marque toutes les pénalités actives d'un adhérent comme payées.
     * @param adherentId L'ID de l'adhérent.
     * @return Le nombre de pénalités réglées.
     */
    int reglerToutesPenalites(long adherentId);
    

}
