package application.domain.core.policy;

import java.time.Clock;
import java.time.LocalDate;

import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Money;

/**
 * <p>
 * Interface représentant un <strong>calculateur de pénalités</strong> dans
 * le domaine SGEB.
 * </p>
 *
 * <p>
 * Un {@code PenaltyCalculator} définit comment une pénalité est calculée
 * pour un emprunt en retard.  
 * Il s'agit d'une règle métier pure : <strong>aucun accès BD, aucun accès réseau</strong>,
 * et une dépendance optionnelle à une horloge injectable {@link Clock}.
 * </p>
 *
 * <h2>Principe général</h2>
 * <p>
 * Nombre de jours de retard :
 * </p>
 *
 * <pre>
 * max(0, daysBetween(dateRetourPrevue, dateDeRéférence))
 * </pre>
 *
 * où la date de référence est :
 * <ul>
 *     <li>la date de retour effective, si l’emprunt est déjà rendu</li>
 *     <li>la date de valorisation passée en paramètre (ex : "aujourd’hui") sinon</li>
 * </ul>
 *
 * <p>
 * Le calcul retourne une instance immuable de {@link Money}, qui encapsule le montant
 * et la devise.
 * </p>
 *
 * <h3>Objectif</h3>
 * <p>
 * Permettre différentes stratégies de calcul :
 * </p>
 * <ul>
 *     <li>taux fixe par jour (ex: 0,50 € / jour)</li>
 *     <li>taux variable selon le type de document</li>
 *     <li>plafonnement, pénalité progressive, etc.</li>
 * </ul>
 *
 * @see application.domain.core.emprunt.Emprunt
 * @see application.domain.core.emprunt.Money
 */
public interface PenaltyCalculator {

    // =====================================================================
    //                               CONTRAT
    // =====================================================================

    /**
     * <p>
     * Calcule la pénalité due pour un emprunt donné, valorisée à une date précise.
     * </p>
     *
     * @param emprunt       emprunt concerné (non null)
     * @param valuationDate date à laquelle on évalue la pénalité (non null)
     *
     * @return montant de pénalité (toujours >= 0), jamais null
     */
    Money computePenalty(Emprunt emprunt, LocalDate valuationDate);

    // =====================================================================
    //                         SURCHARGES UTILITAIRES
    // =====================================================================

    /**
     * <p>
     * Utilitaire : calcule la pénalité en utilisant une horloge injectée
     * pour les tests unitaires pour maîtriser le "temps".
     * </p>
     *
     * @param emprunt emprunt concerné
     * @param clock   horloge à utiliser (ex: Clock.fixed(...) en test)
     * @return pénalité calculée
     */
    default Money computePenalty(Emprunt emprunt, Clock clock) {
        return computePenalty(emprunt, LocalDate.now(clock));
    }

    /**
     * <p>
     * Utilitaire : calcule la pénalité en utilisant la date actuelle du système.
     * </p>
     * 
     * @param emprunt emprunt concerné
     * @return pénalité calculée à "aujourd’hui"
     */
    default Money computePenaltyNow(Emprunt emprunt) {
        return computePenalty(emprunt, LocalDate.now());
    }
}