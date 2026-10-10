package finnus.transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds all recorded transactions in the order they were added (oldest first).
 * Other features (balance, recent, delete, storage) should read and modify
 * transactions only through this class.
 */
public class TransactionList {
    private final List<Transaction> transactions = new ArrayList<>();

    public void add(Transaction transaction) {
        transactions.add(transaction);
    }

    /**
     * Returns the transaction at the given zero-based index.
     */
    public Transaction get(int index) {
        return transactions.get(index);
    }

    public int size() {
        return transactions.size();
    }

    /**
     * Returns a read-only view of all transactions, so callers cannot modify the list directly.
     */
    public List<Transaction> getAll() {
        return List.copyOf(transactions);
    }
}
