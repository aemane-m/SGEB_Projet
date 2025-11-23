package application.domain.ports.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import application.domain.core.emprunt.Penalite;

/**
 * Port de persistance pour les {@link Penalite}.
 *
 * <p>
 * Permet de récupérer, lister et mettre à jour les pénalités
 * associées aux adhérents.
 * </p>
 */
public interface PenaliteRepository extends Repository<Penalite, Long> {

    /**
     * Retourne les pénalités <strong>actives</strong> (non payées) d’un adhérent.
     *
     * @param adherentId identifiant de l’adhérent
     * @return liste des pénalités actives
     */
    List<Penalite> findActivesByAdherentId(Long adherentId);

    /**
     * Retourne toutes les pénalités (actives ou payées) d’un adhérent.
     *
     * @param adherentId identifiant de l’adhérent
     * @return liste complète de ses pénalités
     */
    List<Penalite> findByAdherentId(long adherentId);

    /**
     * Retourne, pour chaque adhérent, le nombre de pénalités actives.
     *
     * @return une map { idAdherent -> nombreDePénalitésActives }
     */
    Map<Long, Integer> findActivePenaltyCountsMap();

    /**
     * Retourne toutes les pénalités actives (tous adhérents confondus).
     *
     * @return liste des pénalités actives
     */
    List<Penalite> findAllActives();

    /**
     * Marque comme payées toutes les pénalités actives d’un adhérent,
     * en fixant leur date de paiement.
     *
     * @param adherentId  identifiant de l’adhérent
     * @param datePaiement date de paiement à appliquer
     * @return nombre de pénalités mises à jour
     */
    int reglerToutesPourAdherent(long adherentId, LocalDate datePaiement);
}