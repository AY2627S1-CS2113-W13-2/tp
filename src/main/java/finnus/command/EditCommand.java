package finnus.command;

import java.time.LocalDate;
import java.util.Objects;
import java.util.logging.Logger;

import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

/** Corrects selected fields of an SGD transaction after validating the entire replacement. */
public class EditCommand extends Command {
    private static final Logger logger = Logger.getLogger(EditCommand.class.getName());

    /** Newest-first, one-based number shared by recent, history, find, and delete. */
    private final int index;
    private final EditFields fields;

    /** Creates an edit whose fields have already passed syntax validation. */
    public EditCommand(int index, EditFields fields) {
        this.index = index;
        this.fields = Objects.requireNonNull(fields);
    }

    /**
     * Builds and validates the replacement before changing the ledger.
     *
     * @throws FinNusException If the index or requested fields are invalid for the selected entry.
     */
    @Override
    public void execute(TransactionList transactions, Ui ui) throws FinNusException {
        if (index < 1 || index > transactions.size()) {
            throw new FinNusException("Invalid transaction index.");
        }
        if (!fields.hasChanges()) {
            throw new FinNusException("Supply at least one field to edit.");
        }
        int position = transactions.size() - index;
        Transaction original = transactions.get(position);
        if (original.getForeignAmount().isPresent()) {
            throw new FinNusException("Editing foreign-currency transactions is not supported yet.");
        }
        Transaction replacement = buildReplacement(original, transactions);
        if (hasSameValues(original, replacement)) {
            ui.showMessage("No changes made.");
            return;
        }
        transactions.replace(position, replacement);
        logger.info("Edited transaction at recent-list index " + index);
        ui.showMessage("Transaction updated:", "  Before: " + original, "  After:  " + replacement);
    }

    private Transaction buildReplacement(Transaction original, TransactionList transactions) throws FinNusException {
        String description = fields.description() == null ? original.getDescription() : fields.description();
        if (original.getType() == TransactionType.WITHDRAWAL && description.isBlank()) {
            throw new FinNusException("A withdrawal needs a description so you know what you spent on.");
        }
        if (original.getType() == TransactionType.DEPOSIT && fields.categoryName() != null) {
            throw new FinNusException("Categories are only for spending.");
        }
        String category = fields.categoryName() == null ? original.getCategory().orElse(null)
                : transactions.getCategories().resolve(fields.categoryName());
        Money amount = fields.amount() == null ? original.getAmount() : fields.amount();
        if (!amount.isPositive() || amount.compareTo(Money.MAX_AMOUNT) > 0) {
            throw new FinNusException("Amount must be positive and must not exceed " + Money.MAX_AMOUNT.format());
        }
        LocalDate date = fields.date() == null ? original.getDate().orElse(null) : fields.date();
        return new Transaction(original.getType(), amount, description, category, date);
    }

    private static boolean hasSameValues(Transaction original, Transaction replacement) {
        return original.getAmount().equals(replacement.getAmount())
                && original.getDescription().equals(replacement.getDescription())
                && original.getCategory().equals(replacement.getCategory())
                && original.getDate().equals(replacement.getDate());
    }
}
