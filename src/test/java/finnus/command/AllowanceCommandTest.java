//@@author chancheese
package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import finnus.allowance.Allowance;
import finnus.currency.ForeignAmount;
import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

class AllowanceCommandTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

    private final TransactionList transactions = new TransactionList();
    private final RecordingUi ui = new RecordingUi();

    @Test
    public void show_noAllowance_explainsHowToSetOne() throws FinNusException {
        AllowanceCommand.show(TODAY).execute(transactions, ui);

        assertEquals("No allowance set yet. Set one with, e.g.: allowance 5 a/500", ui.output());
    }

    @Test
    public void set_withAmount_savesSettingAndShowsSummary() throws FinNusException {
        AllowanceCommand.set(5, new BigDecimal("500"), null, TODAY).execute(transactions, ui);

        assertEquals(new Allowance(5, new BigDecimal("500"), null, LocalDate.of(2026, 10, 5)),
                transactions.getAllowance().orElseThrow());
        assertEquals(String.join("\n",
                "Allowance set.",
                "Allowance: $500.00 on day 5 (added automatically)",
                "This cycle: 5 Oct 2026 - 4 Nov 2026",
                "Next allowance in 7 days (5 Nov 2026).",
                "Received this cycle: $0.00 in 0 deposit(s).",
                "Spent this cycle: $0.00 in 0 withdrawal(s).",
                "From 5 Nov 2026, FinNUS adds it as a deposit for you, so you do not need to record it."),
                ui.output());
        assertEquals(0, transactions.size(), "Setting an allowance must not add a transaction");
    }

    @Test
    public void set_dayOnly_onlyTracksCycle() throws FinNusException {
        AllowanceCommand.set(31, null, null, TODAY).execute(transactions, ui);

        assertTrue(ui.output().contains("Allowance day: 31 or the last day of shorter months"));
        assertFalse(ui.output().contains("adds it as a deposit"));
    }

    @Test
    public void set_foreignCurrencyWithoutRate_warnsToSetRate() throws FinNusException {
        AllowanceCommand.set(5, new BigDecimal("60000"), "JPY", TODAY).execute(transactions, ui);

        assertTrue(ui.output().contains("Allowance: JPY 60000 on day 5 (added automatically)"));
        assertTrue(ui.output().contains("No exchange rate is set for JPY yet. "
                + "Set one before then with: rate JPY RATE"));
    }

    @Test
    public void show_transactionsInAndOutOfCycle_countsOnlyThisCycleInSgd() throws FinNusException {
        transactions.setAllowance(new Allowance(5, null, null, LocalDate.of(2026, 10, 5)));
        // In the cycle (5 Oct to 4 Nov), including its first day.
        transactions.add(dated(TransactionType.DEPOSIT, "500", LocalDate.of(2026, 10, 5)));
        transactions.add(dated(TransactionType.WITHDRAWAL, "4.50", LocalDate.of(2026, 10, 20)));
        ForeignAmount yen = new ForeignAmount("JPY", new BigDecimal("1100"), new BigDecimal("110"));
        transactions.add(new Transaction(TransactionType.WITHDRAWAL, yen, "Ramen", "Food", TODAY));
        // Before the cycle, and saved before dates existed.
        transactions.add(dated(TransactionType.WITHDRAWAL, "99", LocalDate.of(2026, 10, 4)));
        transactions.add(new Transaction(TransactionType.WITHDRAWAL, Money.of("7"), "Old", "Others"));

        AllowanceCommand.show(TODAY).execute(transactions, ui);

        assertTrue(ui.output().contains("Received this cycle: $500.00 in 1 deposit(s)."));
        assertTrue(ui.output().contains("Spent this cycle: $14.50 in 2 withdrawal(s)."));
        assertTrue(ui.output().contains("1 older transaction(s) have no date, so they are not counted."));
    }

    @Test
    public void set_sameDayWhileAllowanceWaitsForRate_keepsItDue() throws FinNusException {
        // The 5 Oct allowance is still waiting for a JPY rate.
        transactions.setAllowance(new Allowance(5, new BigDecimal("60000"), "JPY", LocalDate.of(2026, 9, 5)));

        AllowanceCommand.set(5, new BigDecimal("60000"), "JPY", TODAY).execute(transactions, ui);

        assertEquals(LocalDate.of(2026, 9, 5), transactions.getAllowance().orElseThrow().lastCredited());
        assertTrue(ui.output().contains("Your allowance for 5 Oct 2026 is still waiting to be added."));
    }

    @Test
    public void set_dayOnlyAllowanceGetsAmount_doesNotAddPastMonths() throws FinNusException {
        // A day-only allowance never moves its last credited date, so it must not count as waiting.
        transactions.setAllowance(new Allowance(5, null, null, LocalDate.of(2026, 7, 5)));

        AllowanceCommand.set(5, new BigDecimal("500"), null, TODAY).execute(transactions, ui);

        assertEquals(LocalDate.of(2026, 10, 5), transactions.getAllowance().orElseThrow().lastCredited());
    }

    @Test
    public void set_differentDay_startsFromNextDate() throws FinNusException {
        transactions.setAllowance(new Allowance(5, new BigDecimal("500"), null, LocalDate.of(2026, 9, 5)));

        AllowanceCommand.set(20, new BigDecimal("500"), null, TODAY).execute(transactions, ui);

        assertEquals(LocalDate.of(2026, 11, 20), transactions.getAllowance().orElseThrow().nextDueDate());
    }

    @Test
    public void set_foreignAmountOutOfRangeAtCurrentRate_isRejected() {
        transactions.getRates().setRate("JPY", new BigDecimal("1"));

        assertThrows(FinNusException.class, () -> AllowanceCommand.set(5, new BigDecimal("1000001"), "JPY", TODAY)
                .execute(transactions, ui));
        assertThrows(FinNusException.class, () -> AllowanceCommand.set(5, new BigDecimal("0.001"), "JPY", TODAY)
                .execute(transactions, ui));
        assertTrue(transactions.getAllowance().isEmpty(), "A rejected allowance must not be saved");
    }

    @Test
    public void stop_existingAllowance_removesSettingButKeepsDeposits() throws FinNusException {
        transactions.setAllowance(new Allowance(5, new BigDecimal("500"), null, LocalDate.of(2026, 10, 5)));
        transactions.add(dated(TransactionType.DEPOSIT, "500", LocalDate.of(2026, 10, 5)));

        AllowanceCommand.stop(TODAY).execute(transactions, ui);

        assertTrue(transactions.getAllowance().isEmpty());
        assertEquals(1, transactions.size());
        assertEquals("Allowance stopped. Deposits already added are kept.", ui.output());
    }

    @Test
    public void stop_noAllowance_saysSo() throws FinNusException {
        AllowanceCommand.stop(TODAY).execute(transactions, ui);

        assertEquals("No allowance is set.", ui.output());
    }

    @Test
    public void countdown_oneDayLeft_usesSingular() {
        Allowance allowance = new Allowance(30, null, null, LocalDate.of(2026, 9, 30));

        assertEquals("Next allowance in 1 day (30 Oct 2026).", AllowanceCommand.countdown(allowance, TODAY));
    }

    private static Transaction dated(TransactionType type, String amount, LocalDate date) {
        String category = type == TransactionType.WITHDRAWAL ? "Others" : null;
        return new Transaction(type, Money.of(amount), "Test", category, date);
    }

    /** Keeps every line shown, so tests can check the exact output. */
    static class RecordingUi extends Ui {
        private final List<String> lines = new ArrayList<>();

        @Override
        public void showMessage(String... messageLines) {
            lines.addAll(List.of(messageLines));
        }

        @Override
        public void showMessage(List<String> messageLines) {
            lines.addAll(messageLines);
        }

        String output() {
            return String.join("\n", lines);
        }
    }
}
//@@author
