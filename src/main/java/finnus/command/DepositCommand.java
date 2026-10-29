package finnus.command;

import java.time.LocalDate;
import java.util.logging.Logger;

import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

/**
 * Records money received, e.g. {@code deposit 500 d/Monthly Allowance}.
 */
public class DepositCommand extends Command {
    private static final Logger logger = Logger.getLogger(DepositCommand.class.getName());

    private final Money amount;
    private final String description;
    /** The day of the deposit, or null if unknown. */
    private final LocalDate date;

    /** Creates a deposit command with the supplied amount and description, without a date. */
    public DepositCommand(Money amount, String description) {
        this(amount, description, null);
    }

    /**
     * Creates a deposit command.
     *
     * @param date The day of the deposit, or null if unknown.
     */
    public DepositCommand(Money amount, String description, LocalDate date) {
        assert amount != null && amount.isPositive() : "Deposit amount must be positive";
        assert description != null : "Deposit description must not be null";
        this.amount = amount;
        this.description = description;
        this.date = date;
    }

    /**
     * Creates a deposit without a description, e.g. {@code deposit 500}.
     */
    public DepositCommand(Money amount) {
        this(amount, "");
    }

    /** Adds the deposit to the ledger and confirms the updated transaction count. */
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        Transaction deposit = new Transaction(TransactionType.DEPOSIT, amount, description, null, date);
        transactions.add(deposit);
        logger.info("Added " + deposit + ", total transactions: " + transactions.size());
        ui.showMessage("Deposit added:", "  " + deposit,
                "You now have " + transactions.size() + " transaction(s).");
    }
}
