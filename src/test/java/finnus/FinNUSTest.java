package finnus;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import finnus.allowance.Allowance;
import finnus.money.Money;
import finnus.storage.LedgerSnapshot;
import finnus.storage.LoadResult;
import finnus.storage.Storage;
import finnus.currency.ForeignAmount;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;

//@@author naieel04
/**
 * Verifies autosave timing and failure handling independently of filesystem behavior.
 */
class FinNUSTest {
    @Test
    public void run_foreignNoOpAndInvalidRate_doNotWrite() {
        TransactionList ledger = new TransactionList();
        ledger.add(new Transaction(TransactionType.DEPOSIT,
                new ForeignAmount("JPY", new BigDecimal("1100"), new BigDecimal("110")), "Income"));
        RecordingStorage storage = new RecordingStorage();
        storage.loaded = new LoadResult(LedgerSnapshot.capture(ledger), List.of(), true);
        ScriptedUi ui = new ScriptedUi("edit 1 a/1100.00 r/110.000000", "edit 1 r/0", "exit");
        new FinNUS(ui, storage, fixedOn(LocalDate.of(2026, 11, 3))).run();
        assertEquals(0, storage.attempts);
        assertTrue(ui.output().contains("No changes made."));
    }

    @Test
    public void run_editSaveFails_retainsCorrectionAndRetries() {
        TransactionList ledger = new TransactionList();
        ledger.add(new Transaction(TransactionType.DEPOSIT, Money.of("5"), "Income"));
        RecordingStorage storage = new RecordingStorage();
        storage.loaded = new LoadResult(LedgerSnapshot.capture(ledger), List.of(), true);
        storage.failuresRemaining = 1;
        ScriptedUi ui = new ScriptedUi("edit 1 a/10", "balance", "exit");
        new FinNUS(ui, storage, fixedOn(LocalDate.of(2026, 11, 3))).run();
        assertEquals(2, storage.attempts);
        assertEquals(1, storage.saved.size());
        //@@author
        assertEquals(Money.of("10"), storage.saved.getFirst().toTransactionList().getBalance());
        //@@author naieel04
        assertTrue(ui.output().contains("Automatic save failed"));
        assertTrue(ui.output().contains("Your current balance is: $10.00"));
    }

    @Test
    public void run_editWhileHistoryActive_isBlockedUntilStop() {
        RecordingStorage storage = new RecordingStorage();
        ScriptedUi ui = new ScriptedUi("deposit 5", "history", "edit 1 a/10", "stop", "balance", "exit");
        new FinNUS(ui, storage, fixedOn(LocalDate.of(2026, 11, 3))).run();
        //@@author
        assertEquals(1, storage.attempts);
        //@@author naieel04
        assertEquals(Money.of("5"), storage.saved.getLast().toTransactionList().getBalance());
        assertTrue(ui.output().contains("While viewing history"));
    }

    @Test
    public void run_allowanceBecomesDueBetweenListingAndEdit_editsDisplayedEntryBeforeCrediting() {
        TransactionList ledger = new TransactionList();
        ledger.add(new Transaction(TransactionType.DEPOSIT, Money.of("10"), "Original", null,
                LocalDate.of(2026, 10, 10)));
        ledger.setAllowance(new Allowance(5, new BigDecimal("500"), null, LocalDate.of(2026, 10, 5)));
        RecordingStorage storage = new RecordingStorage();
        storage.loaded = new LoadResult(LedgerSnapshot.capture(ledger), List.of(), true);
        AtomicReference<Instant> time = new AtomicReference<>(fixedOn(LocalDate.of(2026, 11, 4)).instant());
        //@@author
        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                //@@author naieel04
                return Clock.fixed(time.get(), zone);
                //@@author
            }

