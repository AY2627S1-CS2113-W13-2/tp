package finnus.command;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Sets the money the user started with, e.g. {@code opening 1200.50}.
 * Running it again replaces the old value; it is not recorded as a transaction.
 */
public class OpeningBalanceCommand extends Command {
    private static final Logger logger = Logger.getLogger(OpeningBalanceCommand.class.getName());

    private final BigDecimal amount;

    /**
     * Creates the command.
     *
     * @param amount The new opening balance in SGD; must not be negative.
     */
    public OpeningBalanceCommand(BigDecimal amount) {
        assert amount != null && amount.signum() >= 0 : "Opening balance must not be negative";
        this.amount = amount;
    }

    @Override
    public void execute(TransactionList transactions, Ui ui) {
        BigDecimal oldAmount = transactions.getOpeningBalance();
        transactions.setOpeningBalance(amount);
        // The old value is replaced, so record it here in case a balance later looks wrong.
        logger.info("Opening balance changed from " + oldAmount + " to " + amount
                + " with " + transactions.size() + " existing transaction(s)");

        List<String> lines = new ArrayList<>();
        lines.add("Opening balance updated: " + formatMoney(oldAmount) + " -> " + formatMoney(amount));
        lines.add("Your balance is now " + formatMoney(transactions.getBalance()) + ".");
        // Changing the starting point shifts the balance for every existing transaction,
        // so make that visible instead of letting it happen silently.
        if (transactions.size() > 0) {
            lines.add("Note: this also changes the balance for your " + transactions.size()
                    + " existing transaction(s).");
        }
        ui.showMessage(lines);
    }

    /**
     * Formats an amount as {@code $12.30}, or {@code -$12.30} when negative.
     */
    private static String formatMoney(BigDecimal amount) {
        String sign = amount.signum() < 0 ? "-" : "";
        return String.format("%s$%.2f", sign, amount.abs());
    }
}
