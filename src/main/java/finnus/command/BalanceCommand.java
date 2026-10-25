package finnus.command;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.logging.Logger;

import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Represents the command to get the user's balance
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

        BigDecimal balance = transactions.getBalance();
        ui.showMessage("Your current balance is: $" + balance.setScale(2, RoundingMode.HALF_UP));
    }
}
