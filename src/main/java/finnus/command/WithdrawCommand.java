package finnus.command;

import java.math.BigDecimal;

import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

/**
 * Records money spent, e.g. {@code withdraw 4.50 d/Lunch}.
 */
public class WithdrawCommand extends Command {
    private final BigDecimal amount;
    private final String description;

    public WithdrawCommand(BigDecimal amount, String description) {
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
