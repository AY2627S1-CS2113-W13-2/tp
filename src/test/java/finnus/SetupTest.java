package finnus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import finnus.allowance.Allowance;
import finnus.money.Money;
import finnus.transaction.TransactionList;

//@@author lololol8484
/** Checks first-run opening balance entry and confirmation without creating transactions. */
class SetupTest {
    //@@author chancheese
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);
    //@@author

    //@@author naieel04
    @Test
    public void run_confirmationCallback_runsBeforeOptionalPrompts() {
        //@@author
        TransactionList transactions = new TransactionList();
        //@@author naieel04
        boolean[] wasSaved = {false};
        ScriptedUi ui = new ScriptedUi("67", "yes", "", "5", "500") {
            @Override
            public String readCommand(String prompt) {
                if (prompt.startsWith("Allowance") || prompt.startsWith("Balance alert")) {
                    assertTrue(wasSaved[0], "The balance checkpoint must run before optional prompts");
                }
                return super.readCommand(prompt);
            }
        };

        assertTrue(new Setup().run(ui, transactions, TODAY, () -> {
            assertFalse(wasSaved[0], "Confirmation callback must run only once");
            assertEquals(Money.of(67), transactions.getOpeningBalance());
            //@@author
            assertTrue(transactions.getAllowance().isEmpty());
            //@@author naieel04
            wasSaved[0] = true;
        }));

        assertTrue(wasSaved[0]);
        assertEquals(new BigDecimal("500"), transactions.getAllowance().orElseThrow().amount());
    }

    @Test
    public void run_unconfirmedBalance_doesNotInvokeCallback() {
        //@@author
        TransactionList transactions = new TransactionList();
        //@@author naieel04
        assertFalse(new Setup().run(new ScriptedUi("67", "no"), transactions, TODAY, () -> {
            throw new AssertionError("Unconfirmed balances must not be saved");
        }));
        //@@author
    }

    //@@author lololol8484
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
    //@@author

    //@@author chancheese
    @Test
    public void run_allowanceDayAndAmount_setsAllowance() {
        TransactionList transactions = new TransactionList();
        ScriptedUi ui = new ScriptedUi("100", "yes", "", "5", "500");

        assertTrue(new Setup().run(ui, transactions, TODAY));

        assertEquals(new Allowance(5, new BigDecimal("500"), null, LocalDate.of(2026, 10, 5)),
                transactions.getAllowance().orElseThrow());
        assertTrue(ui.output().contains("Next allowance in 7 days (5 Nov 2026)."));
        assertEquals(0, transactions.size(), "Setup must not add the allowance as a deposit straight away");
    }

    @Test
    public void run_enterAtAllowanceDay_skipsAllowance() {
        TransactionList transactions = new TransactionList();

        assertTrue(new Setup().run(new ScriptedUi("100", "yes", "", ""), transactions, TODAY));

        assertTrue(transactions.getAllowance().isEmpty());
    }

    @Test
    public void run_enterAtAllowanceAmount_onlyTracksDay() {
        TransactionList transactions = new TransactionList();

        assertTrue(new Setup().run(new ScriptedUi("100", "yes", "", "31", "   "), transactions, TODAY));

        assertEquals(31, transactions.getAllowance().orElseThrow().day());
        assertFalse(transactions.getAllowance().orElseThrow().hasAmount());
    }

    @Test
    public void run_invalidAllowanceAnswers_asksAgain() {
        TransactionList transactions = new TransactionList();

        assertTrue(new Setup().run(new ScriptedUi("100", "yes", "", "32", "five", "5", "-1", "0", "250.50"),
                transactions, TODAY));

        assertEquals(new BigDecimal("250.50"), transactions.getAllowance().orElseThrow().amount());
    }

    @Test
    public void run_inputEndsAtAllowance_stillCompletesWithOpeningBalance() {
        TransactionList transactions = new TransactionList();

        assertTrue(new Setup().run(new ScriptedUi("100", "yes"), transactions, TODAY));

        assertEquals(Money.of("100"), transactions.getOpeningBalance());
        assertTrue(transactions.getAllowance().isEmpty());
    }
    //@@author

    //@@author lololol8484
    @Test
    public void run_alertOptionsAndAllowanceAmount_setsBothOptionalThresholds() {
        TransactionList transactions = new TransactionList();
        ScriptedUi ui = new ScriptedUi("100", "yes", "75", "5", "500", "90");

        assertTrue(new Setup().run(ui, transactions, TODAY));

        assertEquals(Money.of("75"), transactions.getSpendingAlert().balanceThreshold().orElseThrow());
        assertEquals(new BigDecimal("90"), transactions.getSpendingAlert().allowancePercentage().orElseThrow());
        assertTrue(transactions.getAllowance().orElseThrow().hasAmount());
    }

    @Test
    public void run_allowanceDayWithoutAmount_doesNotAskForPercentage() {
        TransactionList transactions = new TransactionList();
        ScriptedUi ui = new ScriptedUi("100", "yes", "", "5", "");

        assertTrue(new Setup().run(ui, transactions, TODAY));

        assertFalse(transactions.getAllowance().orElseThrow().hasAmount());
        assertTrue(transactions.getSpendingAlert().allowancePercentage().isEmpty());
        assertFalse(ui.output().contains("percentage of your monthly allowance"));
    }

    @Test
    public void run_invalidAlertOptions_repromptsUntilValuesAreValid() {
        TransactionList transactions = new TransactionList();
        ScriptedUi ui = new ScriptedUi("100", "yes", "0", "50", "5", "500", "abc", "101", "90");

        assertTrue(new Setup().run(ui, transactions, TODAY));

        assertEquals(Money.of("50"), transactions.getSpendingAlert().balanceThreshold().orElseThrow());
        assertEquals(new BigDecimal("90"), transactions.getSpendingAlert().allowancePercentage().orElseThrow());
        assertTrue(ui.output().contains("threshold must be greater than $0.00"));
        assertTrue(ui.output().contains("whole percentage from 1 to 100"));
    }
    //@@author
}
//@@author
