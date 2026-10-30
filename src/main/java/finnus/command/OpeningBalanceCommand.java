//@@author nkyjStar
package finnus.command;

import java.util.logging.Logger;

import finnus.money.Money;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Sets the starting balance during first-run setup; it is not recorded as a transaction.
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

    /** Stores the opening balance without adding it to the transaction list. */
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        transactions.setOpeningBalance(amount);
        logger.info("Opening balance set during first-run setup");
    }
}
//@@author
