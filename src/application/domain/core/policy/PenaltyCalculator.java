package application.domain.core.policy;

import java.time.Clock;
import java.time.LocalDate;

import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Money;

/**
 * Contrat de calcul des pénalités de retard.
 * Doit être déterministe et sans I/O (dépendance à l'horloge injectable si besoin).
 */
public interface PenaltyCalculator {

    /**
     * Calcule la pénalité due à une date de valorisation donnée.
     * Règle générale : jours de retard = max(0, daysBetween(dateRetourPrevue, dateValorisationOuEffective)).
     *
     * @param emprunt          l'emprunt concerné (non null)
     * @param valuationDate    la date de valorisation (non null)
     * @return montant de pénalité (>= 0) dans la devise paramétrée par Money
     */
    Money computePenalty(Emprunt emprunt, LocalDate valuationDate);

    /**
     * Surcharge utilitaire basée sur l'horloge passée en dépendance.
     */
    default Money computePenalty(Emprunt emprunt, Clock clock) {
        return computePenalty(emprunt, LocalDate.now(clock));
    }

    /**
     * Surcharge utilitaire basée sur l'horloge système (éviter en tests unitaires).
     */
    default Money computePenaltyNow(Emprunt emprunt) {
        return computePenalty(emprunt, LocalDate.now());
    }
}
