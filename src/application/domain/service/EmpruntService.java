package application.domain.service;

import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Penalite; // Corrigé (Money n'est pas l'objet retourné)

import java.time.LocalDate;
import java.util.List;

/**
 * Interface (Port) pour le service de gestion des emprunts.
 * Contient la logique métier (validation des règles).
 */
public interface EmpruntService {

    /**
     * Tente de créer un nouvel emprunt.
     * Valide toutes les règles métier (disponibilité, statut adhérent, max emprunts).
     * @return L'emprunt créé et sauvegardé.
     * @throws EmpruntException si une règle métier n'est pas respectée.
     */
    Emprunt borrow(long adherentId, long documentId, LocalDate dateEmprunt) throws EmpruntException;

    /**
     * Enregistre le retour d'un document.
     * Calcule et sauvegarde les pénalités si nécessaire.
     * @return La pénalité générée (ou null si pas de retard).
     * @throws EmpruntException si l'emprunt n'existe pas ou est déjà retourné.
     */
    Penalite returnEmprunt(long empruntId, LocalDate dateRetour) throws EmpruntException;

    /** Récupère tous les emprunts (actifs et passés). */
    List<Emprunt> findAll();
    
    /** Récupère uniquement les emprunts en cours. */
    List<Emprunt> findActive();
    
    /** Récupère les emprunts en retard (actifs, date retour dépassée). */
    List<Emprunt> findLate(LocalDate dateActuelle);
    
    /**
     * Vérifie si un document est actuellement emprunté.
     * @param documentId L'ID (Long) du document.
     * @return true si emprunté, false sinon.
     */
    boolean isDocumentEmprunte(Long documentId); // <-- Utilise Long
}


