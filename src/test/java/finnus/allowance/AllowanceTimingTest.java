package finnus.allowance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.FinNUS;
import finnus.ScriptedUi;
import finnus.money.Money;
import finnus.storage.LedgerSnapshot;
import finnus.storage.LoadResult;
import finnus.storage.Storage;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;

//@@author naieel04
/** Verifies command results and deletion indices when an allowance becomes due during a session. */
class AllowanceTimingTest {
    private static final LocalDate BEFORE_DUE = LocalDate.of(2026, 11, 4);
    private static final LocalDate DUE_DATE = BEFORE_DUE.plusDays(1);
    private static final String ORIGINAL_LINE = "[4 Nov 2026] [Deposit] +$100.00 (Original)";
    private static final String ALLOWANCE_LINE = "[5 Nov 2026] [Deposit] +$500.00 (Allowance)";

    private final MutableClock clock = new MutableClock();
    private final RecordingStorage storage = new RecordingStorage();

    @ParameterizedTest
    @ValueSource(strings = {"recent", "find Original"})
    public void run_allowanceDueBeforeListing_displayedIndexDeletesOriginal(String command) {
        ScriptedUi ui = advancingOn(command, command, "delete 2", "exit");

        new FinNUS(ui, storage, clock).run();

        assertTrue(ui.output().contains("2. " + ORIGINAL_LINE));
        assertTrue(ui.output().indexOf("Allowance added:") < ui.output().indexOf("2. " + ORIGINAL_LINE));
        assertTrue(ui.output().contains("Transaction deleted: " + ORIGINAL_LINE));
        assertOnlyAllowanceRemains();
    }

    @Test
    public void run_allowanceDueBeforeBalance_showsCreditedBalanceImmediately() {
        ScriptedUi ui = advancingOn("balance", "balance", "exit");

        new FinNUS(ui, storage, clock).run();

        assertTrue(ui.output().contains("Your current balance is: $600.00"));
        assertFalse(ui.output().contains("Your current balance is: $100.00"));
        assertEquals(2, storage.saved.getLast().transactions().size());
    }

    @Test
    public void run_allowanceDueBetweenListingAndDelete_consumesDisplayedIndexBeforeCrediting() {
        ScriptedUi ui = advancingOn("delete 1", "recent", "delete 1", "exit");

        new FinNUS(ui, storage, clock).run();

        assertTrue(ui.output().contains("1. " + ORIGINAL_LINE));
        assertTrue(ui.output().contains("Transaction deleted: " + ORIGINAL_LINE));
        assertOnlyAllowanceRemains();
    }

    @Test
    public void run_dateChangesWhileViewingHistory_keepsNavigationAndDeleteIndicesStable() {
        ScriptedUi ui = advancingOn("n/1", "history n/1", "n/1", "stop", "delete 1", "exit");

        new FinNUS(ui, storage, clock).run();

        assertTrue(ui.output().contains("Transaction history: 1-1 of 1 (1 per page):"));
        assertFalse(ui.output().contains("1. " + ALLOWANCE_LINE));
        assertTrue(ui.output().contains("Transaction deleted: " + ORIGINAL_LINE));
        assertOnlyAllowanceRemains();
    }

    @Test
    public void run_stoppingAllowanceOnDueDay_keepsDepositAlreadyDue() {
        ScriptedUi ui = advancingOn("allowance off", "allowance off", "exit");

        new FinNUS(ui, storage, clock).run();

        LedgerSnapshot saved = storage.saved.getLast();
        assertEquals(null, saved.allowance());
        assertEquals(2, saved.transactions().size());
        assertEquals(Money.of(600), saved.toTransactionList().getBalance());
    }

    @Test
    public void run_rateReleasesWaitingAllowance_creditsExactlyOnce() {
        storage.ledger.setAllowance(new Allowance(5, new BigDecimal("60000"), "JPY",
                LocalDate.of(2026, 10, 5)));
        ScriptedUi ui = advancingOn("rate JPY 120", "rate JPY 120", "recent", "rate JPY 120", "exit");

        new FinNUS(ui, storage, clock).run();

        assertTrue(ui.output().contains("1. " + ALLOWANCE_LINE + " [JPY 60000]"));
        assertEquals(2, storage.saved.getLast().transactions().size());
        assertEquals(1, storage.saved.size());
    }

