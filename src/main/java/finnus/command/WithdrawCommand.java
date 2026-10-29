package finnus.command;

import java.time.LocalDate;
import java.util.logging.Logger;

import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

/**
 * Records money spent, e.g. {@code withdraw 4.50 d/Lunch t/food}.
 */
public class WithdrawCommand extends Command {
    private static final Logger logger = Logger.getLogger(WithdrawCommand.class.getName());

    private final Money amount;
    private final String description;
    /** The category name as typed after {@code t/}, or null to use the default category. */
    private final String categoryName;
    /** The day of the withdrawal, or null if unknown. */
    private final LocalDate date;

    /** Creates a withdrawal command with a required description and the default category. */
    public WithdrawCommand(Money amount, String description) {
        this(amount, description, null);
    }

    /**
     * Creates a withdrawal command without a date.
     *
     * @param categoryName The category as typed after {@code t/}, or null for the default category.
     */
    public WithdrawCommand(Money amount, String description, String categoryName) {
        this(amount, description, categoryName, null);
    }

    /**
     * Creates a withdrawal command with a required description.
     *
     * @param categoryName The category as typed after {@code t/}, or null for the default category.
     * @param date The day of the withdrawal, or null if unknown.
     */
    public WithdrawCommand(Money amount, String description, String categoryName, LocalDate date) {
        assert amount != null && amount.isPositive() : "Withdrawal amount must be positive";
        assert description != null : "Withdrawal description must not be null";
        assert !description.isBlank() : "A withdrawal must have a description";
        this.amount = amount;
        this.description = description;
        this.categoryName = categoryName;
        this.date = date;
    }

    /**
     * Adds the withdrawal to the ledger and confirms the updated transaction count.
     *
     * @throws FinNusException If the category does not exist.
     */
    @Override
    public void execute(TransactionList transactions, Ui ui) throws FinNusException {
        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        // The category is checked here rather than in the parser, because the list of
        // categories (including custom ones) lives in the TransactionList.
        String category = transactions.getCategories().resolve(categoryName);
        Transaction withdrawal = new Transaction(TransactionType.WITHDRAWAL, amount, description, category, date);
        transactions.add(withdrawal);
        logger.info("Added " + withdrawal + ", total transactions: " + transactions.size());
        ui.showMessage("Withdrawal added:", "  " + withdrawal,
                "You now have " + transactions.size() + " transaction(s).");
    }
}
