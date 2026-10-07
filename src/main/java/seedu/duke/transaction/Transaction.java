package seedu.duke.transaction;

/**
 * Represents a single money movement (a deposit or a withdrawal) in SGD.
 * A transaction is immutable: once created, its fields never change.
 */
public class Transaction {
    private final TransactionType type;
    private final double amount;
    private final String description;

    /**
     * Creates a transaction.
     *
     * @param type Whether money was added or removed.
     * @param amount A strictly positive amount; the sign is implied by {@code type}.
     * @param description A short note, or an empty string if none was given.
     */
    public Transaction(TransactionType type, double amount, String description) {
        assert amount > 0 : "Transaction amount must be positive";
        assert description != null : "Description must not be null (use \"\" instead)";
        this.type = type;
        this.amount = amount;
        this.description = description;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns the effect of this transaction on the balance:
     * positive for a deposit, negative for a withdrawal.
     */
    public double getSignedAmount() {
        return type == TransactionType.DEPOSIT ? amount : -amount;
    }

    @Override
    public String toString() {
        String sign = type == TransactionType.DEPOSIT ? "+" : "-";
        String label = type == TransactionType.DEPOSIT ? "Deposit" : "Withdrawal";
        String note = description.isEmpty() ? "" : " (" + description + ")";
        return String.format("[%s] %s$%.2f%s", label, sign, amount, note);
    }
}
