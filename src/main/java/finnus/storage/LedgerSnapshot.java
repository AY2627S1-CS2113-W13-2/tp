package finnus.storage;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import finnus.currency.CurrencyRates;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;

/**
 * An immutable value copy of all financial state, used for saving and change detection.
 * Decimal scales are retained so persistence never silently rounds a value.
 */
public record LedgerSnapshot(BigDecimal openingBalance, List<StoredTransaction> transactions,
                             Map<String, BigDecimal> rates) {
    /**
     * Copies mutable collections and rejects values that cannot represent a valid ledger.
     */
    public LedgerSnapshot {
        Objects.requireNonNull(openingBalance);
        if (openingBalance.signum() < 0) {
            throw new IllegalArgumentException("Opening balance must not be negative");
        }
        transactions = List.copyOf(transactions);
        rates = Map.copyOf(rates);
        rates.forEach(LedgerSnapshot::validateRate);
    }

    /**
     * A transaction value independent of the mutable ledger and console formatting.
     */
    public record StoredTransaction(TransactionType type, BigDecimal amount, String description) {
        /**
         * Validates external values before they reach assertion-based domain constructors.
         */
        public StoredTransaction {
            Objects.requireNonNull(type, "Transaction type must not be null");
            Objects.requireNonNull(amount, "Transaction amount must not be null");
            Objects.requireNonNull(description, "Description must not be null");
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("Transaction amount must be positive");
            }
        }
    }

    /**
     * Returns an empty ledger with no exchange rates and a zero opening balance.
     */
    public static LedgerSnapshot empty() {
        return new LedgerSnapshot(BigDecimal.ZERO, List.of(), Map.of());
    }

    /**
     * Captures values rather than live views so later commands cannot alter this snapshot.
     */
    public static LedgerSnapshot capture(TransactionList ledger) {
        List<StoredTransaction> records = ledger.getAll().stream()
                .map(transaction -> new StoredTransaction(transaction.getType(), transaction.getAmount(),
                        transaction.getDescription()))
                .toList();
        return new LedgerSnapshot(ledger.getOpeningBalance(), records, ledger.getRates().getAll());
    }

    /**
     * Builds a fresh mutable ledger in the original transaction order.
     */
    public TransactionList toTransactionList() {
        TransactionList ledger = new TransactionList();
        ledger.setOpeningBalance(openingBalance);
        for (StoredTransaction record : transactions) {
            ledger.add(new Transaction(record.type(), record.amount(), record.description()));
        }
        rates.forEach(ledger.getRates()::setRate);
        return ledger;
    }

    /**
     * Checks the domain rules for a stored rate without depending on console argument limits.
     */
    static void validateRate(String code, BigDecimal rate) {
        try {
            Currency.getInstance(code);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid currency code");
        }
        if (CurrencyRates.BASE_CURRENCY.equals(code) || rate == null || rate.signum() <= 0) {
            throw new IllegalArgumentException("Rate must be positive and use a foreign currency");
        }
    }
}
