package finnus.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.money.Money;

//@@author naieel04
/** Verifies replacement preserves chronological order and does not remove unrelated entries. */
class TransactionReplacementTest {
    @Test
    void replace_sameDate_preservesPositionAmongEqualDatesAndReturnsOriginal() {
        TransactionList ledger = new TransactionList();
        Transaction first = deposit("First", "2026-10-20");
        Transaction second = deposit("Second", "2026-10-20");
        ledger.add(first);
        ledger.add(second);
        Transaction replacement = deposit("Corrected", "2026-10-20");
        assertSame(first, ledger.replace(0, replacement));
        assertEquals(List.of(replacement, second), ledger.getAll());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-01", "2026-10-30"})
    void replace_changedDate_movesEntryToChronologicalPosition(String newDate) {
        TransactionList ledger = new TransactionList();
        Transaction first = deposit("First", "2026-10-10");
        Transaction middle = deposit("Middle", "2026-10-15");
        Transaction last = deposit("Last", "2026-10-20");
        ledger.add(first);
        ledger.add(middle);
        ledger.add(last);
        Transaction replacement = deposit("Corrected", newDate);
        assertSame(middle, ledger.replace(1, replacement));
        List<Transaction> expected = newDate.equals("2026-10-01")
                ? List.of(replacement, first, last) : List.of(first, last, replacement);
        assertEquals(expected, ledger.getAll());
    }

    @Test
    void replace_legacyEntryWithDate_movesAfterUndatedEntries() {
        TransactionList ledger = new TransactionList();
        Transaction first = new Transaction(TransactionType.DEPOSIT, Money.of("1"), "Legacy first");
        Transaction second = new Transaction(TransactionType.DEPOSIT, Money.of("1"), "Legacy second");
        ledger.add(first);
        ledger.add(second);
        Transaction dated = deposit("Corrected", "2026-10-20");
        ledger.replace(0, dated);
        assertEquals(List.of(second, dated), ledger.getAll());
    }

    @Test
    void replace_invalidArguments_leavesLedgerUnchanged() {
        TransactionList ledger = new TransactionList();
        Transaction original = deposit("Original", "2026-10-20");
        ledger.add(original);
        assertThrows(NullPointerException.class, () -> ledger.replace(0, null));
        assertThrows(IndexOutOfBoundsException.class, () -> ledger.replace(-1, original));
        assertThrows(IndexOutOfBoundsException.class, () -> ledger.replace(1, original));
        assertEquals(List.of(original), ledger.getAll());
    }

    private Transaction deposit(String description, String date) {
        return new Transaction(TransactionType.DEPOSIT, Money.of("1"), description, null, LocalDate.parse(date));
        //@@author
    }
}
