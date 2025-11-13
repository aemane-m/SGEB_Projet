package application.domain.core.policy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Money;

/**
 * Pénalité à taux fixe par jour de retard.
 * Par défaut : 0,50 € / jour (à configurer via le constructeur).
 */
public final class FixedRatePenalty implements PenaltyCalculator {

    private final Money perDayAmount;

    /**
     * @param perDayAmount montant fixe par jour (ex: Money.eur("0.50"))
     */
    public FixedRatePenalty(Money perDayAmount) {
        this.perDayAmount = Objects.requireNonNull(perDayAmount, "perDayAmount");
        if (perDayAmount.isNegative()) {
            throw new IllegalArgumentException("perDayAmount must be >= 0");
        }
    }

    @Override
    public Money computePenalty(Emprunt emprunt, LocalDate valuationDate) {
        Objects.requireNonNull(emprunt, "emprunt");
        Objects.requireNonNull(valuationDate, "valuationDate");

        // Détermine la date à comparer : si déjà rendu, on valorise à la date de retour effective,
        // sinon à la date passée en paramètre (now/clock).
        LocalDate reference = emprunt.getDateRetourEffective() != null
                ? emprunt.getDateRetourEffective()
                : valuationDate;

        LocalDate due = emprunt.getDateRetourPrevue();
        long daysLate = Math.max(0, ChronoUnit.DAYS.between(due, reference));

        // pénalité = perDayAmount * daysLate
        return perDayAmount.multiply(daysLate);
    }

    public Money perDayAmount() {
        return perDayAmount;
    }

    // Factory pratique pour la règle SGEB (0,50 €/jour)
    public static FixedRatePenalty sgebDefault(Money moneyFactory) {
        // On suppose que Money dispose d'un helper type Money.of("EUR", 0.50) ou Money.eur("0.50")
        // Ici on prend le montant depuis moneyFactory si tu utilises un pattern factory dédié.
        return new FixedRatePenalty(moneyFactory);
    }
}
