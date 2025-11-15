package application.domain.core.emprunt;

/**
 * Statut métier d'une pénalité.
 *
 * DUE               : due, non encore en processus de paiement.
 * PAYMENT_REQUESTED : l'adhérent a demandé le paiement (en attente de validation admin).
 * PAID              : pénalité réglée.
 * CANCELLED         : pénalité annulée.
 */
public enum PenaliteStatus {
    DUE,
    PAYMENT_REQUESTED,
    PAID,
    CANCELLED, 
    DEMANDE
}