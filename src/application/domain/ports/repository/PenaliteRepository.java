package application.domain.ports.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import application.domain.core.emprunt.Penalite;


/**
 * Port pour la gestion des pénalités.
 */
public interface PenaliteRepository extends Repository<Penalite, Long> {

    /**
     * CORRIGÉ : Renommée pour plus de clarté (Exigence "Statut").
     * Trouve uniquement les pénalités ACTIVES (non payées) 
     * pour un adhérent donné.
     * * @param adherentId L'ID de l'adhérent.
     * @return Une liste des pénalités non payées.
     */
    List<Penalite> findActivesByAdherentId(Long adherentId);

    // Note : findByAdherentId (pour TOUTES les pénalités)
    // pourrait être ajoutée si l'historique complet est nécessaire
    // List<Penalite> findByAdherentId(Long adherentId);
    
    /**
     * NOUVELLE MÉTHODE : Récupère le compte de pénalités actives pour TOUS les adhérents
     * en une seule requête.
     * @return Une Map où la clé est l'adherent_id (emprunteur_id) et la valeur est le nombre
     * de pénalités actives.
     */
    Map<Long, Integer> findActivePenaltyCountsMap();


    /** Récupère toutes les pénalités actives (pour le ViewModel). */
    List<Penalite> findAllActives();


    /**
     * NOUVEAU : Règle toutes les pénalités actives pour un adhérent.
     * @param adherentId L'ID de l'adhérent.
     * @param datePaiement La date à laquelle le paiement est enregistré.
     * @return Le nombre de pénalités (lignes) mises à jour.
     */
    int reglerToutesPourAdherent(long adherentId, LocalDate datePaiement);
}
