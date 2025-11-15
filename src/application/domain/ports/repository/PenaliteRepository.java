package application.domain.ports.repository;

import application.domain.core.emprunt.Penalite;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Port (Repository) pour la gestion des pénalités.
 * 
 * Cette interface définit toutes les opérations nécessaires pour
 * récupérer, persister et mettre à jour les pénalités appliquées aux adhérents.
 */
public interface PenaliteRepository extends Repository<Penalite, Long> {

    /**
     * Retourne les pénalités ACTIVES (non payées) d’un adhérent.
     *
     * @param adherentId ID de l'adhérent
     * @return liste des pénalités actives
     */
    List<Penalite> findActivesByAdherentId(Long adherentId);

    /**
     * Retourne toutes les pénalités (actives ou payées) d’un adhérent.
     *
     * @param adherentId ID de l'adhérent
     * @return liste complète des pénalités
     */
    List<Penalite> findByAdherentId(long adherentId);

    /**
     * Retourne les pénalités actives pour l’ensemble des adhérents,
     * sous forme d’une map { adherentId -> nombre de pénalités actives }.
     *
     * @return map des compteurs de pénalités actives
     */
    Map<Long, Integer> findActivePenaltyCountsMap();

    /**
     * Retourne toutes les pénalités actives (utile pour les écrans administrateur).
     *
     * @return liste de toutes les pénalités actives
     */
    List<Penalite> findAllActives();

    /**
     * Marque toutes les pénalités actives d’un adhérent comme payées,
     * en fixant leur date de paiement.
     *
     * @param adherentId ID de l'adhérent
     * @param datePaiement date du paiement
     * @return nombre de pénalités mises à jour
     */
    int reglerToutesPourAdherent(long adherentId, LocalDate datePaiement);
}