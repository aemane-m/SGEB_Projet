package application.domain.ports.repository;

import java.time.LocalDate;
import java.util.List;

// SUPPRIMÉ : Import de Document n'est plus nécessaire ici
// import application.domain.core.document.Document; 
import application.domain.core.emprunt.Emprunt;

/**
 * Port pour la gestion des emprunts.
 */
public interface EmpruntRepository extends Repository<Emprunt, Long> {

    /** Trouve tous les emprunts (actifs ou passés) pour un adhérent donné. */
    List<Emprunt> findByAdherentId(Long adherentId);

    /** Trouve tous les emprunts (actifs ou passés) pour un document donné. */
    List<Emprunt> findByDocumentId(Long documentId);

    /** Trouve tous les emprunts actuellement en cours (non retournés). */
    List<Emprunt> findActiveEmprunts();

    /**
     * Trouve les emprunts en cours (non retournés) dont la date de retour
     * prévue est strictement antérieure à la date fournie.
     * (Exigence 2.3 - Alertes)
     *
     * @param dateLimite La date par rapport à laquelle vérifier le retard (ex: LocalDate.now()).
     * @return La liste des emprunts en retard.
     */
    List<Emprunt> findLateEmprunts(LocalDate dateLimite);
    /**
     * Vérifie si un document spécifique est actuellement emprunté.
     * (c'est-à-dire qu'il a un emprunt non retourné).
     *
     * @param documentId L'ID du document à vérifier.
     * @return true si le document est en cours d'emprunt, false sinon.
     */
    boolean isDocumentEmprunte(long documentId);


}