            @Override
            public Instant instant() {
                //@@author naieel04
                return time.get();
            }
        };
        ScriptedUi ui = new ScriptedUi("recent", "edit 1 a/20", "edit 1 d/Corrected allowance", "exit") {
            //@@author
            @Override
            public String readCommand() {
                //@@author naieel04
                String input = super.readCommand();
                if ("edit 1 a/20".equals(input)) {
                    time.set(fixedOn(LocalDate.of(2026, 11, 5)).instant());
                }
                return input;
                //@@author
            }
        };
        new FinNUS(ui, storage, clock).run();
        //@@author naieel04
        LedgerSnapshot last = storage.saved.getLast();
        assertEquals(2, last.transactions().size());
        assertEquals("Original", last.transactions().getFirst().description());
        assertEquals(Money.of("20"), last.transactions().getFirst().amount());
        assertEquals("Corrected allowance", last.transactions().getLast().description());
        assertEquals(Money.of("500"), last.transactions().getLast().amount());
        assertEquals(LocalDate.of(2026, 11, 5), last.allowance().lastCredited());
        assertEquals(new BigDecimal("500"), last.allowance().amount());
        assertEquals(5, last.allowance().day());
    }

    @Test
    public void run_editChangedValues_savesReplacementWithoutAddingEntry() {
        RecordingStorage storage = new RecordingStorage();
        ScriptedUi ui = new ScriptedUi("deposit 50 d/Income", "edit 1 a/60", "exit");
        new FinNUS(ui, storage, fixedOn(LocalDate.of(2026, 11, 3))).run();
        //@@author
        assertEquals(2, storage.saved.size());
        //@@author naieel04
        LedgerSnapshot last = storage.saved.getLast();
        assertEquals(1, last.transactions().size());
        assertEquals(Money.of("60"), last.toTransactionList().getBalance());
        assertEquals("Income", last.transactions().getFirst().description());
    }

    @Test
    public void run_invalidAndUnchangedEdits_doNotSaveAgain() {
        RecordingStorage storage = new RecordingStorage();
        ScriptedUi ui = new ScriptedUi("deposit 50 d/Income", "edit 1 a/50.00", "edit 2 a/60",
                "edit 1 a/60 t/food", "edit 1 on/2026-11-04", "exit");
        new FinNUS(ui, storage, fixedOn(LocalDate.of(2026, 11, 3))).run();
        //@@author
        assertEquals(1, storage.attempts);
        //@@author naieel04
        assertEquals(Money.of("50"), storage.saved.getLast().toTransactionList().getBalance());
        assertTrue(ui.output().contains("No changes made."));
    }

    @Test
    public void run_mutatingCommands_savesEachChangedSnapshot() {
        RecordingStorage storage = new RecordingStorage();
        //@@author
        ScriptedUi ui = new ScriptedUi("deposit 50", "withdraw 5 d/Lunch",
                //@@author naieel04
                "rate JPY 110", "delete 1", "exit");
        new FinNUS(ui, storage).run();
        //@@author
        assertEquals(4, storage.saved.size());
        //@@author naieel04
        LedgerSnapshot last = storage.saved.getLast();
        //@@author
        assertEquals(Money.of("50"), last.toTransactionList().getBalance());
        //@@author naieel04
        assertEquals(new BigDecimal("110"), last.rates().get("JPY"));
        assertEquals(1, last.transactions().size());
    }

    @Test
    public void run_readOnlyAndInvalidCommands_doesNotSave() {
        RecordingStorage storage = new RecordingStorage();
        ScriptedUi ui = new ScriptedUi("help", "recent", "balance", "rate", "unknown", "withdraw 0", "exit");
        new FinNUS(ui, storage).run();
        assertEquals(0, storage.attempts);
        //@@author
    }

    //@@author nkyjStar
    @Test
    public void run_historyMode_blocksNormalCommandsUntilStop() {
        RecordingStorage storage = new RecordingStorage();
        ScriptedUi ui = new ScriptedUi("deposit 1 d/First", "history n/1", "deposit 2 d/Blocked",
                "exit", "next", "stop", "deposit 3 d/Allowed", "exit");

        new FinNUS(ui, storage).run();

        assertTrue(ui.output().contains("While viewing history, use next, prev, p/PAGE, n/NUMBER, or stop."));
        assertTrue(ui.output().contains("Stopped viewing transaction history."));
        assertEquals(2, storage.saved.size());
        assertEquals(2, storage.saved.getLast().transactions().size());
        assertEquals(Money.of("4"), storage.saved.getLast().toTransactionList().getBalance());
    }

    @Test
    public void run_invalidInitialHistoryPage_doesNotEnterHistoryMode() {
        RecordingStorage storage = new RecordingStorage();
        ScriptedUi ui = new ScriptedUi("history p/2", "deposit 2", "exit");

        new FinNUS(ui, storage).run();

        assertTrue(ui.output().contains("History page is out of range."));
        assertEquals(1, storage.saved.size());
    }

    @Test
    public void run_allowanceDueWhenOpeningHistory_numbersBothPagesConsistently() {
        RecordingStorage storage = new RecordingStorage();
        Allowance allowance = new Allowance(5, new BigDecimal("500"), null, LocalDate.of(2026, 9, 5));
        storage.loaded = new LoadResult(new LedgerSnapshot(Money.ZERO, List.of(), Map.of(), List.of(), allowance),
                List.of(), true);
        AtomicReference<Instant> currentTime = new AtomicReference<>(fixedOn(LocalDate.of(2026, 10, 4)).instant());
        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return Clock.fixed(currentTime.get(), zone);
            }

            @Override
            public Instant instant() {
                return currentTime.get();
            }
        };
        ScriptedUi ui = new ScriptedUi("deposit 10 d/First", "history n/1", "next", "stop", "exit") {
            @Override
            public String readCommand() {
                String command = super.readCommand();
                if ("history n/1".equals(command)) {
                    currentTime.set(fixedOn(LocalDate.of(2026, 10, 5)).instant());
                }
                return command;
            }
        };

        new FinNUS(ui, storage, clock).run();

        assertTrue(ui.output().contains("Transaction history: 1-1 of 2 (1 per page):"));
        assertTrue(ui.output().contains("1. [5 Oct 2026] [Deposit] +$500.00 (Allowance)"));
        assertTrue(ui.output().contains("Transaction history: 2-2 of 2 (1 per page):"));
        assertTrue(ui.output().contains("2. [4 Oct 2026] [Deposit] +$10.00 (First)"));
        assertEquals(2, storage.saved.getLast().transactions().size());
    }
    //@@author

    //@@author naieel04
    @Test
    //@@author
    public void run_repeatedRate_doesNotWriteAgain() {
        //@@author naieel04
        RecordingStorage storage = new RecordingStorage();
        //@@author
        new FinNUS(new ScriptedUi("rate JPY 110", "rate JPY 110", "exit"),
                //@@author naieel04
                storage).run();
        //@@author
        assertEquals(1, storage.attempts);
    }
    //@@author

    //@@author naieel04
    @Test
    public void run_failedSave_retainsStateAndRetriesAfterReadOnlyCommand() {
        RecordingStorage storage = new RecordingStorage();
        storage.failuresRemaining = 1;
        ScriptedUi ui = new ScriptedUi("deposit 10", "balance", "exit");
        new FinNUS(ui, storage).run();
        assertEquals(2, storage.attempts);
        //@@author
        assertEquals(Money.of("10"), storage.saved.getFirst().toTransactionList().getBalance());
        //@@author naieel04
        assertTrue(ui.output().contains("Automatic save failed"));
        assertTrue(ui.output().contains("Your current balance is: $10.00"));
    }

    @Test
    public void run_exitAfterFailure_retriesPendingSave() {
        RecordingStorage storage = new RecordingStorage();
        storage.failuresRemaining = 1;
        new FinNUS(new ScriptedUi("deposit 10", "exit"), storage).run();
        assertEquals(2, storage.attempts);
        assertEquals(1, storage.saved.size());
    }

    @Test
    public void run_endOfInputAfterFailure_retriesPendingSave() {
        RecordingStorage storage = new RecordingStorage();
        storage.failuresRemaining = 1;
        new FinNUS(new ScriptedUi("deposit 10"), storage).run();
        assertEquals(2, storage.attempts);
        assertEquals(1, storage.saved.size());
    }

    @Test
    public void run_finalSaveFailure_warnsOfUnsavedChanges() {
        RecordingStorage storage = new RecordingStorage();
        storage.failuresRemaining = 10;
        ScriptedUi ui = new ScriptedUi("deposit 10", "exit");
        new FinNUS(ui, storage).run();
        assertEquals(2, storage.attempts);
        assertTrue(ui.output().contains("will be lost after exit"));
        assertTrue(storage.saved.isEmpty());
    }

    @Test
    public void run_savingDisabled_neverOverwritesStorage() {
        RecordingStorage storage = new RecordingStorage();
        storage.loaded = new LoadResult(LedgerSnapshot.empty(), List.of("Read failed"), false);
        ScriptedUi ui = new ScriptedUi("deposit 10", "exit");
        new FinNUS(ui, storage).run();
        assertEquals(0, storage.attempts);
        assertTrue(ui.output().contains("Read failed"));
        assertTrue(ui.output().contains("Saving is disabled"));
        assertTrue(ui.output().contains("will be lost after exit"));
    }

    @Test
    public void run_loadedState_isAvailableBeforeCommands() {
        RecordingStorage storage = new RecordingStorage();
        //@@author
        storage.loaded = new LoadResult(new LedgerSnapshot(Money.of("12.30"), List.of(),
                //@@author naieel04
                Map.of("JPY", new BigDecimal("110"))), List.of(), true);
        ScriptedUi ui = new ScriptedUi("balance", "rate", "exit");
        new FinNUS(ui, storage).run();
        assertTrue(ui.output().contains("$12.30"));
        assertTrue(ui.output().contains("1 SGD = 110 JPY"));
        assertEquals(0, storage.attempts);
        //@@author
    }
    //@@author chancheese
    /** Returns a clock that is always at the start of the given day. */
    private static Clock fixedOn(LocalDate date) {
        return Clock.fixed(date.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    @Test
    public void run_allowanceArrivedWhileClosed_addsAndSavesItAtStartup() {
        RecordingStorage storage = new RecordingStorage();
        Allowance allowance = new Allowance(5, new BigDecimal("500"), null, LocalDate.of(2026, 9, 5));
        storage.loaded = new LoadResult(new LedgerSnapshot(Money.ZERO, List.of(), Map.of(), List.of(), allowance),
                List.of(), true);
        ScriptedUi ui = new ScriptedUi("exit");

        new FinNUS(ui, storage, fixedOn(LocalDate.of(2026, 11, 5))).run();

        assertTrue(ui.output().contains("Allowance added: [5 Oct 2026] [Deposit] +$500.00 (Allowance)"));
        assertTrue(ui.output().contains("Allowance added: [5 Nov 2026] [Deposit] +$500.00 (Allowance)"));
        assertEquals(1, storage.attempts);
        LedgerSnapshot saved = storage.saved.getLast();
        assertEquals(2, saved.transactions().size());
        assertEquals(LocalDate.of(2026, 11, 5), saved.allowance().lastCredited());
    }

    @Test
    public void run_rateSetForWaitingAllowance_addsItAfterTheCommand() {
        RecordingStorage storage = new RecordingStorage();
        Allowance allowance = new Allowance(5, new BigDecimal("60000"), "JPY", LocalDate.of(2026, 10, 5));
        storage.loaded = new LoadResult(new LedgerSnapshot(Money.ZERO, List.of(), Map.of(), List.of(), allowance),
                List.of(), true);
        ScriptedUi ui = new ScriptedUi("rate JPY 120", "exit");

        new FinNUS(ui, storage, fixedOn(LocalDate.of(2026, 11, 6))).run();

        assertTrue(ui.output().contains("Your allowance for 5 Nov 2026 was not added yet."));
        assertTrue(ui.output().contains("Allowance added: [5 Nov 2026] [Deposit] +$500.00 (Allowance) [JPY 60000]"));
        assertEquals(1, storage.saved.getLast().transactions().size());
    }

    @Test
    public void run_allowanceCommand_savesSettingAndShowsCountdownInBalance() {
        RecordingStorage storage = new RecordingStorage();
        ScriptedUi ui = new ScriptedUi("allowance 5 a/500", "balance", "allowance off", "exit");

        new FinNUS(ui, storage, fixedOn(LocalDate.of(2026, 10, 29))).run();

        assertEquals(2, storage.saved.size(), "Setting and stopping the allowance are both saved");
        assertEquals(5, storage.saved.getFirst().allowance().day());
        assertEquals(null, storage.saved.getLast().allowance());
        assertTrue(ui.output().contains("Next allowance in 7 days (5 Nov 2026)."));
    }
    //@@author

    //@@author naieel04
    /**
     * Records successful snapshots and fails a configurable number of saves without a mocking library.
     */
    private static class RecordingStorage implements Storage {
        private LoadResult loaded = new LoadResult(LedgerSnapshot.empty(), List.of(), true);
        private final List<LedgerSnapshot> saved = new ArrayList<>();
        private int attempts;
        private int failuresRemaining;

        @Override
        public LoadResult load() {
            return loaded;
        }

        @Override
        public void save(LedgerSnapshot snapshot) throws IOException {
            attempts++;
            if (failuresRemaining > 0) {
                failuresRemaining--;
                throw new IOException("Injected save failure");
            }
            saved.add(snapshot);
            //@@author
        }
    }
}
