package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.ScriptedUi;
import finnus.currency.ForeignAmount;
import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.parser.Parser;
import finnus.storage.LedgerSnapshot;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;

//@@author naieel04
/** Checks atomic replacement, retained fields, ledger balances, and user-facing edit results. */
class EditCommandTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void execute_validIndex_changesOnlySelectedEntry(int index) throws FinNusException {
        TransactionList ledger = createLedger();
        List<Transaction> before = ledger.getAll();
        execute("edit " + index + " d/Corrected", ledger);
        assertEquals(before.size(), ledger.size());
        for (int position = 0; position < ledger.size(); position++) {
            Transaction original = before.get(position);
            Transaction updated = ledger.get(position);
            if (position == ledger.size() - index) {
                assertEquals("Corrected", updated.getDescription());
                assertEquals(original.getAmount(), updated.getAmount());
                assertEquals(original.getType(), updated.getType());
                assertEquals(original.getCategory(), updated.getCategory());
                assertEquals(original.getDate(), updated.getDate());
            } else {
                assertSame(original, updated);
            }
        }
        assertEquals(Money.of("50"), ledger.getOpeningBalance());
    }

    @Test
    void execute_depositAmount_recalculatesBalanceAndShowsBeforeAfter() throws FinNusException {
        TransactionList ledger = createLedger();
        Transaction original = ledger.get(0);
        ScriptedUi ui = execute("edit 3 a/120", ledger);
        assertEquals(Money.of("163"), ledger.getBalance());
        assertTrue(ui.output().contains("Transaction updated:"));
        assertTrue(ui.output().contains("  Before: " + original));
        assertTrue(ui.output().contains("  After:  " + ledger.get(0)));
    }

    @Test
    void execute_withdrawalAmount_recalculatesBalance() throws FinNusException {
        TransactionList ledger = createLedger();
        execute("edit 1 a/6.50", ledger);
        assertEquals(Money.of("141"), ledger.getBalance());
    }

    @Test
    void execute_emptyDepositDescription_clearsIt() throws FinNusException {
        TransactionList ledger = createLedger();
        execute("edit 3 d/", ledger);
        assertEquals("", ledger.get(0).getDescription());
    }

    @Test
    void execute_category_resolvesCustomCategoryAndCanResetToOthers() throws FinNusException {
        TransactionList ledger = createLedger();
        ledger.getCategories().add("Gym");
        execute("edit 1 t/gYM", ledger);
        assertEquals("Gym", ledger.get(2).getCategory().orElseThrow());
        execute("edit 1 t/Others", ledger);
        assertEquals("Others", ledger.get(2).getCategory().orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"edit 1 d/", "edit 1 a/8 t/Unknown", "edit 3 a/120 t/food",
        "edit 4 a/5", "edit 1 a/8 on/2026-11-04"})
    void execute_invalidEdit_preservesCompleteSnapshot(String input) {
        TransactionList ledger = createLedger();
        LedgerSnapshot before = LedgerSnapshot.capture(ledger);
        assertThrows(FinNusException.class, () -> execute(input, ledger));
        assertEquals(before, LedgerSnapshot.capture(ledger));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    void execute_emptyLedgerOrInvalidIndex_throws(int index) {
        TransactionList ledger = new TransactionList();
        EditFields fields = new EditFields(new BigDecimal("5"), null, null, null, null);
        assertThrows(FinNusException.class, () -> new EditCommand(index, fields).execute(ledger, new ScriptedUi()));
        assertEquals(0, ledger.size());
    }

    @Test
    void execute_noFields_throwsWithoutMutation() {
        TransactionList ledger = createLedger();
        LedgerSnapshot before = LedgerSnapshot.capture(ledger);
        EditCommand command = new EditCommand(1, new EditFields(null, null, null, null, null));
        assertThrows(FinNusException.class, () -> command.execute(ledger, new ScriptedUi()));
        assertEquals(before, LedgerSnapshot.capture(ledger));
    }

    @Test
    void execute_equivalentValues_retainsOriginalAndReportsNoChange() throws FinNusException {
        TransactionList ledger = createLedger();
        Transaction original = ledger.get(2);
        LedgerSnapshot before = LedgerSnapshot.capture(ledger);
        ScriptedUi ui = execute("edit 1 a/4.5 d/Lunch t/OTHERS on/2026-11-03", ledger);
        assertEquals("No changes made.", ui.output());
        assertSame(original, ledger.get(2));
        assertEquals(before, LedgerSnapshot.capture(ledger));
    }

    @Test
    void execute_changedDate_updatesEntryAndReordersChronologically() throws FinNusException {
        TransactionList ledger = createLedger();
        Transaction deposit = ledger.get(0);
        Transaction bus = ledger.get(1);
        execute("edit 1 on/2026-10-25", ledger);
        assertEquals("Lunch", ledger.get(0).getDescription());
        assertEquals(LocalDate.of(2026, 10, 25), ledger.get(0).getDate().orElseThrow());
        assertSame(deposit, ledger.get(1));
        assertSame(bus, ledger.get(2));
        assertEquals(3, ledger.size());
        assertEquals(Money.of("143"), ledger.getBalance());
    }

    @Test
    void execute_legacyEntry_omittedDateRemainsUnknown() throws FinNusException {
        TransactionList ledger = new TransactionList();
        ledger.add(new Transaction(TransactionType.DEPOSIT, Money.of("5"), "Legacy"));
        execute("edit 1 d/Corrected", ledger);
        assertTrue(ledger.get(0).getDate().isEmpty());
    }

    @Test
    void execute_foreignMetadata_preservesSavedConversion() throws FinNusException {
        TransactionList ledger = new TransactionList();
        ForeignAmount foreign = new ForeignAmount("JPY", new BigDecimal("1100"), new BigDecimal("110"));
        ledger.add(new Transaction(TransactionType.DEPOSIT, foreign, "Foreign"));
        execute("edit 1 d/Corrected", ledger);
        assertEquals("Corrected", ledger.get(0).getDescription());
        assertSame(foreign, ledger.get(0).getForeignAmount().orElseThrow());
        assertEquals(Money.of("10"), ledger.get(0).getAmount());
    }

    /** Dates are already chronological so the selection cases do not depend on insertion sorting. */
    private TransactionList createLedger() {
        TransactionList ledger = new TransactionList();
        ledger.setOpeningBalance(Money.of("50"));
        ledger.add(new Transaction(TransactionType.DEPOSIT, Money.of("100"), "Allowance", null,
                TODAY.minusDays(2)));
        ledger.add(new Transaction(TransactionType.WITHDRAWAL, Money.of("2.50"), "Bus", "Others",
                TODAY.minusDays(1)));
        ledger.add(new Transaction(TransactionType.WITHDRAWAL, Money.of("4.50"), "Lunch", "Others", TODAY));
        return ledger;
    }

    private ScriptedUi execute(String input, TransactionList ledger) throws FinNusException {
        ScriptedUi ui = new ScriptedUi();
        Parser.parse(input, TODAY).execute(ledger, ui);
        return ui;
        //@@author
    }
}
