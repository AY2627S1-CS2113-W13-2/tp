package finnus.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import finnus.money.Money;

class TransactionTest {
    @Test
    public void toString_depositWithDescription_formatsCorrectly() {
        Transaction t = new Transaction(TransactionType.DEPOSIT, Money.of("500"), "Allowance");
        assertEquals("[Deposit] +$500.00 (Allowance)", t.toString());
    }

    @Test
    public void toString_depositWithoutDescription_omitsBrackets() {
        Transaction t = new Transaction(TransactionType.DEPOSIT, Money.of("12.3"), "");
        assertEquals("[Deposit] +$12.30", t.toString());
    }

    @Test
    public void toString_withdrawal_showsMinusSign() {
        Transaction t = new Transaction(TransactionType.WITHDRAWAL, Money.of("4.50"), "Lunch");
        assertEquals("[Withdrawal] -$4.50 (Lunch)", t.toString());
    }

    @Test
    public void getSignedAmount_manySmallAmounts_sumIsExact() {
        // With double, adding 0.10 ten times gives 0.9999999999999999 instead of 1.00.
        Money total = Money.ZERO;
        for (int i = 0; i < 10; i++) {
            total = total.add(new Transaction(TransactionType.DEPOSIT, Money.of("0.10"), "").getSignedAmount());
        }
        assertEquals(0, total.compareTo(Money.of(1)));
    }
}