    @Test
    public void run_commandFailsAfterAllowanceCredit_savesCreditBeforeCommand() {
        String command = "withdraw 1 d/Invalid t/unknown";
        ScriptedUi ui = advancingOn(command, command, "exit");

        new FinNUS(ui, storage, clock).run();

        assertTrue(ui.output().contains("Unknown category"));
        assertEquals(1, storage.attempts);
        assertEquals(2, storage.saved.getLast().transactions().size());
    }

    @Test
    public void run_saveAndCommandFail_retriesAllowanceAtShutdown() {
        storage.failuresRemaining = 1;
        ScriptedUi ui = advancingOn("history p/999", "history p/999", "exit");

        new FinNUS(ui, storage, clock).run();

        assertTrue(ui.output().contains("Automatic save failed"));
        assertTrue(ui.output().contains("History page is out of range"));
        assertEquals(2, storage.attempts);
        assertEquals(1, storage.saved.size());
        assertEquals(DUE_DATE, storage.saved.getLast().allowance().lastCredited());
    }

    @Test
    public void run_dateChangesDuringCommand_usesOneDateUntilNextCommand() {
        ScriptedUi ui = new ScriptedUi("deposit 10 d/Extra", "recent", "exit") {
            @Override
            public void showMessage(String... lines) {
                super.showMessage(lines);
                if (List.of(lines).contains("Deposit added:")) {
                    clock.date = DUE_DATE;
                }
            }
        };

        new FinNUS(ui, storage, clock).run();

        assertEquals(2, storage.saved.getFirst().transactions().size(),
                "The first command must not credit tomorrow's allowance midway through execution");
        assertTrue(ui.output().contains("1. " + ALLOWANCE_LINE));
        assertEquals(3, storage.saved.getLast().transactions().size());
    }

    /** Advances the clock when input is read, leaving startup on the previous day. */
    private ScriptedUi advancingOn(String trigger, String... commands) {
        return new ScriptedUi(commands) {
            @Override
            public String readCommand() {
                String command = super.readCommand();
                if (trigger.equals(command)) {
                    clock.date = DUE_DATE;
                }
                return command;
            }
        };
    }

    /** Checks saved values rather than just messages, so deletion of the wrong record cannot pass. */
    private void assertOnlyAllowanceRemains() {
        LedgerSnapshot saved = storage.saved.getLast();
        assertEquals(1, saved.transactions().size());
        assertEquals(Allowance.DEPOSIT_DESCRIPTION, saved.transactions().getFirst().description());
        assertEquals(Money.of(500), saved.toTransactionList().getBalance());
        assertEquals(DUE_DATE, saved.allowance().lastCredited());
    }

    /** Supplies an existing transaction and allowance, recording saves without touching real files. */
    private static class RecordingStorage implements Storage {
        private final TransactionList ledger = new TransactionList();
        private final List<LedgerSnapshot> saved = new ArrayList<>();
        private int attempts;
        private int failuresRemaining;

        RecordingStorage() {
            ledger.add(new Transaction(TransactionType.DEPOSIT, Money.of(100), "Original", null, BEFORE_DUE));
            ledger.setAllowance(new Allowance(5, new BigDecimal("500"), null, LocalDate.of(2026, 10, 5)));
        }

        @Override
        public LoadResult load() {
            return new LoadResult(LedgerSnapshot.capture(ledger), List.of(), true);
        }

        @Override
        public void save(LedgerSnapshot snapshot) throws IOException {
            attempts++;
            if (failuresRemaining > 0) {
                failuresRemaining--;
                throw new IOException("Injected save failure");
            }
            saved.add(snapshot);
        }
    }

    /** Lets each scenario advance time without changing the system clock or waiting overnight. */
    private static class MutableClock extends Clock {
        private LocalDate date = BEFORE_DUE;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant(), zone);
        }

        @Override
        public Instant instant() {
            return date.atStartOfDay().toInstant(ZoneOffset.UTC);
            //@@author
        }
    }
}
