package finnus.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An immutable monetary amount in the application's base currency, SGD.
 *
 * <p>The underlying value is a {@link BigDecimal} so decimal arithmetic remains
 * exact. Values are rounded only when displayed; calculations retain their full
 * precision.
 */
public final class Money implements Comparable<Money> {
    /** A reusable zero amount. */
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    /** Largest monetary amount accepted from user input or storage. */
    public static final Money MAX_AMOUNT = new Money(new BigDecimal("1000000"));

    private static final int DISPLAY_SCALE = 2;
    private static final RoundingMode DISPLAY_ROUNDING = RoundingMode.HALF_UP;

    private final BigDecimal value;

    private Money(BigDecimal value) {
        this.value = Objects.requireNonNull(value, "Money value must not be null");
    }

    /**
     * Creates an amount from an exact decimal value without rounding it.
     *
     * @param value Exact decimal value to wrap.
     * @return An immutable monetary amount.
     */
    public static Money of(BigDecimal value) {
        Objects.requireNonNull(value, "Value should not be null");
        return value.signum() == 0 ? ZERO : new Money(value);
    }

    /** Creates an exact amount from its plain decimal representation. */
    public static Money of(String value) {
        Objects.requireNonNull(value, "Value should not be null");
        return Money.of(new BigDecimal(value));
    }

    /** Creates an exact whole-number amount. */
    public static Money of(long value) {
        Objects.requireNonNull(value, "Value should not be null");
        return Money.of(BigDecimal.valueOf(value));
    }

    /** Returns the sum of this amount and {@code other}. */
    public Money add(Money other) {
        Objects.requireNonNull(value, "Value should not be null");
        return Money.of(value.add(other.value));
    }

    /** Returns this amount with its sign reversed. */
    public Money negate() {
        return Money.of(value.negate());
    }

    /** Returns whether this amount is strictly positive. */
    public boolean isPositive() {
        return value.signum() > 0;
    }

    /** Returns whether this amount is negative. */
    public boolean isNegative() {
        return value.signum() < 0;
    }

    /** Returns whether this amount is zero. */
    public boolean isZero() {
        return value.signum() == 0;
    }

    /**
     * Exposes the exact decimal value at persistence and integration boundaries.
     * Domain calculations should prefer {@link #add(Money)} and {@link #negate()}.
     */
    public BigDecimal toBigDecimal() {
        return value;
    }

    /** Returns the exact, non-scientific decimal representation used by storage. */
    public String toPlainString() {
        return value.toPlainString();
    }

    /** Formats this amount as {@code $12.30}, or {@code -$12.30} when negative. */
    public String format() {
        String sign = isNegative() ? "-" : "";
        String magnitude = value.abs().setScale(DISPLAY_SCALE, DISPLAY_ROUNDING).toPlainString();
        return sign + "$" + magnitude;
    }

    /** Formats this amount with an explicit sign, such as {@code +$12.30} or {@code -$12.30}. */
    public String formatWithSign() {
        String sign = isNegative() ? "-" : "+";
        String magnitude = value.abs().setScale(DISPLAY_SCALE, DISPLAY_ROUNDING).toPlainString();
        return sign + "$" + magnitude;
    }

    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "Amount to compare must not be null");
        return value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Money money)) {
            return false;
        }
        // Monetary equality is numeric: $10 and $10.00 represent the same value.
        return value.compareTo(money.value) == 0;
    }

    @Override
    public int hashCode() {
        return value.stripTrailingZeros().hashCode();
    }

    /** Returns the exact decimal representation; use {@link #format()} for display. */
    @Override
    public String toString() {
        return value.toPlainString();
    }
}
