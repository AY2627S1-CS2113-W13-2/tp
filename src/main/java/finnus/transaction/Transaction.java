package finnus.transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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
    //@@author nabijonzzz
    /**
     * Shows dates such as {@code 30 Oct 2026}. English month names are used on every computer,
     * so the output does not depend on the user's language settings.
     */
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    //@@author

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

    /** The day the transaction happened, or null if it was saved before dates were recorded. */
    private final LocalDate date;
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
        this(type, amount, description, category, null);
    }

    /**
     * Creates a transaction typed in SGD with a spending category and a date.
     *
     * @param type Whether money was added or removed.
     * @param amount A strictly positive amount; the sign is implied by {@code type}.
     * @param description A short note, or an empty string if none was given.
     * @param category The category of a withdrawal, or null for a deposit.
     * @param date The day it happened, or null if unknown.
     */
    public Transaction(TransactionType type, Money amount, String description, String category, LocalDate date) {
        assert type != null : "Transaction type must not be null";
        assert amount != null && amount.isPositive() : "Transaction amount must be positive";
        assert description != null : "Description must not be null (use \"\" instead)";
        assertValidCategory(type, category);
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.foreignAmount = null;
        this.category = category;
        this.date = date;
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
        this(type, foreignAmount, description, category, null);
    }

    /**
     * Creates a transaction typed in another currency with a spending category and a date.
     *
     * @param type Whether money was added or removed.
     * @param foreignAmount The amount as typed; its SGD value must be at least $0.01.
     * @param description A short note, or an empty string if none was given.
     * @param category The category of a withdrawal, or null for a deposit.
     * @param date The day it happened, or null if unknown.
     */
    public Transaction(TransactionType type, ForeignAmount foreignAmount, String description, String category,
            LocalDate date) {
        assert type != null : "Transaction type must not be null";
        assert foreignAmount != null && foreignAmount.toSgd().isPositive() : "Transaction amount must be positive";
        assert description != null : "Description must not be null (use \"\" instead)";
        assertValidCategory(type, category);
        this.type = type;
        this.amount = foreignAmount.toSgd();
        this.description = description;
        this.foreignAmount = foreignAmount;
        this.category = category;
        this.date = date;
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

    /**
     * Returns the day the transaction happened, or an empty Optional if it was saved
     * before dates were recorded.
     */
    public Optional<LocalDate> getDate() {
        return Optional.ofNullable(date);
    }
    //@@author

    //@@author chancheese
    /**
     * Formats a date the same way transactions show it, e.g. {@code 30 Oct 2026}, so other
     * features such as the allowance summary show dates consistently.
     */
    public static String formatDate(LocalDate date) {
        assert date != null : "Date must not be null";
        return date.format(DISPLAY_DATE);
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
     * [30 Oct 2026] [WITHDRAWAL] -$4.50 (DESCRIPTION) #Food
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
        String datePrefix = date == null ? "" : "[" + date.format(DISPLAY_DATE) + "] ";
        //@@author
        return datePrefix + "[" + label + "] " + signedAmount.formatWithSign() + note + original + categoryTag;
    }
}
