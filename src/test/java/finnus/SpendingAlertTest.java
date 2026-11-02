package finnus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import finnus.alert.SpendingAlert;
import finnus.allowance.Allowance;
import finnus.command.DepositCommand;
import finnus.command.ForeignTransactionCommand;
import finnus.command.WithdrawCommand;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;

//@@author lololol8484
/** Checks threshold-crossing rules and confirmation behavior for proposed withdrawals. */
class SpendingAlertTest {
    private static final LocalDate CYCLE_DATE = LocalDate.of(2026, 10, 20);

    @Test
    public void crossedBy_fixedBalanceThreshold_listsEveryBalanceThresholdCrossed() {
        TransactionList ledger = new TransactionList();
        ledger.setOpeningBalance(Money.of("200"));
        SpendingAlert alert = new SpendingAlert(Money.of("100"), null);

        List<String> messages = alert.crossedBy(ledger, withdrawal("110", CYCLE_DATE));

        assertEquals(2, messages.size());
        assertTrue(messages.get(0).contains("alert threshold of $100.00"));
        assertTrue(messages.get(1).contains("opening balance of $200.00"));
    }

    @Test
    public void crossedBy_allowancePercentageCrossed_reportsPercentageButNotFullAllowance() {
        TransactionList ledger = ledgerWithAllowance(new BigDecimal("500"));
        ledger.setOpeningBalance(Money.of("1000"));
        ledger.add(withdrawal("440", LocalDate.of(2026, 10, 10)));
        SpendingAlert alert = new SpendingAlert(null, new BigDecimal("90"));

        List<String> messages = alert.crossedBy(ledger, withdrawal("11", CYCLE_DATE));

        assertEquals(1, messages.size());
        assertTrue(messages.getFirst().contains("exceed 90%"));
    }

    @Test
    public void crossedBy_fullAllowanceCrossed_reportsFixedAllowanceThreshold() {
        TransactionList ledger = ledgerWithAllowance(new BigDecimal("500"));
        ledger.setOpeningBalance(Money.of("1000"));
        ledger.add(withdrawal("490", LocalDate.of(2026, 10, 10)));

        List<String> messages = new SpendingAlert(null, null).crossedBy(ledger, withdrawal("20", CYCLE_DATE));

        assertEquals(1, messages.size());
        assertTrue(messages.getFirst().contains("exceed 100%"));
    }

    @Test
    public void crossedBy_undatedLegacyWithdrawal_isNotCountedTowardAllowance() {
        TransactionList ledger = ledgerWithAllowance(new BigDecimal("500"));
        ledger.setOpeningBalance(Money.of("1000"));
        ledger.add(new Transaction(TransactionType.WITHDRAWAL, Money.of("500"), "Old entry", "Others"));

        List<String> messages = new SpendingAlert(null, new BigDecimal("90"))
                .crossedBy(ledger, withdrawal("10", CYCLE_DATE));

        assertTrue(messages.isEmpty());
    }

    @Test
    public void crossedBy_allowanceWithoutAmount_hasNoAllowanceThresholds() {
        TransactionList ledger = new TransactionList();
        ledger.setOpeningBalance(Money.of("1000"));
        ledger.setAllowance(new Allowance(5, null, null, LocalDate.of(2026, 10, 5)));
        ledger.add(new Transaction(TransactionType.DEPOSIT, Money.of("1000"), "Savings"));

        List<String> messages = new SpendingAlert(null, new BigDecimal("90"))
                .crossedBy(ledger, withdrawal("600", CYCLE_DATE));

        assertTrue(messages.isEmpty());
    }

    @Test
    public void crossedBy_zeroOpeningBalance_reportsOpeningAndNegativeBalanceThresholds() {
        TransactionList ledger = new TransactionList();

        List<String> messages = new SpendingAlert(null, null).crossedBy(ledger, withdrawal("1", CYCLE_DATE));

        assertEquals(2, messages.size());
        assertTrue(messages.get(0).contains("opening balance of $0.00"));
        assertTrue(messages.get(1).contains("fall below $0.00"));
    }

