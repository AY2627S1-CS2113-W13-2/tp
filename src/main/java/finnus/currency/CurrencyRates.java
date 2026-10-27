package finnus.currency;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import finnus.exception.FinNusException;
import finnus.money.Money;

/**
 * Stores the exchange rates entered by the user, each meaning
 * "1 SGD = rate units of that currency", e.g. JPY -> 110.
 *
 * <p>Rates are typed in by the user rather than fetched online, so FinNUS works
 * offline and always gives the same result for the same input.
 */
public class CurrencyRates {
    /**
     * The currency all balances are kept in. It never needs a rate.
     */
    public static final String BASE_CURRENCY = "SGD";

    /**
     * Maps an ISO 4217 code such as "JPY" to its rate. A TreeMap keeps the codes
     * in alphabetical order when they are listed.
     */
    private final Map<String, BigDecimal> rates = new TreeMap<>();

    /**
     * Sets the rate for a currency, replacing any earlier rate for it.
     *
     * @param currencyCode An upper-case ISO 4217 code other than SGD, e.g. "JPY".
     * @param rate How many units of the currency 1 SGD buys; must be positive.
     */
    public void setRate(String currencyCode, BigDecimal rate) {
        assert currencyCode != null : "Currency code must not be null";
        assert !currencyCode.equals(BASE_CURRENCY) : "SGD does not need a rate";
        assert rate != null && rate.signum() > 0 : "Rate must be positive";
        rates.put(currencyCode, rate);
    }

    /** Returns whether a rate has been set for the given currency code. */
    public boolean hasRate(String currencyCode) {
        return rates.containsKey(currencyCode);
    }

    /**
     * Returns the rate for a currency. Call {@link #hasRate} first.
     */
    public BigDecimal getRate(String currencyCode) {
        assert hasRate(currencyCode) : "No rate set for " + currencyCode;
        return rates.get(currencyCode);
    }

    /**
     * Converts an amount typed in another currency to SGD using the stored rate.
     *
     * @param amount The amount in that currency; must be positive.
     * @param currencyCode An upper-case ISO 4217 code other than SGD, e.g. "JPY".
     * @return The amount together with the rate used, ready to be recorded.
     * @throws FinNusException If no rate is set for the currency, or the SGD value
     *     rounds to $0.00 or exceeds {@link Money#MAX_AMOUNT}.
     */
    public ForeignAmount convert(BigDecimal amount, String currencyCode) throws FinNusException {
        if (!hasRate(currencyCode)) {
            throw new FinNusException("No exchange rate set for " + currencyCode + ". Set one first with: rate "
                    + currencyCode + " RATE");
        }
        ForeignAmount foreignAmount = new ForeignAmount(currencyCode, amount, getRate(currencyCode));
        Money sgd = foreignAmount.toSgd();
        if (sgd.isZero()) {
            throw new FinNusException(foreignAmount + " is less than $0.01, which is too small to record.");
        }
        if (sgd.compareTo(Money.MAX_AMOUNT) > 0) {
            throw new FinNusException(foreignAmount + " is " + sgd.format()
                    + ", but an amount must not exceed " + Money.MAX_AMOUNT.format() + ".");
        }
        return foreignAmount;
    }

    /**
     * Returns a read-only view of all rates, sorted by currency code.
     */
    public Map<String, BigDecimal> getAll() {
        return Collections.unmodifiableMap(rates);
    }
}
