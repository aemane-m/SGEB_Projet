package application.domain.core.emprunt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value Object représentant une somme d'argent immuable.
 * Encapsule BigDecimal et Currency pour éviter les erreurs d'arrondi
 * et simplifie les opérations monétaires dans le domaine.
 */
public final class Money implements Comparable<Money> {

    private static final int SCALE = 2; // précision standard : centimes
    private final BigDecimal amount;
    private final Currency currency;

    // --------------------
    //   Constructeurs
    // --------------------

    private Money(BigDecimal amount, Currency currency) {
        this.amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(double amount, Currency currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money euros(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("EUR"));
    }

    public static Money euros(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("EUR"));
    }

    // --------------------
    //   Opérations
    // --------------------

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), currency);
    }

    public Money multiply(long multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), currency);
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), currency);
    }

    public Money negate() {
        return new Money(this.amount.negate(), currency);
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    // --------------------
    //   Comparaisons
    // --------------------

    public int compareTo(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    public boolean greaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public boolean lessThan(Money other) {
        return compareTo(other) < 0;
    }

    // --------------------
    //   Getters
    // --------------------

    public BigDecimal amount() {
        return amount;
    }

    public Currency currency() {
        return currency;
    }

    // --------------------
    //   Helpers
    // --------------------

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Les devises doivent être identiques : " + this.currency + " != " + other.currency);
        }
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.equals(money.amount) && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
}
