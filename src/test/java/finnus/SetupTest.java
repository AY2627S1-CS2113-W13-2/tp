package finnus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import finnus.money.Money;
import finnus.transaction.TransactionList;

/** Checks first-run opening balance entry and confirmation without creating transactions. */
class SetupTest {
    @Test
    public void run_confirmedAmount_setsOpeningBalanceWithoutTransaction() {
        TransactionList transactions = new TransactionList();

        boolean completed = new Setup().run(new ScriptedUi("1250.50", "yes"), transactions);

        assertTrue(completed);
        assertEquals(Money.of("1250.50"), transactions.getOpeningBalance());
        assertEquals(0, transactions.size());
    }

    @Test
    public void run_unconfirmedAmountContinuesUntilAnotherAmountIsConfirmed() {
        TransactionList transactions = new TransactionList();

        boolean completed = new Setup().run(new ScriptedUi("1250", "no", "125.00", "yes"), transactions);

        assertTrue(completed);
        assertEquals(Money.of("125.00"), transactions.getOpeningBalance());
        assertEquals(0, transactions.size());
    }

    @Test
    public void run_unrecognizedConfirmation_keepsAskingUntilConfirmed() {
        TransactionList transactions = new TransactionList();

        boolean completed = new Setup().run(new ScriptedUi("1250", "perhaps", "yes"), transactions);

        assertTrue(completed);
        assertEquals(Money.of("1250"), transactions.getOpeningBalance());
        assertEquals(0, transactions.size());
    }

    @Test
    public void run_inputEndsBeforeConfirmation_doesNotComplete() {
        TransactionList transactions = new TransactionList();

        boolean completed = new Setup().run(new ScriptedUi("1250"), transactions);

        assertFalse(completed);
        assertEquals(Money.ZERO, transactions.getOpeningBalance());
        assertEquals(0, transactions.size());
    }

    @Test
    public void run_rejectedAmountThenInputEnds_leavesOpeningBalanceUnchanged() {
        TransactionList transactions = new TransactionList();

        boolean completed = new Setup().run(new ScriptedUi("1250", "no"), transactions);

        assertFalse(completed);
        assertEquals(Money.ZERO, transactions.getOpeningBalance());
        assertEquals(0, transactions.size());
    }

    @Test
    public void run_invalidAmountPromptsAgain() {
        TransactionList transactions = new TransactionList();

        boolean completed = new Setup().run(new ScriptedUi("-1", "abc", "20", "yes"), transactions);

        assertTrue(completed);
        assertEquals(Money.of("20"), transactions.getOpeningBalance());
        assertEquals(0, transactions.size());
    }
}
