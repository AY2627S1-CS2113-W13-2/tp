package finnus.transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import finnus.currency.CurrencyRates;

/**
 * Holds all recorded transactions in the order they were added (oldest first).
 * Other features (balance, recent, delete, storage) should read and modify
 * transactions only through this class.
 */
public class TransactionList {

    private final List<Transaction> transactions = new ArrayList<>();

    /**
     * Exchange rates the user has set, used to convert foreign amounts to SGD.
     */
    private final CurrencyRates rates = new CurrencyRates();

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

    public CurrencyRates getRates() {
        return rates;
    }

    /**
     * Returns a read-only view of all transactions, so callers cannot modify
     *     the list directly.
     */
    public List<Transaction> getAll() {
        return List.copyOf(transactions);
    }

    /**
     * Returns an immutable sub-list of transactions.
     * 
     * @param start The starting index of the sublist (inclusive)
     * @param end The ending index of the sublist (exclusive)
     */
    public List<Transaction> getSubList(int start, int end) {
        assert (start < end && start >= 0 && end <= transactions.size());
        return Collections.unmodifiableList(transactions.subList(start, end));
    }

    /**
     * Returns a read-only list of the most recent transactions of a specified
     *     length. If the list size is smaller than the length, returns the list
     *     itself. The most recent transaction is at index 0
     *
     * @param length The number of transactions to show, starting from the most
     *     recent one.
     */
    public List<Transaction> getRecentTransactions(int length) {
        // Safeguard against lists of size smaller than length
        int fromIndex = Math.max(0, transactions.size() - length);

        // Reverse subList so that the most recent transaction is at the start of the list
        List<Transaction> subList = getSubList(fromIndex, transactions.size()).reversed();
        return subList;
    }

    /**
     * Deletes the transaction at the given zero-based index and returns it.
     *
     * @param index The zero-based index of the transaction to delete.
     * @return The deleted transaction.
     */
    public Transaction delete(int index) {
        return transactions.remove(index);
    }
}
