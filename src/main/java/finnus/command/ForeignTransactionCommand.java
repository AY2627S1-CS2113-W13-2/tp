//@@author chancheese
package finnus.command;

import java.math.BigDecimal;
import java.util.logging.Logger;

import finnus.currency.ForeignAmount;
import finnus.exception.FinNusException;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

/**
 * Records a deposit or withdrawal typed in another currency, e.g.
 * {@code withdraw 1100 d/Ramen c/JPY}, converting it to SGD with the stored rate.
 *
 * <p>This is a separate class from {@link DepositCommand} and {@link WithdrawCommand}
 * because the conversion can only happen when the command runs: the rates live in
 * the {@link TransactionList}, which the parser cannot see. Doing the conversion
 * then also means a missing rate is reported as a normal error.
 */
public class ForeignTransactionCommand extends Command {
    private static final Logger logger = Logger.getLogger(ForeignTransactionCommand.class.getName());

    private final TransactionType type;
    private final BigDecimal amount;
    private final String currencyCode;
    private final String description;
    //@@author nabijonzzz
    /** The category name as typed after {@code t/}, or null to use the default category. */
    private final String categoryName;
    //@@author chancheese

    /**
     * Creates the command with the default category.
     *
     * @param type Whether money was added or removed.
     * @param amount The amount in the foreign currency; must be positive.
     * @param currencyCode An upper-case ISO 4217 code other than SGD, e.g. "JPY".
     * @param description A short note, or an empty string if none was given.
     */
    public ForeignTransactionCommand(TransactionType type, BigDecimal amount, String currencyCode,
            String description) {
        this(type, amount, currencyCode, description, null);
    }

    //@@author nabijonzzz
    /**
     * Creates the command.
     *
     * @param type Whether money was added or removed.
     * @param amount The amount in the foreign currency; must be positive.
     * @param currencyCode An upper-case ISO 4217 code other than SGD, e.g. "JPY".
     * @param description A short note, or an empty string if none was given.
     * @param categoryName The category typed after {@code t/} for a withdrawal, or null for
     *     the default category. Must be null for a deposit.
     */
    public ForeignTransactionCommand(TransactionType type, BigDecimal amount, String currencyCode,
            String description, String categoryName) {
        //@@author chancheese
        assert type != null : "Transaction type must not be null";
        assert amount != null && amount.signum() > 0 : "Foreign amount must be positive";
        assert currencyCode != null : "Currency code must not be null";
        assert description != null : "Description must not be null";
        assert type == TransactionType.DEPOSIT || !description.isBlank() : "A withdrawal must have a description";
        this.type = type;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.description = description;
        //@@author nabijonzzz
        assert type == TransactionType.WITHDRAWAL || categoryName == null : "A deposit must not have a category";
        this.categoryName = categoryName;
        //@@author chancheese
    }

    /**
     * Converts the amount to SGD, adds the transaction and shows the rate that was used.
     *
     * @throws FinNusException If no rate is set for the currency, the SGD value is out of range,
     *     or the category does not exist.
     */
    @Override
    public void execute(TransactionList transactions, Ui ui) throws FinNusException {
        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        //@@author nabijonzzz
        String category = type == TransactionType.WITHDRAWAL
                ? transactions.getCategories().resolve(categoryName) : null;
        //@@author chancheese
        ForeignAmount foreignAmount = transactions.getRates().convert(amount, currencyCode);
        Transaction transaction = new Transaction(type, foreignAmount, description, category);
        transactions.add(transaction);
        logger.info("Added " + transaction + " at rate " + foreignAmount.rate().toPlainString()
                + ", total transactions: " + transactions.size());

        String label = type == TransactionType.DEPOSIT ? "Deposit" : "Withdrawal";
        ui.showMessage(label + " added:", "  " + transaction,
                "Converted at " + RateCommand.formatRate(currencyCode, foreignAmount.rate()) + ".",
                "You now have " + transactions.size() + " transaction(s).");
    }
}
