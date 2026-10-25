package finnus.transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import finnus.currency.CurrencyRates;
import finnus.money.Money;

/**
 * Holds all recorded transactions in the order they were added (oldest first),
 * together with the opening balance the ledger starts from.
 * Other features (balance, recent, delete, storage) should read and modify
 * transactions only through this class.
 */
public class TransactionList {

    private final List<Transaction> transactions = new ArrayList<>();

    /**
     * The money the user had before recording any transactions, in SGD.
     * It is a starting point, not a transaction, so it never appears in the list
     * and does not shift the indices used by {@code recent} and {@code delete}.
     * Defaults to 0 for users who never set it.
     */
    private Money openingBalance = Money.ZERO;

    /**
     * Exchange rates the user has set, used to convert foreign amounts to SGD.
     */
    private final CurrencyRates rates = new CurrencyRates();

    /** Adds a transaction at the end of the ledger, preserving entry order. */
    public void add(Transaction transaction) {
        int oldSize = transactions.size();
        transactions.add(transaction);
        assert transactions.size() == oldSize + 1 : "Adding a transaction must increase the list size";
    }

    /**
     * Returns the transaction at the given zero-based index.
     */
    public Transaction get(int index) {
        return transactions.get(index);
    }

    /** Returns the number of recorded transactions. */
    public int size() {
        return transactions.size();
    }

    /** Returns the rate collection used to convert foreign currencies. */
    public CurrencyRates getRates() {
        return rates;
    }

    /**
     * Returns a read-only view of all transactions, so callers cannot modify
     * the list directly.
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
        assert start >= 0 && start < end && end <= transactions.size()
                : "Sublist bounds must be within the transaction list";
        return Collections.unmodifiableList(transactions.subList(start, end));
    }

    /**
     * Returns a read-only list of the most recent transactions of a specified
     * length. If the list size is smaller than the length, returns the list
     * itself. The most recent transaction is at index 0
     *
     * @param length The number of transactions to show, starting from the most
     *     recent one.
     * @return An immutable list ordered from newest to oldest.
     */
    public List<Transaction> getRecentTransactions(int length) {
        // Safeguard against empty transactions list
        if (transactions.isEmpty()) {
            return Collections.emptyList();
        }

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
        int oldSize = transactions.size();
        Transaction removed = transactions.remove(index);
        assert transactions.size() == oldSize - 1 : "Deleting a transaction must reduce the list size";
        return removed;
    }

    /** Returns the starting balance before any transactions are applied. */
    public Money getOpeningBalance() {
        return openingBalance;
    }

    /**
     * Replaces the opening balance. Because the balance is always recalculated from
     * the opening balance, changing it shifts the current balance by the same difference.
     *
     * @param openingBalance The new opening balance; must not be negative.
     */
    public void setOpeningBalance(Money openingBalance) {
        assert openingBalance != null && !openingBalance.isNegative() : "Opening balance must not be negative";
        this.openingBalance = openingBalance;
    }

    /**
     * Returns the current balance: the opening balance plus all deposits minus all withdrawals.
     * This is the single place the balance is calculated, so every feature reports the same figure.
     */
    public Money getBalance() {
        return transactions.stream()
                .map(Transaction::getSignedAmount)
                .reduce(openingBalance, Money::add);
    }
}
