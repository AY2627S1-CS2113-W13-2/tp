package seedu.duke.command;

import seedu.duke.transaction.Transaction;
import seedu.duke.transaction.TransactionList;
import seedu.duke.transaction.TransactionType;
import seedu.duke.ui.Ui;

/**
 * Records money spent, e.g. {@code withdraw 4.50 d/Lunch}.
 */
public class WithdrawCommand extends Command {
    private final double amount;
    private final String description;

    public WithdrawCommand(double amount, String description) {
        assert !description.isBlank() : "A withdrawal must have a description";
        this.amount = amount;
        this.description = description;
    }

    @Override
    public void execute(TransactionList transactions, Ui ui) {
        Transaction withdrawal = new Transaction(TransactionType.WITHDRAWAL, amount, description);
        transactions.add(withdrawal);
        ui.showMessage("Withdrawal added:", "  " + withdrawal,
                "You now have " + transactions.size() + " transaction(s).");
    }
}
