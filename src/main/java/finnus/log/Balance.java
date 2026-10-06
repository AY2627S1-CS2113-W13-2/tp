package finnus.log;

import java.math.BigDecimal;

/** Represents the balance value */
public class Balance {

    private BigDecimal balance;

    /** Creates a new balance */
    public Balance() {
        balance = new BigDecimal(0);
    }

    /** Checks for negative value */
    private boolean isNegative(BigDecimal value) {
        return value.compareTo(new BigDecimal(0)) < 0;
    }

    /** Throws an IllegalArgumentException if the value is negative 
     * 
     *  @param value The BigDecimal value to check
     *  @throws IllegalArgumentException if the value is negative 
     */
    private void throwOnNegativeValue(BigDecimal value) throws IllegalArgumentException {
        if (isNegative(value)) {
            throw new IllegalArgumentException("Negative values are not allowed!");
        }
    }

    /** Adds deposit value to balance
     * 
     * @param deposit The non-negative BigDecimal value to add to balance 
     * @throws IllegalArgumentException if the deposit is negative
     */
    public void addToBalance(BigDecimal deposit) throws IllegalArgumentException {
        throwOnNegativeValue(deposit);

        balance = balance.add(deposit);
    }

    /** Deducts withdrawal value from balance
     * 
     * @param withdrawal The non-negative BigDecimal value to deduct from balance
     * @throws IllegalArgumentException if the withdrawal value is negative
     */
    public void deductFromBalance(BigDecimal withdrawal) throws IllegalArgumentException {
        throwOnNegativeValue(withdrawal);

        balance = balance.subtract(withdrawal);
    }

    /** Returns the balance as a BigDecimal value */
    public BigDecimal getBalance() {
        return balance;
    }
}