    @Test
    public void execute_declinedOrEndedConfirmation_doesNotRecordWithdrawal() throws Exception {
        for (ScriptedUi ui : List.of(new ScriptedUi("no"), new ScriptedUi())) {
            TransactionList ledger = new TransactionList();
            ledger.setOpeningBalance(Money.of("100"));
            ledger.setSpendingAlert(new SpendingAlert(Money.of("50"), null));

            new WithdrawCommand(Money.of("60"), "Books").execute(ledger, ui);

            assertEquals(0, ledger.size());
            assertTrue(ui.output().contains("Spending alert:"));
            assertTrue(ui.output().contains("Withdrawal cancelled."));
        }
    }

    @Test
    public void execute_invalidConfirmationThenYes_recordsWithdrawalAfterRetry() throws Exception {
        TransactionList ledger = new TransactionList();
        ledger.setOpeningBalance(Money.of("100"));
        ledger.setSpendingAlert(new SpendingAlert(Money.of("50"), null));
        ScriptedUi ui = new ScriptedUi("maybe", "y");

        new WithdrawCommand(Money.of("60"), "Books").execute(ledger, ui);

        assertEquals(1, ledger.size());
        assertTrue(ui.output().contains("Please answer yes or no."));
    }

    @Test
    public void execute_foreignWithdrawalDeclined_doesNotRecordConvertedTransaction() throws Exception {
        TransactionList ledger = new TransactionList();
        ledger.setOpeningBalance(Money.of("100"));
        ledger.setSpendingAlert(new SpendingAlert(Money.of("95"), null));
        ledger.getRates().setRate("JPY", new BigDecimal("100"));
        ScriptedUi ui = new ScriptedUi("n");

        new ForeignTransactionCommand(TransactionType.WITHDRAWAL, new BigDecimal("1000"), "JPY", "Ramen")
                .execute(ledger, ui);

        assertEquals(0, ledger.size());
        assertTrue(ui.output().contains("alert threshold of $95.00"));
        assertTrue(ui.output().contains("Withdrawal cancelled."));
    }

    @Test
    public void execute_crossingThresholdAgainAfterRecovery_alertsAgain() throws Exception {
        TransactionList ledger = new TransactionList();
        ledger.setOpeningBalance(Money.of("100"));
        ledger.setSpendingAlert(new SpendingAlert(Money.of("50"), null));
        new WithdrawCommand(Money.of("60"), "Books").execute(ledger, new ScriptedUi("yes"));
        new DepositCommand(Money.of("20")).execute(ledger, new ScriptedUi());
        ScriptedUi confirmation = new ScriptedUi("yes");

        new WithdrawCommand(Money.of("20"), "Books").execute(ledger, confirmation);

        assertEquals(3, ledger.size());
        assertTrue(confirmation.output().contains("alert threshold of $50.00"));
    }

    @Test
    public void crossedBy_alreadyBelowThreshold_doesNotRepeatAlert() {
        TransactionList ledger = new TransactionList();
        ledger.setOpeningBalance(Money.of("100"));
        ledger.add(withdrawal("60", CYCLE_DATE));

        List<String> messages = new SpendingAlert(Money.of("50"), null)
                .crossedBy(ledger, withdrawal("5", CYCLE_DATE));

        assertFalse(messages.stream().anyMatch(message -> message.contains("alert threshold")));
    }

    private static TransactionList ledgerWithAllowance(BigDecimal amount) {
        TransactionList ledger = new TransactionList();
        ledger.setAllowance(new Allowance(5, amount, null, LocalDate.of(2026, 10, 5)));
        return ledger;
    }

    private static Transaction withdrawal(String amount, LocalDate date) {
        return new Transaction(TransactionType.WITHDRAWAL, Money.of(amount), "Test", "Others", date);
    }
}
//@@author
