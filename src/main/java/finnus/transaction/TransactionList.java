package finnus.transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import finnus.currency.CurrencyRates;

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
    private BigDecimal openingBalance = BigDecimal.ZERO;

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
        assert (start < end && start >= 0 && end <= transactions.size());
        return Collections.unmodifiableList(transactions.subList(start, end));
    }

    /**
     * Returns a read-only list of the most recent transactions of a specified
     * length. If the list size is smaller than the length, returns the list
     * itself. The most recent transaction is at index 0
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

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    /**
     * Replaces the opening balance. Because the balance is always recalculated from
     * the opening balance, changing it shifts the current balance by the same difference.
     *
     * @param openingBalance The new opening balance; must not be negative.
     */
    public void setOpeningBalance(BigDecimal openingBalance) {
        assert openingBalance != null && openingBalance.signum() >= 0 : "Opening balance must not be negative";
        this.openingBalance = openingBalance;
    }

    /**
     * Returns the current balance: the opening balance plus all deposits minus all withdrawals.
     * This is the single place the balance is calculated, so every feature reports the same figure.
     */
    public BigDecimal getBalance() {
        return transactions.stream()
                .map(Transaction::getSignedAmount)
                .reduce(openingBalance, BigDecimal::add);
    }
}
