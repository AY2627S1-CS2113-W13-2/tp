package finnus.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.logging.Logger;

import finnus.currency.ForeignAmount;
import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

//@@author naieel04
/** Corrects selected transaction fields after validating the entire replacement, including saved FX values. */
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
        Transaction replacement = buildReplacement(original, transactions);
        if (hasSameValues(original, replacement)) {
            ui.showMessage("No changes made.");
            return;
        }
        if (replacement.getType() == TransactionType.WITHDRAWAL
                && !transactions.getSpendingAlert().confirmEdit(transactions, position, replacement, ui)) {
            return;
        }
        transactions.replace(position, replacement);
        logger.info("Edited transaction at recent-list index " + index);
        ui.showMessage("Transaction updated:", "  Before: " + original, "  After:  " + replacement);
        replacement.getForeignAmount().ifPresent(foreign -> ui.showMessage("Converted at "
                + RateCommand.formatRate(foreign.currencyCode(), foreign.rate()) + "."));
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
        LocalDate date = fields.date() == null ? original.getDate().orElse(null) : fields.date();
        if (original.getForeignAmount().isPresent()) {
            ForeignAmount foreign = correctedForeignAmount(original.getForeignAmount().orElseThrow());
            validateAmount(foreign.toSgd());
            return new Transaction(original.getType(), foreign, description, category, date);
        }
        if (fields.rate() != null) {
            throw new FinNusException("A saved exchange rate can only be edited for a foreign-currency transaction.");
        }
        Money amount = fields.amount() == null ? original.getAmount() : Money.of(fields.amount());
        validateAmount(amount);
        return new Transaction(original.getType(), amount, description, category, date);
    }

    /** Uses only historical values and explicit corrections, never the global rate collection. */
    private ForeignAmount correctedForeignAmount(ForeignAmount original) {
        if (fields.amount() == null && fields.rate() == null) {
            return original;
        }
        BigDecimal amount = fields.amount() == null ? original.amount() : fields.amount();
        BigDecimal rate = fields.rate() == null ? original.rate() : fields.rate();
        return new ForeignAmount(original.currencyCode(), amount, rate);
    }

    private static void validateAmount(Money amount) throws FinNusException {
        if (!amount.isPositive()) {
            throw new FinNusException("The resulting SGD amount must be at least $0.01.");
        }
        if (amount.compareTo(Money.MAX_AMOUNT) > 0) {
            throw new FinNusException("Amount must not exceed " + Money.MAX_AMOUNT.format());
        }
    }

    private static boolean hasSameValues(Transaction original, Transaction replacement) {
        return original.getAmount().equals(replacement.getAmount())
                && original.getDescription().equals(replacement.getDescription())
                && original.getCategory().equals(replacement.getCategory())
                && original.getDate().equals(replacement.getDate())
                && hasSameForeignValues(original, replacement);
    }

    /** BigDecimal scale changes alone are not financial corrections. */
    private static boolean hasSameForeignValues(Transaction original, Transaction replacement) {
        if (original.getForeignAmount().isEmpty()) {
            return replacement.getForeignAmount().isEmpty();
        }
        ForeignAmount before = original.getForeignAmount().orElseThrow();
        ForeignAmount after = replacement.getForeignAmount().orElseThrow();
        return before.currencyCode().equals(after.currencyCode())
                && before.amount().compareTo(after.amount()) == 0
                && before.rate().compareTo(after.rate()) == 0;
        //@@author
    }
}
