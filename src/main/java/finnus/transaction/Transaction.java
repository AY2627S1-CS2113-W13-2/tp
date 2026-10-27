package finnus.transaction;

import java.util.Optional;

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

    /**
     * Creates a transaction.
     *
     * @param type Whether money was added or removed.
     * @param amount A strictly positive amount; the sign is implied by {@code type}.
     * @param description A short note, or an empty string if none was given.
     */
    public Transaction(TransactionType type, Money amount, String description) {
        assert type != null : "Transaction type must not be null";
        assert amount != null && amount.isPositive() : "Transaction amount must be positive";
        assert description != null : "Description must not be null (use \"\" instead)";
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.foreignAmount = null;
    }

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
        assert type != null : "Transaction type must not be null";
        assert foreignAmount != null && foreignAmount.toSgd().isPositive() : "Transaction amount must be positive";
        assert description != null : "Description must not be null (use \"\" instead)";
        this.type = type;
        this.amount = foreignAmount.toSgd();
        this.description = description;
        this.foreignAmount = foreignAmount;
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
     */
    @Override
    public String toString() {
        String label = type == TransactionType.DEPOSIT ? "Deposit" : "Withdrawal";
        String note = description.isEmpty() ? "" : " (" + description + ")";
        Money signedAmount = getSignedAmount();
        //@@author chancheese
        String original = foreignAmount == null ? "" : " [" + foreignAmount + "]";
        //@@author
        return "[" + label + "] " + signedAmount.formatWithSign() + note + original;
    }
}
