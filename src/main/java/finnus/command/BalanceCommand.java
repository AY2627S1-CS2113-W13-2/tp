package finnus.command;

import java.util.logging.Logger;

import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Displays the user's balance rounded to two decimal places.
 */
public class BalanceCommand extends Command {
    private static final Logger logger = Logger.getLogger(BalanceCommand.class.getName());

    /**
     * Prints the user's balance to 2 decimal places
     * Use half-up rounding (typical financial rounding strategy)
     *
     * @param transactions The list of transactions to get the balance from
     * @param ui The Ui instance
     */
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        logger.fine("Getting balance");

        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        ui.showMessage("Your current balance is: " + transactions.getBalance().format());
    }
}
