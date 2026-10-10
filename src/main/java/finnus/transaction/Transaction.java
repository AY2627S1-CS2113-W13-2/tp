package finnus.transaction;

import java.math.BigDecimal;

/**
 * Represents a single money movement (a deposit or a withdrawal) in SGD.
 * A transaction is immutable: once created, its fields never change.
 *
 * <p>Amounts use {@link BigDecimal} rather than {@code double} because {@code double}
 * cannot store most decimal values exactly (e.g. {@code 0.1 + 0.2} gives {@code 0.30000000000000004}),
 * which would make balances drift over many transactions.
 */
public class Transaction {
    private final TransactionType type;
    private final BigDecimal amount;
    private final String description;

    /**
     * Creates a transaction.
     *
     * @param type Whether money was added or removed.
     * @param amount A strictly positive amount; the sign is implied by {@code type}.
     * @param description A short note, or an empty string if none was given.
     */
    public Transaction(TransactionType type, BigDecimal amount, String description) {
        assert amount != null && amount.signum() > 0 : "Transaction amount must be positive";
        assert description != null : "Description must not be null (use \"\" instead)";
        this.type = type;
        this.amount = amount;
        this.description = description;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns the effect of this transaction on the balance:
     * positive for a deposit, negative for a withdrawal.
     */
    public BigDecimal getSignedAmount() {
        return type == TransactionType.DEPOSIT ? amount : amount.negate();
    }

    @Override
    public String toString() {
        String sign = type == TransactionType.DEPOSIT ? "+" : "-";
        String label = type == TransactionType.DEPOSIT ? "Deposit" : "Withdrawal";
        String note = description.isEmpty() ? "" : " (" + description + ")";
        return String.format("[%s] %s$%.2f%s", label, sign, amount, note);
    }
}
