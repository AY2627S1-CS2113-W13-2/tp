package finnus.command;

import java.math.BigDecimal;
import java.util.logging.Logger;

import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Sets the starting balance during first-run setup; it is not recorded as a transaction.
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

    /** Stores the opening balance without adding it to the transaction list. */
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        transactions.setOpeningBalance(amount);
        logger.info("Opening balance set during first-run setup");
    }
}
