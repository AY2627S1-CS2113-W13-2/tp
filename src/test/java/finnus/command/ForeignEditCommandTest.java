package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
/** Tests historical conversion corrections independently of global exchange-rate settings. */
class ForeignEditCommandTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    @ParameterizedTest
    @EnumSource(TransactionType.class)
    void execute_amountOnly_retainsSavedRateDespiteChangedGlobalRate(TransactionType type) throws FinNusException {
        TransactionList ledger = createLedger(type);
        ledger.getRates().setRate("JPY", new BigDecimal("220"));
        execute("edit 1 a/2200", ledger);
        assertConversion(ledger, "2200", "110", "20");
        assertEquals(new BigDecimal("220"), ledger.getRates().getRate("JPY"));
        assertEquals(type, ledger.get(0).getType());
        assertEquals(type == TransactionType.DEPOSIT ? Money.of("20") : Money.of("-20"), ledger.getBalance());
    }

    @ParameterizedTest
    @EnumSource(TransactionType.class)
    void execute_rateOnlyWithoutGlobalRate_updatesConversion(TransactionType type) throws FinNusException {
        TransactionList ledger = createLedger(type);
        ScriptedUi ui = execute("edit 1 r/100", ledger);
        assertConversion(ledger, "1100", "100", "11");
        assertTrue(ledger.getRates().getAll().isEmpty());
        assertTrue(ui.output().contains("Converted at 1 SGD = 100 JPY."));
    }

    @ParameterizedTest
    @EnumSource(TransactionType.class)
    void execute_amountAndRate_updatesTogether(TransactionType type) throws FinNusException {
        TransactionList ledger = createLedger(type);
        execute("edit 1 a/6000 r/120", ledger);
        assertConversion(ledger, "6000", "120", "50");
    }

    @Test
    void execute_metadataOnly_preservesForeignObjectAndAllConversionValues() throws FinNusException {
        TransactionList ledger = createLedger(TransactionType.WITHDRAWAL);
        ForeignAmount original = ledger.get(0).getForeignAmount().orElseThrow();
        execute("edit 1 d/Corrected t/food on/2026-10-25", ledger);
        assertSame(original, ledger.get(0).getForeignAmount().orElseThrow());
        assertEquals("Corrected", ledger.get(0).getDescription());
        assertEquals("Food", ledger.get(0).getCategory().orElseThrow());
        assertEquals(LocalDate.of(2026, 10, 25), ledger.get(0).getDate().orElseThrow());
        assertEquals(Money.of("10"), ledger.get(0).getAmount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"edit 1 a/0.01 r/1000", "edit 1 a/1000000 r/0.000001",
        "edit 1 a/2200 t/Unknown", "edit 1 r/100 d/", "edit 1 c/USD"})
    void execute_invalidForeignEdit_preservesWholeSnapshot(String input) {
        TransactionList ledger = createLedger(TransactionType.WITHDRAWAL);
        ledger.getRates().setRate("JPY", new BigDecimal("220"));
        LedgerSnapshot before = LedgerSnapshot.capture(ledger);
        assertThrows(FinNusException.class, () -> execute(input, ledger));
        assertEquals(before, LedgerSnapshot.capture(ledger));
    }

    @Test
    void execute_categoryOnForeignDeposit_failsAtomically() {
        TransactionList ledger = createLedger(TransactionType.DEPOSIT);
        LedgerSnapshot before = LedgerSnapshot.capture(ledger);
        assertThrows(FinNusException.class, () -> execute("edit 1 r/100 t/food", ledger));
        assertEquals(before, LedgerSnapshot.capture(ledger));
    }

    @Test
    void execute_rateOnSgd_failsWithoutChangingAmount() {
        TransactionList ledger = new TransactionList();
        ledger.add(new Transaction(TransactionType.DEPOSIT, Money.of("5"), "SGD"));
        LedgerSnapshot before = LedgerSnapshot.capture(ledger);
        assertThrows(FinNusException.class, () -> execute("edit 1 a/10 r/100", ledger));
        assertEquals(before, LedgerSnapshot.capture(ledger));
    }

    @Test
    void execute_equivalentDecimalValues_retainsOriginalWithoutChange() throws FinNusException {
        TransactionList ledger = createLedger(TransactionType.DEPOSIT);
        Transaction original = ledger.get(0);
        ScriptedUi ui = execute("edit 1 a/1100.00 r/110.000000", ledger);
        assertSame(original, ledger.get(0));
        assertEquals("No changes made.", ui.output());
    }

    @Test
    void execute_rateChangeWithSameRoundedSgd_stillUpdatesHistoricalRate() throws FinNusException {
        TransactionList ledger = createLedger(TransactionType.DEPOSIT);
        execute("edit 1 r/110.000001", ledger);
        assertConversion(ledger, "1100", "110.000001", "10");
    }

    @Test
    void execute_conversionBoundaries_appliesCentRoundingAndSgdLimit() throws FinNusException {
        TransactionList ledger = createLedger(TransactionType.DEPOSIT);
        execute("edit 1 a/1 r/200", ledger);
        assertEquals(Money.of("0.01"), ledger.get(0).getAmount());
        execute("edit 1 a/110000000 r/110", ledger);
        assertEquals(Money.MAX_AMOUNT, ledger.get(0).getAmount());
        LedgerSnapshot before = LedgerSnapshot.capture(ledger);
        assertThrows(FinNusException.class, () -> execute("edit 1 a/110000001.10", ledger));
        assertEquals(before, LedgerSnapshot.capture(ledger));
    }

    @Test
    void execute_sgdAmountAboveLimit_isRejectedAtExecution() {
        TransactionList ledger = new TransactionList();
        ledger.add(new Transaction(TransactionType.DEPOSIT, Money.of("5"), "SGD"));
        assertThrows(FinNusException.class, () -> execute("edit 1 a/1000000.01", ledger));
        assertEquals(Money.of("5"), ledger.get(0).getAmount());
    }

    private TransactionList createLedger(TransactionType type) {
        TransactionList ledger = new TransactionList();
        ForeignAmount foreign = new ForeignAmount("JPY", new BigDecimal("1100"), new BigDecimal("110"));
        ledger.add(new Transaction(type, foreign, "Original"));
        return ledger;
    }

    private void assertConversion(TransactionList ledger, String amount, String rate, String sgd) {
        ForeignAmount foreign = ledger.get(0).getForeignAmount().orElseThrow();
        assertEquals("JPY", foreign.currencyCode());
        assertEquals(0, new BigDecimal(amount).compareTo(foreign.amount()));
        assertEquals(0, new BigDecimal(rate).compareTo(foreign.rate()));
        assertEquals(Money.of(sgd), ledger.get(0).getAmount());
    }

    private ScriptedUi execute(String input, TransactionList ledger) throws FinNusException {
        ScriptedUi ui = new ScriptedUi();
        Parser.parse(input, TODAY).execute(ledger, ui);
        return ui;
        //@@author
    }
}
