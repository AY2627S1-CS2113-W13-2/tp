package finnus.log;

import java.math.BigDecimal;
import java.util.ArrayList;

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

    /** 
     * Throws an IllegalArgumentException if the value is negative 
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

    /** 
     * Calculates the balance from lists of deposits and withdrawals,
     * then return it
     */
    public BigDecimal getBalance(ArrayList<BigDecimal> deposits, ArrayList<BigDecimal> withdrawals) {
        BigDecimal listBalance = balanceFromLists(deposits, withdrawals);

        return listBalance;
    }

    /** 
     * Returns balance from lists of deposits and withdrawals
     * 
     * @param deposits The list of deposits as BigDecimal values
     * @param withdrawals  The list of withdrawals as BigDecimal values
     */
    private BigDecimal balanceFromLists(ArrayList<BigDecimal> deposits, ArrayList<BigDecimal> withdrawals) {
        BigDecimal depositTotal = deposits.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal withdrawalTotal = withdrawals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return depositTotal.subtract(withdrawalTotal);
    }

    /** Print balance, currently in balance */
    public void printBalance() {
        System.out.println("Your balance is: $" + balance);
    }
}
