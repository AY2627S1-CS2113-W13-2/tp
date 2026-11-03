package finnus.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.FinNUS;
import finnus.ScriptedUi;
import finnus.currency.ForeignAmount;
import finnus.money.Money;
import finnus.transaction.TransactionType;

//@@author naieel04
/** Runs edit commands against real temporary storage and checks independent restarts and reports. */
class EditPersistenceTest {
    private static final Clock CLOCK = Clock.fixed(
            LocalDate.of(2026, 11, 3).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    Path directory;

    @Test
    void run_editedEntries_savedBeforeShutdownAndRestoredInReports() {
        Path path = directory.resolve("finnus.txt");
        ScriptedUi first = new ScriptedUi("1000", "yes", "", "", "allowance 5", "rate JPY 110",
                "deposit 2200 c/JPY d/Income on/2026-10-15",
                "withdraw 550 c/JPY d/Lunch t/food on/2026-10-20", "deposit 5 d/Unchanged on/2026-10-10",
                "edit 1 a/1100 r/100", "edit 2 d/Tuition r/100 on/2026-10-25") {
            @Override
            public String readCommand() {
                String input = super.readCommand();
                if (input == null) {
                    throw new IllegalStateException("Interrupted before normal shutdown");
                }
                return input;
            }
        };
        assertThrows(IllegalStateException.class, () -> new FinNUS(first, new FileStorage(path), CLOCK).run());
        LedgerSnapshot saved = new FileStorage(path).load().snapshot();
        assertEquals(3, saved.transactions().size());
        assertEquals(Money.of("1000"), saved.openingBalance());
        assertEquals(Money.of("1016"), saved.toTransactionList().getBalance());
        assertEquals(new BigDecimal("110"), saved.rates().get("JPY"));
        assertEquals("Unchanged", saved.transactions().getFirst().description());
        assertEquals(Money.of("5"), saved.transactions().getFirst().amount());
        LedgerSnapshot.StoredTransaction withdrawal = saved.transactions().get(1);
        assertEquals(TransactionType.WITHDRAWAL, withdrawal.type());
        assertEquals("Food", withdrawal.category());
        assertEquals(Money.of("11"), withdrawal.amount());
        assertEquals(new ForeignAmount("JPY", new BigDecimal("1100"), new BigDecimal("100")),
                withdrawal.foreignAmount());
        LedgerSnapshot.StoredTransaction deposit = saved.transactions().getLast();
        assertEquals("Tuition", deposit.description());
        assertEquals(LocalDate.of(2026, 10, 25), deposit.date());
        assertEquals(Money.of("22"), deposit.amount());
        assertEquals(new ForeignAmount("JPY", new BigDecimal("2200"), new BigDecimal("100")),
                deposit.foreignAmount());

        ScriptedUi second = new ScriptedUi("balance", "recent", "find t/food", "allowance",
                "history", "stop", "exit");
        new FinNUS(second, new FileStorage(path), CLOCK).run();
        assertTrue(second.output().contains("Your current balance is: $1016.00"));
        assertTrue(second.output().contains("1. [25 Oct 2026] [Deposit] +$22.00 (Tuition) [JPY 2200]"));
        assertTrue(second.output().contains("2. [20 Oct 2026] [Withdrawal] -$11.00 (Lunch) [JPY 1100] #Food"));
        assertTrue(second.output().contains("Received this cycle: $27.00 in 2 deposit(s)."));
        assertTrue(second.output().contains("Spent this cycle: $11.00 in 1 withdrawal(s)."));
        assertTrue(second.output().contains("Spent: $11.00 in 1 withdrawal(s)."));
        assertTrue(second.output().contains("Transaction history: 1-3 of 3"));
        assertEquals(saved, new FileStorage(path).load().snapshot());
    }

    @Test
    void run_failedAndUnchangedEdits_preserveSavedFile() throws Exception {
        Path path = directory.resolve("finnus.txt");
        new FinNUS(new ScriptedUi("50", "yes", "", "", "deposit 5 d/Income", "exit"),
                new FileStorage(path), CLOCK).run();
        String before = Files.readString(path);
        ScriptedUi second = new ScriptedUi("edit 1 a/5.00", "edit 1 a/6 t/food", "edit 1 r/100", "exit");
        new FinNUS(second, new FileStorage(path), CLOCK).run();
        assertEquals(before, Files.readString(path));
        assertTrue(second.output().contains("No changes made."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"no", "EOF"})
    void run_cancelledEdit_preservesSavedFileAcrossRestart(String answer) throws Exception {
        Path path = directory.resolve("finnus.txt");
        new FinNUS(new ScriptedUi("100", "yes", "50", "", "deposit 100 d/Funding",
                "withdraw 20 d/Lunch", "exit"), new FileStorage(path), CLOCK).run();
        String before = Files.readString(path);
        LedgerSnapshot original = new FileStorage(path).load().snapshot();
        ScriptedUi edit = answer.equals("EOF") ? new ScriptedUi("edit 1 a/160")
                : new ScriptedUi("edit 1 a/160", answer, "exit");

        new FinNUS(edit, new FileStorage(path), CLOCK).run();

        assertTrue(edit.output().contains("Balance would fall below your alert threshold of $50.00."));
        assertTrue(edit.output().contains("Edit cancelled."));
        assertFalse(edit.output().contains("Transaction updated:"));
        assertEquals(before, Files.readString(path));
        ScriptedUi reopened = new ScriptedUi("balance", "recent", "exit");
        new FinNUS(reopened, new FileStorage(path), CLOCK).run();
        assertTrue(reopened.output().contains("Your current balance is: $180.00"));
        assertTrue(reopened.output().contains("[Withdrawal] -$20.00 (Lunch) #Others"));
        assertEquals(original, new FileStorage(path).load().snapshot());
        assertEquals(before, Files.readString(path));
    }

    @Test
    void run_rateEditWithUnchangedSgd_preservesCorrectedRateAcrossRestart() {
        Path path = directory.resolve("finnus.txt");
        new FinNUS(new ScriptedUi("100", "yes", "", "", "rate JPY 110",
                "deposit 1100 c/JPY d/Income", "exit"), new FileStorage(path), CLOCK).run();
        LedgerSnapshot before = new FileStorage(path).load().snapshot();
        ScriptedUi edit = new ScriptedUi("edit 1 r/110.000001", "exit");

        new FinNUS(edit, new FileStorage(path), CLOCK).run();

        assertTrue(edit.output().contains("Transaction updated:"));
        LedgerSnapshot saved = new FileStorage(path).load().snapshot();
        assertEquals(before.transactions().getFirst().amount(), saved.transactions().getFirst().amount());
        assertEquals(new BigDecimal("110.000001"), saved.transactions().getFirst().foreignAmount().rate());
        assertEquals(before.rates(), saved.rates());
        ScriptedUi reopened = new ScriptedUi("balance", "recent", "exit");
        new FinNUS(reopened, new FileStorage(path), CLOCK).run();
        assertTrue(reopened.output().contains("Your current balance is: $110.00"));
        assertEquals(saved, new FileStorage(path).load().snapshot());
    }
    //@@author
}
