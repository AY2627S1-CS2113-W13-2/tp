package finnus.command;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import finnus.money.Money;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Sets the money the user started with, e.g. {@code opening 1200.50}.
 * Running it again replaces the old value; it is not recorded as a transaction.
 */
public class OpeningBalanceCommand extends Command {
    private static final Logger logger = Logger.getLogger(OpeningBalanceCommand.class.getName());

    private final Money amount;

    /**
     * Creates the command.
     *
     * @param amount The new opening balance in SGD; must not be negative.
     */
    public OpeningBalanceCommand(Money amount) {
        assert amount != null && !amount.isNegative() : "Opening balance must not be negative";
        this.amount = amount;
    }

    /** Replaces the opening balance and reports the resulting balance change. */
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        Money oldAmount = transactions.getOpeningBalance();
        transactions.setOpeningBalance(amount);
        // The old value is replaced, so record it here in case a balance later looks wrong.
        logger.info("Opening balance changed from " + oldAmount + " to " + amount
                + " with " + transactions.size() + " existing transaction(s)");

        List<String> lines = new ArrayList<>();
        lines.add("Opening balance updated: " + oldAmount.format() + " -> " + amount.format());
        lines.add("Your balance is now " + transactions.getBalance().format() + ".");
        // Changing the starting point shifts the balance for every existing transaction,
        // so make that visible instead of letting it happen silently.
        if (transactions.size() > 0) {
            lines.add("Note: this also changes the balance for your " + transactions.size()
                    + " existing transaction(s).");
        }
        ui.showMessage(lines);
    }

}
