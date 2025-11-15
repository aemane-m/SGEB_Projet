
package application.domain.core.emprunt;

/**
 * Statut "métier" d'un emprunt, calculé à partir des dates.
 *
 * EN_COURS            : emprunt non rendu, encore dans les délais.
 * EN_RETARD           : emprunt non rendu, date de retour prévue dépassée.
 * RENDU               : emprunt rendu dans les délais.
 * RETOURNE_EN_RETARD  : emprunt rendu après la date de retour prévue.
 */
public enum EmpruntStatus {
    EN_COURS,
    EN_RETARD,
    RENDU,
    RETOURNE_EN_RETARD
}