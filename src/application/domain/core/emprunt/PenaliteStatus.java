package application.domain.core.emprunt;

/**
 * Représente le statut métier d’une {@link Penalite}.
 *
 * <p>Une pénalité suit un cycle de vie simple piloté par les actions
 * de l’utilisateur ou de l’administrateur :</p>
 *
 * <ul>
 *     <li><strong>ACTIVE</strong> :
 *         statut initial utilisé pour la création technique.
 *         Peu utilisé dans le métier, mais présent en base (DEFAULT).</li>
 *
 *     <li><strong>DUE</strong> :
 *         la pénalité est due : elle n’a pas encore été réglée, ni même demandée
 *         par l’adhérent.</li>
 *
 *     <li><strong>PAYMENT_REQUESTED</strong> :
 *         l’adhérent a demandé le paiement (workflow UI).</li>
 *
 *     <li><strong>PAID</strong> :
 *         la pénalité a été réglée à une date validée
 *         (via {@link application.domain.core.emprunt.Penalite#marquerCommePayee}).</li>
 *
 *     <li><strong>CANCELLED</strong> :
 *         un administrateur a annulé la pénalité.</li>
 *         
 * </ul>
 *
 * <h3>Remarque persistance</h3>
 * <p>
 * Le statut par défaut défini par la base SQLite est <strong>ACTIVE</strong>,
 * ce qui correspond à une pénalité nouvellement insérée mais pas encore
 * manipulée par le métier.
 * Le service {@code Penalite} synchronise ensuite avec le statut logique.
 * </p>
 */
public enum PenaliteStatus {

    /** Statut par défaut en base : pénalité enregistrée mais non traitée. */
    ACTIVE,

    /** Pénalité due (non demandée et non payée). */
    DUE,

    /** L’adhérent a demandé à payer (workflow utilisateur). */
    PAYMENT_REQUESTED,

    /** Pénalité réglée. */
    PAID,

    /** Pénalité annulée (souvent par un administrateur). */
    CANCELLED,

    
}