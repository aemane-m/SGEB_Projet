package application.domain.ports.repository;

import java.time.LocalDate;
import java.util.List;

import application.domain.core.emprunt.Emprunt;

/**
 * Port de persistance pour les {@link Emprunt}.
 *
 * <p>
 * Fournit des méthodes pour récupérer les emprunts par adhérent,
 * par document, ou selon leur statut (en cours, en retard).
 * </p>
 */
public interface EmpruntRepository extends Repository<Emprunt, Long> {

    /**
     * Retourne tous les emprunts (actifs ou passés) d’un adhérent.
     *
     * @param adherentId identifiant de l’adhérent
     * @return liste de ses emprunts
     */
    List<Emprunt> findByAdherentId(Long adherentId);

    /**
     * Retourne tous les emprunts (actifs ou passés) concernant un document donné.
     *
     * @param documentId identifiant du document
     * @return liste des emprunts pour ce document
     */
    List<Emprunt> findByDocumentId(Long documentId);

    /**
     * Retourne tous les emprunts actuellement en cours (non retournés).
     *
     * @return liste des emprunts en cours
     */
    List<Emprunt> findActiveEmprunts();

    /**
     * Retourne les emprunts en cours (non retournés) dont la date de
     * retour prévue est antérieure à {@code dateLimite}.
     *
     * @param dateLimite date de référence
     * @return liste des emprunts en retard
     */
    List<Emprunt> findLateEmprunts(LocalDate dateLimite);

    /**
     * Indique si un document est actuellement emprunté
     * (c’est-à-dire s’il existe au moins un emprunt en cours pour ce document).
     *
     * @param documentId identifiant du document
     * @return {@code true} si le document est emprunté, {@code false} sinon
     */
    boolean isDocumentEmprunte(long documentId);
}