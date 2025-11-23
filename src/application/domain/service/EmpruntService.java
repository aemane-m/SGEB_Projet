package application.domain.service;

import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Penalite;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>Service métier chargé de gérer le cycle de vie d'un emprunt :</p>
 * <ul>
 *     <li>créer un nouvel emprunt si toutes les règles sont respectées,</li>
 *     <li>enregistrer un retour et générer une pénalité si nécessaire,</li>
 *     <li>interroger les emprunts actifs / en retard / par adhérent,</li>
 *     <li>vérifier si un document est déjà emprunté.</li>
 * </ul>
 *
 * <p>Ce service ne dépend d'aucune technologie particulière
 * (pas de SQLite, pas de JavaFX) : il ne contient que du métier pur.</p>
 */
public interface EmpruntService {

    /**
     * Crée un nouvel emprunt si toutes les règles métier sont respectées.
     *
     * @param adherentId  id de l’adhérent
     * @param documentId  id du document
     * @param dateEmprunt date de début d’emprunt
     * @return l’emprunt créé et sauvegardé
     * @throws EmpruntException si une règle métier bloque l’emprunt
     */
    Emprunt borrow(long adherentId, long documentId, LocalDate dateEmprunt)
            throws EmpruntException;

    /**
     * Enregistre le retour d’un emprunt et génère une pénalité en cas de retard.
     *
     * @param empruntId id de l’emprunt
     * @param dateRetour date effective du retour
     * @return la pénalité générée (ou null s’il n’y a pas de retard)
     * @throws EmpruntException si l’emprunt n’existe pas ou est déjà retourné
     */
    Penalite returnEmprunt(long empruntId, LocalDate dateRetour)
            throws EmpruntException;

    /** @return tous les emprunts (actifs + historiques). */
    List<Emprunt> findAll();

    /** @return uniquement les emprunts en cours. */
    List<Emprunt> findActive();

    /** @return les emprunts en retard à une date donnée. */
    List<Emprunt> findLate(LocalDate dateActuelle);

    /**
     * Vérifie si un document est emprunté actuellement.
     *
     * @param documentId id du document
     * @return true si le document est en cours d’emprunt
     */
    boolean isDocumentEmprunte(Long documentId);

    /** @return tous les emprunts (actifs + passés) d’un adhérent. */
    List<Emprunt> findByAdherentId(long adherentId);

    /** @return seulement les emprunts en cours d’un adhérent. */
    List<Emprunt> findActiveByAdherentId(long adherentId);
}