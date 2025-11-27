package application.domain.core.policy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import application.domain.core.emprunt.Emprunt;
import application.domain.core.emprunt.Money;

/**
 * <p>
 * Implémentation de {@link PenaltyCalculator} appliquant une
 * <strong>pénalité à taux fixe par jour de retard</strong>.
 * </p>
 *
 * <p>
 * Le calcul est le suivant :
 * </p>
 * <pre>
 * pénalité = montantParJour × nombreDeJoursDeRetard
 * </pre>
 *
 * <p>
 * Par défaut, le SGEB utilise 0,50 € / jour).
 * </p>
 */
public final class FixedRatePenalty implements PenaltyCalculator {

    /** Montant fixe appliqué par jour de retard. */
    private final Money perDayAmount;

    // ======================================================================
    // =============================== CTOR =================================
    // ======================================================================

    /**
     * Construit un calculateur de pénalité "taux fixe par jour".
     *
     * @param perDayAmount montant fixe appliqué par jour de retard (>= 0)
     * @throws IllegalArgumentException si le montant est négatif
     * @throws NullPointerException si perDayAmount est null
     */
    public FixedRatePenalty(Money perDayAmount) {
        Objects.requireNonNull(perDayAmount, "perDayAmount");
        if (perDayAmount.isNegative()) {
            throw new IllegalArgumentException("perDayAmount must be >= 0");
        }
        this.perDayAmount = perDayAmount;
    }

    // ======================================================================
    // ============================= MÉTIER =================================
    // ======================================================================

    /**
     * Calcule la pénalité due pour un emprunt à une date de valorisation donnée.
     *
     * <p>Logique :</p>
     * <ul>
     *     <li>Si l’emprunt est rendu : calcule le retard par rapport à la date de retour effective.</li>
     *     <li>Sinon : valorise le retard à la date {@code valuationDate}.</li>
     *     <li>Nombre de jours de retard = max(0, dateRef − dateRetourPrevue).</li>
     *     <li>Pénalité = montantParJour × joursDeRetard.</li>
     * </ul>
     *
     * @param emprunt emprunt concerné (non null)
     * @param valuationDate date de référence pour valoriser le retard (non null)
     * @return montant de la pénalité selon une règle simple "montant fixe × jours de retard"
     */
    @Override
    public Money computePenalty(Emprunt emprunt, LocalDate valuationDate) {
        Objects.requireNonNull(emprunt, "emprunt");
        Objects.requireNonNull(valuationDate, "valuationDate");

        // Date de référence = date de retour effective OU la date de valorisation
        LocalDate reference = (emprunt.getDateRetourEffective() != null)
                ? emprunt.getDateRetourEffective()
                : valuationDate;

        LocalDate dueDate = emprunt.getDateRetourPrevue();

        long daysLate = Math.max(0, ChronoUnit.DAYS.between(dueDate, reference));

        return perDayAmount.multiply(daysLate);
    }

    // ======================================================================
    // =============================== GETTER ================================
    // ======================================================================

    /** @return le montant appliqué par jour de retard */
    public Money perDayAmount() {
        return perDayAmount;
    }

    // ======================================================================
    // ============================== FACTORIES ==============================
    // ======================================================================

    /**
     * Factory standard SGEB : pénalité de 0,50 € par jour.
     *
     * @return instance configurée à 0,50 € / jour
     */
    public static FixedRatePenalty sgebDefault() {
        return new FixedRatePenalty(Money.euros(0.50));
    }

    /**
     * Variante : factory utilisant une instance Money fournie 
     * par une "money factory" externe
     *
     * @param money montant par jour
     * @return nouveau calculateur à taux fixe
     */
    public static FixedRatePenalty from(Money money) {
        return new FixedRatePenalty(money);
    }
}