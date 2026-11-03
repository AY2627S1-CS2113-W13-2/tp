package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.ScriptedUi;
import finnus.alert.SpendingAlert;
import finnus.allowance.Allowance;
import finnus.currency.ForeignAmount;
import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.parser.Parser;
import finnus.storage.LedgerSnapshot;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;

//@@author naieel04
/** Checks spending alert confirmation against actual before-and-after withdrawal totals. */
class EditSpendingAlertTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    @Test
    void execute_increaseCrossesBalanceThreshold_requiresApprovalBeforeMutation() throws FinNusException {
        TransactionList ledger = ledgerWithWithdrawal("20", TODAY);
        ledger.setSpendingAlert(new SpendingAlert(Money.of(50), null));
        LedgerSnapshot before = LedgerSnapshot.capture(ledger);
        ScriptedUi ui = new ScriptedUi("yes") {
            @Override
            public String readCommand(String prompt) {
                assertEquals("Proceed with this edit? [yes/no]: ", prompt);
                assertEquals(before, LedgerSnapshot.capture(ledger), "Confirmation must precede mutation");
                return super.readCommand(prompt);
            }
        };

        Parser.parse("edit 1 a/60", TODAY).execute(ledger, ui);

        assertTrue(ui.output().contains("Balance would fall below your alert threshold of $50.00."));
        assertTrue(ui.output().contains("Transaction updated:"));
        assertEquals(Money.of(40), ledger.getBalance());
    }

    @ParameterizedTest
    @ValueSource(strings = {"no", "EOF"})
    void execute_declinedOrEndedConfirmation_preservesCompleteLedger(String answer) throws FinNusException {
        TransactionList ledger = ledgerWithWithdrawal("20", TODAY);
        ledger.setSpendingAlert(new SpendingAlert(Money.of(50), null));
        LedgerSnapshot before = LedgerSnapshot.capture(ledger);
        ScriptedUi ui = answer.equals("EOF") ? new ScriptedUi() : new ScriptedUi(answer);

        Parser.parse("edit 1 a/60", TODAY).execute(ledger, ui);

        assertEquals(before, LedgerSnapshot.capture(ledger));
        assertTrue(ui.output().contains("Edit cancelled."));
        assertFalse(ui.output().contains("Transaction updated:"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a/40", "a/50", "a/10", "d/Corrected", "a/20"})
    void execute_noNewThresholdCrossing_doesNotAskForConfirmation(String fields) throws FinNusException {
        TransactionList ledger = ledgerWithWithdrawal("20", TODAY);
        ledger.setSpendingAlert(new SpendingAlert(Money.of(50), null));
        ScriptedUi ui = new ScriptedUi();

        Parser.parse("edit 1 " + fields, TODAY).execute(ledger, ui);

        assertFalse(ui.output().contains("Spending alert:"));
        assertFalse(ui.output().contains("Edit cancelled."));
        assertTrue(ui.output().contains("Transaction updated:") || ui.output().contains("No changes made."));
    }

    @Test
    void execute_thresholdAlreadyExceeded_doesNotRepeatAlert() throws FinNusException {
        TransactionList ledger = ledgerWithWithdrawal("60", TODAY);
        ledger.setSpendingAlert(new SpendingAlert(Money.of(50), null));
        ScriptedUi ui = new ScriptedUi();

        Parser.parse("edit 1 a/70", TODAY).execute(ledger, ui);

        assertEquals(Money.of(30), ledger.getBalance());
        assertFalse(ui.output().contains("Spending alert:"));
    }

    @Test
    void execute_allowancePercentageCrossed_countsOnlyReplacementDifference() throws FinNusException {
        TransactionList ledger = allowanceLedger();
        ledger.add(withdrawal("20", TODAY));
        ScriptedUi ui = new ScriptedUi("yes");

        Parser.parse("edit 1 a/35", TODAY).execute(ledger, ui);

        assertTrue(ui.output().contains("Withdrawals would exceed 50% of your allowance ($50.00)."));
        assertFalse(ui.output().contains("Withdrawals would exceed 100%"));
        assertEquals(Money.of(45), ledger.getBalance());
    }

    @Test
    void execute_allowanceBelowLimit_doesNotDoubleCountOriginal() throws FinNusException {
        TransactionList ledger = allowanceLedger();
        ScriptedUi ui = new ScriptedUi();

        Parser.parse("edit 1 a/35", TODAY).execute(ledger, ui);

        assertFalse(ui.output().contains("Spending alert:"));
        assertEquals(Money.of(65), ledger.getBalance());
    }

    @Test
    void execute_fullAllowanceAndZeroCrossed_reportsBoth() throws FinNusException {
        TransactionList ledger = allowanceLedger();
        ScriptedUi ui = new ScriptedUi("yes");

        Parser.parse("edit 1 a/110", TODAY).execute(ledger, ui);

        assertTrue(ui.output().contains("Withdrawals would exceed 100% of your allowance ($100.00)."));
        assertTrue(ui.output().contains("Balance would fall below $0.00."));
        assertEquals(Money.of(-10), ledger.getBalance());
    }

    @Test
    void execute_dateMovesWithdrawalIntoCycle_checksDestinationCycle() throws FinNusException {
        TransactionList ledger = ledgerWithWithdrawal("30", TODAY.minusMonths(1));
        ledger.setAllowance(new Allowance(1, new BigDecimal("100"), null, TODAY.withDayOfMonth(1)));
        ledger.setSpendingAlert(new SpendingAlert(null, new BigDecimal("50")));
        ledger.add(withdrawal("30", TODAY));
        ScriptedUi ui = new ScriptedUi("no");
        LedgerSnapshot before = LedgerSnapshot.capture(ledger);

        Parser.parse("edit 2 on/2026-11-02", TODAY).execute(ledger, ui);

        assertTrue(ui.output().contains("Withdrawals would exceed 50%"));
        assertEquals(before, LedgerSnapshot.capture(ledger));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a/80", "r/0.25"})
    void execute_foreignCorrection_checksResultingSgdAmount(String fields) throws FinNusException {
        TransactionList ledger = ledgerWithWithdrawal("20", TODAY);
        ForeignAmount foreign = new ForeignAmount("USD", new BigDecimal("20"), BigDecimal.ONE);
        ledger.replace(1, new Transaction(TransactionType.WITHDRAWAL, foreign, "Foreign", "Others", TODAY));
        ledger.setSpendingAlert(new SpendingAlert(Money.of(50), null));
        ScriptedUi ui = new ScriptedUi("yes");

        Parser.parse("edit 1 " + fields, TODAY).execute(ledger, ui);

        assertTrue(ui.output().contains("Balance would fall below your alert threshold"));
        assertEquals(Money.of(20), ledger.getBalance());
        assertEquals(2, ledger.size());
    }

    @Test
    void execute_openingBalanceCrossed_usesExistingFixedThreshold() throws FinNusException {
        TransactionList ledger = ledgerWithWithdrawal("20", TODAY);
        ledger.setOpeningBalance(Money.of(50));
        ScriptedUi ui = new ScriptedUi("no");

        Parser.parse("edit 1 a/110", TODAY).execute(ledger, ui);

        assertTrue(ui.output().contains("Balance would fall below your opening balance of $50.00."));
        assertEquals(Money.of(130), ledger.getBalance());
    }

    @ParameterizedTest
    @ValueSource(strings = {"d/Corrected", "a/30"})
    void execute_undatedLegacyEdit_doesNotAddItToAllowanceCycle(String fields) throws FinNusException {
        LocalDate currentDate = LocalDate.now();
        TransactionList ledger = ledgerWithWithdrawal("20", null);
        ledger.setAllowance(new Allowance(1, new BigDecimal("100"), null, currentDate.withDayOfMonth(1)));
        ledger.setSpendingAlert(new SpendingAlert(null, new BigDecimal("50")));
        ledger.add(withdrawal("40", currentDate));
        ScriptedUi ui = new ScriptedUi();

        Parser.parse("edit 3 " + fields, currentDate).execute(ledger, ui);

        assertTrue(ui.output().contains("Transaction updated:"));
        assertFalse(ui.output().contains("Spending alert:"));
        assertTrue(ledger.get(0).getDate().isEmpty());
    }

    @Test
    void execute_undatedLegacyIncrease_stillChecksBalanceThresholds() throws FinNusException {
        TransactionList ledger = ledgerWithWithdrawal("20", null);
        ScriptedUi ui = new ScriptedUi("no");
        LedgerSnapshot before = LedgerSnapshot.capture(ledger);

        Parser.parse("edit 2 a/110", TODAY).execute(ledger, ui);

        assertTrue(ui.output().contains("Balance would fall below $0.00."));
        assertEquals(before, LedgerSnapshot.capture(ledger));
    }

    /** Builds a funded ledger directly so fixtures do not themselves consume alert confirmations. */
    private static TransactionList ledgerWithWithdrawal(String amount, LocalDate date) {
        TransactionList ledger = new TransactionList();
        ledger.add(new Transaction(TransactionType.DEPOSIT, Money.of(100), "Funding", null,
                TODAY.minusMonths(2)));
        ledger.add(withdrawal(amount, date));
        return ledger;
    }

    /** Sets a monthly cycle whose spending threshold can be crossed by either amount or date edits. */
    private static TransactionList allowanceLedger() {
        TransactionList ledger = ledgerWithWithdrawal("20", TODAY);
        ledger.setAllowance(new Allowance(1, new BigDecimal("100"), null, TODAY.withDayOfMonth(1)));
        ledger.setSpendingAlert(new SpendingAlert(null, new BigDecimal("50")));
        return ledger;
    }

    private static Transaction withdrawal(String amount, LocalDate date) {
        return new Transaction(TransactionType.WITHDRAWAL, Money.of(amount), "Spending", "Others", date);
        //@@author
    }
}
