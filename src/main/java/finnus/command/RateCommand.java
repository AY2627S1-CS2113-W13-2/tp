package finnus.command;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import finnus.currency.CurrencyRates;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Sets an exchange rate, e.g. {@code rate JPY 110} for 1 SGD = 110 JPY,
 * or lists all rates when given no arguments.
 */
public class RateCommand extends Command {
    private static final String RATE_EXAMPLE = "rate JPY 110";

    /** The currency to set, or null to list all rates instead. */
    private final String currencyCode;
    private final BigDecimal rate;

    /**
     * Creates a command that lists all rates.
     */
    public RateCommand() {
        this.currencyCode = null;
        this.rate = null;
    }

    /**
     * Creates a command that sets the rate for one currency.
     *
     * @param currencyCode An upper-case ISO 4217 code other than SGD, e.g. "JPY".
     * @param rate How many units of the currency 1 SGD buys; must be positive.
     */
    public RateCommand(String currencyCode, BigDecimal rate) {
        assert currencyCode != null && rate != null : "Use RateCommand() to list rates";
        this.currencyCode = currencyCode;
        this.rate = rate;
    }

    /** Lists the configured rates or updates the selected currency rate. */
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        CurrencyRates rates = transactions.getRates();
        if (currencyCode == null) {
            listRates(rates, ui);
        } else {
            setRate(rates, ui);
        }
    }

    /** Stores the requested rate and reports its previous value when one existed. */
    private void setRate(CurrencyRates rates, Ui ui) {
        String message = "Exchange rate set: " + formatRate(currencyCode, rate);
        if (rates.hasRate(currencyCode)) {
            message += " (was " + formatRate(currencyCode, rates.getRate(currencyCode)) + ")";
        }
        rates.setRate(currencyCode, rate);
        ui.showMessage(message);
    }

    /** Displays all configured rates in the order supplied by the rate collection. */
    private static void listRates(CurrencyRates rates, Ui ui) {
        Map<String, BigDecimal> allRates = rates.getAll();
        if (allRates.isEmpty()) {
            ui.showMessage("No exchange rates set yet. Set one with, e.g.: " + RATE_EXAMPLE);
            return;
        }
        List<String> lines = new ArrayList<>();
        lines.add("Exchange rates:");
        for (Map.Entry<String, BigDecimal> entry : allRates.entrySet()) {
            lines.add("  " + formatRate(entry.getKey(), entry.getValue()));
        }
        ui.showMessage(lines);
    }

    /**
     * Formats a rate as {@code 1 SGD = 110 JPY}, dropping trailing zeros such as in 110.00.
     */
    static String formatRate(String currencyCode, BigDecimal rate) {
        return "1 " + CurrencyRates.BASE_CURRENCY + " = " + rate.stripTrailingZeros().toPlainString()
                + " " + currencyCode;
    }
}
