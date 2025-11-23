package application.domain.service;

import application.domain.core.emprunt.Penalite;

import java.util.List;

/**
 * <p>Service métier dédié à la gestion des pénalités :</p>
 * <ul>
 *     <li>régler une pénalité,</li>
 *     <li>régler toutes les pénalités d’un adhérent,</li>
 *     <li>demander un paiement (workflow adhérent),</li>
 *     <li>consulter les pénalités actives d’un adhérent.</li>
 * </ul>
 *
 * <p>Comme toujours dans le domaine, aucune dépendance technique
 * (BD, UI, framework).</p>
 */
public interface PenaliteService {

    /**
     * Marque une pénalité comme payée.
     *
     * @param penaliteId id de la pénalité
     * @return la pénalité mise à jour
     * @throws RuntimeException si introuvable, annulée ou déjà payée (selon logique métier)
     */
    Penalite reglerPenalite(long penaliteId);

    /**
     * Règle toutes les pénalités actives d’un adhérent.
     *
     * @param adherentId id de l’adhérent
     * @return nombre de pénalités mises à jour
     */
    int reglerToutesPenalites(long adherentId);

    /**
     * L’adhérent signale qu'il souhaite payer une pénalité.
     * Cela change le statut métier (ex: PAYMENT_REQUESTED).
     *
     * @param penaliteId id de la pénalité
     */
    void demanderPaiement(long penaliteId);

    /**
     * Récupère les pénalités visibles dans l’espace adhérent.
     */
    List<Penalite> getPenalitesPourAdherent(long adherentId);
}