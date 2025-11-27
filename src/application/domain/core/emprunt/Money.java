package application.domain.core.emprunt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Représente une valeur monétaire immuable (Value Object DDD).
 *
 * <p>Cette classe encapsule un {@link BigDecimal} (montant) et une
 * {@link Currency} (devise), afin de :</p>
 *
 * <ul>
 *     <li>éviter les erreurs classiques liées aux types primitifs (double)</li>
 *     <li>garantir une précision correcte (2 décimales)</li>
 *     <li>faciliter les opérations monétaires du domaine (addition, soustraction, multiplication)</li>
 *     <li>s’assurer que les devises restent cohérentes entre elles</li>
 * </ul>
 *
 * <h3>Caractéristiques principales</h3>
 * <ul>
 *     <li>Immuable : toutes les opérations retournent un nouvel objet</li>
 *     <li>Comparaison sécurisée : uniquement entre mêmes devises</li>
 *     <li>Helpers pratiques : {@link #euros(double)}, {@link #isPositive()}, etc.</li>
 * </ul>
 *
 * <h3>Exemples d’utilisation</h3>
 * <pre>{@code
 * Money m1 = Money.euros(5.50);
 * Money m2 = Money.euros(2.00);
 * Money total = m1.add(m2); // 7.50 EUR
 * }</pre>
 */
public final class Money implements Comparable<Money> {

    /** Précision standard pour les montants en centimes. */
    private static final int SCALE = 2;

    /** Montant de la valeur monétaire. Toujours normalisé à 2 décimales. */
    private final BigDecimal amount;

    /** Devise associée au montant (ex : EUR). */
    private final Currency currency;

    // ======================================================================
    // ============================== CONSTRUCTEURS ==========================
    // ======================================================================

    /**
     * Constructeur interne.
     * Utilise {@link #of(BigDecimal, Currency)} ou {@link #euros(double)}.
     */
    private Money(BigDecimal amount, Currency currency) {
        this.amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
        this.currency = Objects.requireNonNull(currency, "La devise ne peut pas être nulle");
    }

    /**
     * Factory générique pour créer un Money à partir d'un BigDecimal.
     *
     * @param amount montant
     * @param currency devise
     * @return instance immuable de Money
     */
    public static Money of(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "Le montant ne peut pas être null");
        return new Money(amount, currency);
    }

    /**
     * Factory générique à partir d’un double.
     *
     * @param amount montant
     * @param currency devise
     */
    public static Money of(double amount, Currency currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    /**
     * Factory rapide : crée une valeur en euros à partir d’un double.
     */
    public static Money euros(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("EUR"));
    }

    /**
     * Factory rapide : crée une valeur en euros à partir d’un BigDecimal.
     */
    public static Money euros(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("EUR"));
    }

    // ======================================================================
    // =============================== OPÉRATIONS ============================
    // ======================================================================

    /**
     * Additionne deux valeurs monétaires.
     *
     * @param other autre montant
     * @return nouvelle instance Money (immuable)
     */
    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), currency);
    }

    /**
     * Soustrait une valeur monétaire.
     */
    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), currency);
    }

    /**
     * Multiplie le montant par un entier.
     */
    public Money multiply(long multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), currency);
    }

    /**
     * Multiplie le montant par un BigDecimal.
     */
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), currency);
    }

    /**
     * Retourne le montant opposé.
     */
    public Money negate() {
        return new Money(this.amount.negate(), currency);
    }

    // ======================================================================
    // ============================== PROPRIÉTÉS =============================
    // ======================================================================

    /** @return true si le montant est strictement positif */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /** @return true si le montant est exactement égal à zéro */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /** @return true si le montant est strictement négatif */
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    // ======================================================================
    // ============================== COMPARAISONS ===========================
    // ======================================================================

    /**
     * Compare deux valeurs monétaires (en imposant la même devise).
     */
    @Override
    public int compareTo(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    /** @return true si this > other */
    public boolean greaterThan(Money other) {
        return compareTo(other) > 0;
    }

    /** @return true si this < other */
    public boolean lessThan(Money other) {
        return compareTo(other) < 0;
    }

    // ======================================================================
    // ================================ GETTERS ==============================
    // ======================================================================

    /** @return le montant brut en BigDecimal */
    public BigDecimal amount() {
        return amount;
    }

    /** @return la devise associée */
    public Currency currency() {
        return currency;
    }

    // ======================================================================
    // ================================ HELPERS ==============================
    // ======================================================================

    /**
     * Vérifie que deux montants ont la même devise.
     * @throws IllegalArgumentException si les devises diffèrent
     */
    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Les devises doivent être identiques : " +
                    this.currency + " != " + other.currency
            );
        }
    }

    // ======================================================================
    // ============================ OBJECT CONTRACT ==========================
    // ======================================================================

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.equals(money.amount) &&
               currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
}