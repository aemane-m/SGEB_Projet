package application.domain.core.emprunt;

/**
 * Représente le statut métier d’un {@link Emprunt}, calculé dynamiquement
 * à partir des dates d’emprunt, de retour prévu et de retour effectif.
 *
 * <p>Ce statut n’est jamais stocké en base de données : il est dérivé
 * par la logique métier dans {@link Emprunt#statut(java.time.LocalDate)}.</p>
 *
 * <h3>Signification des statuts</h3>
 * <ul>
 *     <li><strong>EN_COURS</strong> :
 *         l’emprunt n’a pas encore été rendu et la date de retour prévue
 *         n’est pas dépassée.</li>
 *
 *     <li><strong>EN_RETARD</strong> :
 *         l’emprunt n’a pas été rendu et la date de retour prévue est dépassée.</li>
 *
 *     <li><strong>RENDU</strong> :
 *         l’emprunt a été rendu et la date de retour effective
 *         est inférieure ou égale à la date de retour prévue.</li>
 *
 *     <li><strong>RETOURNE_EN_RETARD</strong> :
 *         l’emprunt a été rendu, mais après la date de retour prévue.</li>
 * </ul>
 *
 * <h3>Utilisation typique</h3>
 * <pre>{@code
 * Emprunt e = ...;
 * EmpruntStatus statut = e.statut(LocalDate.now());
 *
 * switch (statut) {
 *     case EN_RETARD -> afficherAlerteRetard();
 *     case RETOURNE_EN_RETARD -> facturerPenalite();
 * }
 * }</pre>
 */
public enum EmpruntStatus {
    /** Emprunt non rendu et encore dans les délais. */
    EN_COURS,

    /** Emprunt non rendu et dont la date de retour prévue est dépassée. */
    EN_RETARD,

    /** Emprunt rendu dans les délais. */
    RENDU,

    /** Emprunt rendu après la date de retour prévue. */
    RETOURNE_EN_RETARD
}