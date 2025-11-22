package application.domain.core.emprunt;

/**
 * Statut métier d'une pénalité.
 *
 * ACTIVE            : pénalité ouverte / en cours.
 * DUE               : due, non encore en processus de paiement.
 * PAYMENT_REQUESTED : demande de paiement envoyée.
 * PAID              : pénalité réglée.
 * CANCELLED         : pénalité annulée.
 */
public enum PenaliteStatus {

    // Aligné sur DatabaseInitializer (DEFAULT 'ACTIVE')
    ACTIVE,

    // Aligné sur métiers (pénalité due mais pas encore en demande)
    DUE,

    // L'adhérent a demandé à payer
    PAYMENT_REQUESTED,

    // Marquée comme réglée
    PAID,

    // Annulée par un admin
    CANCELLED,
    
    DEMANDE
}
