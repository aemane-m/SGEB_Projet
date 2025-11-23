package application.domain.service;

import application.domain.core.emprunt.Penalite;
import application.domain.core.emprunt.PenaliteStatus;
import application.domain.ports.repository.PenaliteRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * <p>Implémentation standard du {@link PenaliteService}.</p>
 *
 * <p>Ce service gère toutes les opérations métier autour des pénalités :</p>
 * <ul>
 *     <li>régler une pénalité précise,</li>
 *     <li>régler toutes les pénalités d’un adhérent,</li>
 *     <li>demander un paiement (workflow côté adhérent),</li>
 *     <li>récupérer les pénalités actives d’un adhérent.</li>
 * </ul>
 *
 * <p>Les règles métier principales :</p>
 * <ul>
 *     <li>on ne peut pas régler une pénalité annulée,</li>
 *     <li>si une pénalité est déjà payée, on ne fait rien (idempotence),</li>
 *     <li>la date de paiement appliquée est toujours "aujourd'hui"</li>
 *     <li>le repository gère les mises à jour persistantes.</li>
 * </ul>
 */
public class DefaultPenaliteService implements PenaliteService {

    private final PenaliteRepository penaliteRepository;

    /**
     * Constructeur principal : on injecte simplement le dépôt.
     */
    public DefaultPenaliteService(PenaliteRepository penaliteRepository) {
        this.penaliteRepository = Objects.requireNonNull(
                penaliteRepository, "penaliteRepository ne peut pas être null");
    }

    // ============================================================
    // ==================== RÉGLER UNE PÉNALITÉ ====================
    // ============================================================

    /**
     * Règle une pénalité spécifique.
     *
     * <p>Étapes :</p>
     * <ol>
     *     <li>rechercher la pénalité,</li>
     *     <li>vérifier que le statut permet un paiement,</li>
     *     <li>ignorer si elle est déjà payée (idempotence),</li>
     *     <li>marquer comme payée aujourd’hui,</li>
     *     <li>sauvegarder et retourner.</li>
     * </ol>
     *
     * @param penaliteId identifiant de la pénalité
     * @return la pénalité mise à jour
     */
    @Override
    public Penalite reglerPenalite(long penaliteId) {
        Penalite penalite = penaliteRepository.findById(penaliteId)
                .orElseThrow(() -> new RuntimeException(
                        "Pénalité non trouvée (ID: " + penaliteId + ")"));

        // Règle métier : une pénalité annulée ne peut pas être payée
        if (penalite.getStatus() == PenaliteStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Impossible de régler une pénalité annulée (ID: " + penaliteId + ").");
        }

        // Idempotence : ne rien faire si elle est déjà payée
        if (penalite.estPayee()) {
            return penalite;
        }

        // Marquer comme payée à la date du jour
        penalite.marquerCommePayee(LocalDate.now());

        return penaliteRepository.save(penalite);
    }

    // ============================================================
    // ==================== TOUT RÉGLER POUR UN ADHÉRENT ===========
    // ============================================================

    /**
     * Règle toutes les pénalités actives d’un adhérent.
     *
     * <p>La logique est déléguée au repository, ce qui est optimal
     * (mise à jour en une seule requête).</p>
     *
     * @param adherentId identifiant de l’adhérent
     * @return nombre de pénalités mises à jour
     */
    @Override
    public int reglerToutesPenalites(long adherentId) {
        return penaliteRepository.reglerToutesPourAdherent(adherentId, LocalDate.now());
    }

    // ============================================================
    // ==================== DEMANDE DE PAIEMENT ===================
    // ============================================================

    /**
     * Workflow "demander paiement" (côté adhérent).
     *
     * <p>Cette méthode ne règle pas la pénalité : elle marque simplement
     * un état "demande de paiement". Tu pourras enrichir cette logique
     * plus tard (envoi email, trace d’historique, etc.).</p>
     *
     * @param penaliteId id de la pénalité concernée
     */
    @Override
    public void demanderPaiement(long penaliteId) {
        Penalite penalite = penaliteRepository.findById(penaliteId)
                .orElseThrow(() -> new RuntimeException(
                        "Pénalité non trouvée (ID: " + penaliteId + ")"));

        penalite.demanderPaiement();
        penaliteRepository.save(penalite);
    }

    // ============================================================
    // =================== CONSULTATION ADHÉRENT ===================
    // ============================================================

    /**
     * Retourne toutes les pénalités actives d’un adhérent.
     *
     * <p>Note : ici on retourne uniquement les pénalités non payées
     * (celles visibles pour l’adhérent dans l’espace "Mes pénalités").</p>
     */
    @Override
    public List<Penalite> getPenalitesPourAdherent(long adherentId) {
        return penaliteRepository.findActivesByAdherentId(adherentId);
    }
}