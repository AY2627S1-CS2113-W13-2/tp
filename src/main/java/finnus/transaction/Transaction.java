package finnus.transaction;

import java.util.Optional;

import finnus.category.CategoryList;
import finnus.currency.ForeignAmount;
import finnus.money.Money;

/**
 * Represents a single money movement (a deposit or a withdrawal) in SGD.
 * A transaction is immutable: once created, its fields never change.
 *
 * <p>Amounts use {@link Money} rather than {@code double} because {@code double}
 * cannot store most decimal values exactly (e.g. {@code 0.1 + 0.2} gives {@code 0.30000000000000004}),
 * which would make balances drift over many transactions.
 */
public class Transaction {
    private final TransactionType type;
    private final Money amount;
    private final String description;

    //@@author chancheese
    /** The amount as typed in another currency, or null if it was typed in SGD. */
    private final ForeignAmount foreignAmount;
    //@@author

    //@@author nabijonzzz
    /** The spending category of a withdrawal, or null for a deposit. */
    private final String category;
    //@@author

    /**
     * Creates a transaction.
     *
     * @param type Whether money was added or removed.
     * @param amount A strictly positive amount; the sign is implied by {@code type}.
     * @param description A short note, or an empty string if none was given.
     */
    public Transaction(TransactionType type, Money amount, String description) {
        this(type, amount, description, defaultCategory(type));
    }

    //@@author nabijonzzz
    /**
     * Creates a transaction typed in SGD with a spending category.
     *
     * @param type Whether money was added or removed.
     * @param amount A strictly positive amount; the sign is implied by {@code type}.
     * @param description A short note, or an empty string if none was given.
     * @param category The category of a withdrawal, or null for a deposit.
     */
    public Transaction(TransactionType type, Money amount, String description, String category) {
        assert type != null : "Transaction type must not be null";
        assert amount != null && amount.isPositive() : "Transaction amount must be positive";
        assert description != null : "Description must not be null (use \"\" instead)";
        assertValidCategory(type, category);
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.foreignAmount = null;
        this.category = category;
    }
    //@@author

    //@@author chancheese
    /**
     * Creates a transaction typed in another currency. Its amount is the SGD value,
     * and the original amount is kept so it can be shown later.
     *
     * @param type Whether money was added or removed.
     * @param foreignAmount The amount as typed; its SGD value must be at least $0.01.
     * @param description A short note, or an empty string if none was given.
     */
    public Transaction(TransactionType type, ForeignAmount foreignAmount, String description) {
        this(type, foreignAmount, description, defaultCategory(type));
    }
    //@@author

    //@@author nabijonzzz
    /**
     * Creates a transaction typed in another currency with a spending category.
     *
     * @param type Whether money was added or removed.
     * @param foreignAmount The amount as typed; its SGD value must be at least $0.01.
     * @param description A short note, or an empty string if none was given.
     * @param category The category of a withdrawal, or null for a deposit.
     */
    public Transaction(TransactionType type, ForeignAmount foreignAmount, String description, String category) {
        assert type != null : "Transaction type must not be null";
        assert foreignAmount != null && foreignAmount.toSgd().isPositive() : "Transaction amount must be positive";
        assert description != null : "Description must not be null (use \"\" instead)";
        assertValidCategory(type, category);
        this.type = type;
        this.amount = foreignAmount.toSgd();
        this.description = description;
        this.foreignAmount = foreignAmount;
        this.category = category;
    }

    /**
     * Returns the category used when none is given: the default category for a
     * withdrawal, and no category for a deposit.
     */
    private static String defaultCategory(TransactionType type) {
        return type == TransactionType.WITHDRAWAL ? CategoryList.DEFAULT_CATEGORY : null;
    }

    /** Checks that every withdrawal has exactly one category and a deposit has none. */
    private static void assertValidCategory(TransactionType type, String category) {
        assert type != TransactionType.DEPOSIT || category == null : "A deposit must not have a category";
        assert type != TransactionType.WITHDRAWAL || CategoryList.isValidName(category)
                : "A withdrawal must have a valid category";
    }
    //@@author

    public TransactionType getType() {
        return type;
    }

    public Money getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    //@@author chancheese
    /**
     * Returns the amount as typed in another currency, or an empty Optional if it was typed in SGD.
     */
    public Optional<ForeignAmount> getForeignAmount() {
        return Optional.ofNullable(foreignAmount);
    }
    //@@author

    //@@author nabijonzzz
    /**
     * Returns the category of a withdrawal, or an empty Optional for a deposit.
     */
    public Optional<String> getCategory() {
        return Optional.ofNullable(category);
    }
    //@@author
    
    /**
     * Returns the effect of this transaction on the balance:
     * positive for a deposit, negative for a withdrawal.
     */
    public Money getSignedAmount() {
        return type == TransactionType.DEPOSIT ? amount : amount.negate();
    }

    /**
     * Returns transaction details as string
     *  
     * Examples
     * [DEPOSIT] +$100.00 (DESCRIPTION)
     * [WITHDRAWAL] -$50.00 (DESCRIPTION)
     * [WITHDRAWAL] -$10.00 (DESCRIPTION) [JPY 1100]
     * [WITHDRAWAL] -$4.50 (DESCRIPTION) #Food
     */
    @Override
    public String toString() {
        String label = type == TransactionType.DEPOSIT ? "Deposit" : "Withdrawal";
        String note = description.isEmpty() ? "" : " (" + description + ")";
        Money signedAmount = getSignedAmount();
        //@@author chancheese
        String original = foreignAmount == null ? "" : " [" + foreignAmount + "]";
        //@@author
        //@@author nabijonzzz
        // '#' marks the category so it cannot be confused with the [JPY 1100] brackets.
        String categoryTag = category == null ? "" : " #" + category;
        //@@author
        return "[" + label + "] " + signedAmount.formatWithSign() + note + original + categoryTag;
    }
}
