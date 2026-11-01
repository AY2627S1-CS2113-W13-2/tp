//@@author chancheese
package finnus.command;

import finnus.money.Money;

/**
 * Adds up the SGD value and number of transactions, e.g. for {@code Spent: $18.30 in 3 withdrawal(s).}
 * Shared by {@code allowance} and {@code find}, so both show totals in the same way.
 */
class TransactionTotal {
    private Money amount = Money.ZERO;
    private int count;

    /** Counts one more transaction of the given SGD value. */
    void add(Money value) {
        amount = amount.add(value);
        count++;
    }

    /**
     * Describes the total, e.g. {@code $18.30 in 3 withdrawal(s)}.
     *
     * @param noun What is counted, in the singular, e.g. {@code withdrawal}.
     */
    String describe(String noun) {
        return amount.format() + " in " + count + " " + noun + "(s)";
    }
}
