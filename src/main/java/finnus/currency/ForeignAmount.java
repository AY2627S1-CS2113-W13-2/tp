//@@author chancheese
package finnus.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

import finnus.money.Money;

/**
 * An amount as the user typed it in another currency, e.g. 1100 JPY, together with
 * the rate used to convert it to SGD.
 *
 * <p>The amount is a {@link BigDecimal} rather than {@link Money} because {@code Money}
 * is always in SGD. Keeping the rate means a transaction's SGD value never changes,
 * even if the user later sets a new rate for that currency.
 *
 * <p>This is a Java record: a short way to declare an immutable class whose fields,
 * constructor, getters (e.g. {@code currencyCode()}) and {@code equals} are generated.
 *
 * @param currencyCode An upper-case ISO 4217 code other than SGD, e.g. "JPY".
 * @param amount The amount in that currency; must be positive.
 * @param rate How many units of the currency 1 SGD bought at the time; must be positive.
 */
public record ForeignAmount(String currencyCode, BigDecimal amount, BigDecimal rate) {
    /** Number of decimal places the SGD value is rounded to (whole cents). */
    private static final int SGD_SCALE = 2;

    /**
     * Checks the values; invalid values here are programming errors, since the parser
     * and storage validate user input first.
     */
    public ForeignAmount {
        assert currencyCode != null : "Currency code must not be null";
        assert !currencyCode.equals(CurrencyRates.BASE_CURRENCY) : "SGD is not a foreign currency";
        assert amount != null && amount.signum() > 0 : "Foreign amount must be positive";
        assert rate != null && rate.signum() > 0 : "Rate must be positive";
    }

    /**
     * Returns the SGD value, rounded to the nearest cent (0.005 rounds up).
     * It can be 0 for a tiny amount, so callers must check before recording it.
     */
    public Money toSgd() {
        return Money.of(amount.divide(rate, SGD_SCALE, RoundingMode.HALF_UP));
    }

    /**
     * Returns the amount as typed, e.g. {@code JPY 1100}.
     */
    @Override
    public String toString() {
        return currencyCode + " " + amount.toPlainString();
    }
}
