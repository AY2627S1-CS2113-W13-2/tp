package finnus.command;

import java.util.logging.Logger;

import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

/**
 * Records money spent, e.g. {@code withdraw 4.50 d/Lunch}.
 */
public class WithdrawCommand extends Command {
    private static final Logger logger = Logger.getLogger(WithdrawCommand.class.getName());

    private final Money amount;
    private final String description;

    /** Creates a withdrawal command with a required description. */
    public WithdrawCommand(Money amount, String description) {
        assert amount != null && amount.isPositive() : "Withdrawal amount must be positive";
        assert description != null : "Withdrawal description must not be null";
        assert !description.isBlank() : "A withdrawal must have a description";
        this.amount = amount;
        this.description = description;
    }

    /** Adds the withdrawal to the ledger and confirms the updated transaction count. */
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        Transaction withdrawal = new Transaction(TransactionType.WITHDRAWAL, amount, description);
        transactions.add(withdrawal);
        logger.info("Added " + withdrawal + ", total transactions: " + transactions.size());
        ui.showMessage("Withdrawal added:", "  " + withdrawal,
                "You now have " + transactions.size() + " transaction(s).");
    }
}
