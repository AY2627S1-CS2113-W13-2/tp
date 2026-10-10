package seedu.duke.command;

import java.math.BigDecimal;

import seedu.duke.transaction.Transaction;
import seedu.duke.transaction.TransactionList;
import seedu.duke.transaction.TransactionType;
import seedu.duke.ui.Ui;

/**
 * Records money received, e.g. {@code deposit 500 d/Monthly Allowance}.
 */
public class DepositCommand extends Command {
    private final BigDecimal amount;
    private final String description;

    public DepositCommand(BigDecimal amount, String description) {
        this.amount = amount;
        this.description = description;
    }

    /**
     * Creates a deposit without a description, e.g. {@code deposit 500}.
     */
    public DepositCommand(BigDecimal amount) {
        this(amount, "");
    }

    @Override
    public void execute(TransactionList transactions, Ui ui) {
        Transaction deposit = new Transaction(TransactionType.DEPOSIT, amount, description);
        transactions.add(deposit);
        ui.showMessage("Deposit added:", "  " + deposit,
                "You now have " + transactions.size() + " transaction(s).");
    }
}
