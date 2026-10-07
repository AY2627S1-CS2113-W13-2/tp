package seedu.duke.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TransactionTest {
    @Test
    public void toString_depositWithDescription_formatsCorrectly() {
        Transaction t = new Transaction(TransactionType.DEPOSIT, 500, "Allowance");
        assertEquals("[Deposit] +$500.00 (Allowance)", t.toString());
    }

    @Test
    public void toString_depositWithoutDescription_omitsBrackets() {
        Transaction t = new Transaction(TransactionType.DEPOSIT, 12.3, "");
        assertEquals("[Deposit] +$12.30", t.toString());
    }

    @Test
    public void toString_withdrawal_showsMinusSign() {
        Transaction t = new Transaction(TransactionType.WITHDRAWAL, 4.5, "Lunch");
        assertEquals("[Withdrawal] -$4.50 (Lunch)", t.toString());
    }
}
