package finnus.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.currency.ForeignAmount;
import finnus.money.Money;
import finnus.storage.LedgerCodec.Status;
import finnus.storage.LedgerSnapshot.StoredTransaction;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;

/**
 * Checks exact serialization, independent format examples and row recovery.
 */
class LedgerCodecTest {
    private final LedgerCodec codec = new LedgerCodec();

    @Test
    public void encode_emptyLedger_writesVersionAndOpening() {
        assertEquals("FINNUS|1\nOPENING|0\n", codec.encode(LedgerSnapshot.empty()));
        assertEquals(LedgerSnapshot.empty(), codec.decode("FINNUS|1\nOPENING|0\n").snapshot());
    }

    @Test
    public void encode_completeLedger_preservesValuesAndDescriptions() {
        List<StoredTransaction> transactions = List.of(
                new StoredTransaction(TransactionType.DEPOSIT, Money.of("500.00"), "Allowance"),
                new StoredTransaction(TransactionType.WITHDRAWAL, Money.of("0.005"), "|\t\n\u5348\u9910\r\n"),
                new StoredTransaction(TransactionType.DEPOSIT, Money.of("1"), ""));
        LedgerSnapshot snapshot = new LedgerSnapshot(Money.of("12.30"), transactions,
                Map.of("JPY", new BigDecimal("110.00"), "MYR", new BigDecimal("3.450000")));
        String encoded = codec.encode(snapshot);
        assertTrue(encoded.contains("TRANSACTION|DEPOSIT|500.00|QWxsb3dhbmNl|\n"));
        assertTrue(encoded.indexOf("RATE|JPY") < encoded.indexOf("RATE|MYR"));
        assertEquals(snapshot, codec.decode(encoded).snapshot());
        assertEquals(Status.CLEAN, codec.decode(encoded).status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN|x", "OPENING|-1", "RATE|SGD|1", "RATE|JYP|1", "RATE|JPY|0",
        "RATE|JPY|NaN", "RATE|JPY|1E+999999", "RATE|JPY|1000000.01", "RATE|JPY|99999999999",
        "TRANSACTION|OTHER|1|", "TRANSACTION|DEPOSIT|0|", "TRANSACTION|DEPOSIT|-1|",
        "TRANSACTION|DEPOSIT|1000000.01|", "TRANSACTION|DEPOSIT|99999999999|",
        "TRANSACTION|WITHDRAWAL|1000000.01|Lunch", "TRANSACTION|WITHDRAWAL|99999999999|Lunch",
        "TRANSACTION|DEPOSIT|bad|", "TRANSACTION|DEPOSIT|1|!", "TRANSACTION|DEPOSIT|1|/w==",
        "TRANSACTION|DEPOSIT|1", "TRANSACTION|DEPOSIT|1||extra"})
    public void decode_badRow_keepsSurroundingRecords(String badRow) {
        String text = "FINNUS|1\nOPENING|10\nTRANSACTION|DEPOSIT|2|\n" + badRow
                + "\nTRANSACTION|WITHDRAWAL|1|THVuY2g=\n";
        LedgerCodec.DecodeResult result = codec.decode(text);
        assertEquals(Status.PARTIAL, result.status());
        assertEquals(2, result.snapshot().transactions().size());
        assertTrue(result.warnings().getFirst().startsWith("Skipped row 4:"));
        assertFalse(result.warnings().getFirst().contains(badRow));
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPENING|1000000.01", "OPENING|99999999999"})
    public void decode_excessiveOpeningBalance_skipsRowAndDefaultsToZero(String badOpening) {
        String text = "FINNUS|1\n" + badOpening + "\nTRANSACTION|DEPOSIT|2|\n";
        LedgerCodec.DecodeResult result = codec.decode(text);
        assertEquals(Status.PARTIAL, result.status());
        assertEquals(Money.ZERO, result.snapshot().openingBalance());
        assertEquals(1, result.snapshot().transactions().size());
        assertTrue(result.warnings().getFirst().startsWith("Skipped row 2:"));
        assertTrue(result.warnings().getFirst().contains("exceed"));
    }

    @Test
    public void decode_duplicateRecords_keepsFirstSingletonAndAllTransactions() {
        LedgerCodec.DecodeResult result = codec.decode("""
                FINNUS|1
                OPENING|10
                OPENING|20
                RATE|JPY|110
                RATE|JPY|120
                TRANSACTION|DEPOSIT|1|
                TRANSACTION|DEPOSIT|1|
                """);
        assertEquals(Money.of("10"), result.snapshot().openingBalance());
        assertEquals(new BigDecimal("110"), result.snapshot().rates().get("JPY"));
        assertEquals(2, result.snapshot().transactions().size());
        assertEquals(2, result.warnings().size());
    }

    @Test
    public void decode_missingOpening_recoversTransactionsAndWarns() {
        LedgerCodec.DecodeResult result = codec.decode("FINNUS|1\nTRANSACTION|DEPOSIT|1|\n");
        assertEquals(Status.PARTIAL, result.status());
        assertEquals(Money.ZERO, result.snapshot().openingBalance());
        assertTrue(result.warnings().getFirst().contains("Balances may be incomplete"));
    }

    @Test
    public void decode_byteOrderMarkAndWindowsLines_acceptsFormat() {
        assertEquals(Status.CLEAN, codec.decode("\uFEFFFINNUS|1\r\nOPENING|0\r\n\r\n").status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "garbage", "FINNUS|1\n", "FINNUS|1\nBAD|row\n"})
    public void decode_unusableFile_marksUnrecoverable(String text) {
        assertEquals(Status.UNRECOVERABLE, codec.decode(text).status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "10", "100"})
    public void decode_newerVersion_marksUnsupported(String version) {
        assertEquals(Status.UNSUPPORTED, codec.decode("FINNUS|" + version + "\nOPENING|10\n").status());
    }

    @Test
    public void capture_mutatedLedger_doesNotAlterSnapshot() {
        TransactionList ledger = new TransactionList();
        ledger.add(new Transaction(TransactionType.DEPOSIT, Money.of(1), ""));
        ledger.getRates().setRate("JPY", BigDecimal.TEN);
        LedgerSnapshot snapshot = LedgerSnapshot.capture(ledger);
        ledger.delete(0);
        ledger.getRates().setRate("JPY", BigDecimal.ONE);
        ledger.setOpeningBalance(Money.of(10));
        assertEquals(1, snapshot.transactions().size());
        assertEquals(BigDecimal.TEN, snapshot.rates().get("JPY"));
        assertEquals(Money.ZERO, snapshot.openingBalance());
        assertEquals(snapshot, LedgerSnapshot.capture(snapshot.toTransactionList()));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.rates().put("USD", BigDecimal.ONE));
    }

    @Test
    public void storedTransaction_amountExceedsMaxAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new StoredTransaction(TransactionType.DEPOSIT, Money.of("1000000.01"), ""));
        assertThrows(IllegalArgumentException.class, () ->
                new StoredTransaction(TransactionType.WITHDRAWAL, Money.of("99999999999"), "desc"));
    }

    @Test
    public void validateRate_rateExceedsMaxRate_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                LedgerSnapshot.validateRate("JPY", new BigDecimal("1000000.01")));
        assertThrows(IllegalArgumentException.class, () ->
                LedgerSnapshot.validateRate("USD", new BigDecimal("99999999999")));
    }

    @Test
    public void validateOpeningBalance_amountExceedsMaxAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                LedgerSnapshot.validateOpeningBalance(Money.of("1000000.01")));
        assertThrows(IllegalArgumentException.class, () ->
                LedgerSnapshot.validateOpeningBalance(Money.of("99999999999")));
        assertThrows(IllegalArgumentException.class, () ->
                new LedgerSnapshot(Money.of("1000000.01"), List.of(), Map.of()));
    }

    //@@author chancheese
    @Test
    public void encode_foreignTransaction_roundTripsOriginalAmountAndRate() {
        ForeignAmount yen = new ForeignAmount("JPY", new BigDecimal("1100"), new BigDecimal("110.00"));
        LedgerSnapshot snapshot = new LedgerSnapshot(Money.ZERO, List.of(new StoredTransaction(
                TransactionType.WITHDRAWAL, yen.toSgd(), "Ramen", yen)), Map.of());

        String encoded = codec.encode(snapshot);

        assertTrue(encoded.contains("TRANSACTION|WITHDRAWAL|10.00|UmFtZW4=|JPY|1100|110.00|Others\n"));
        assertEquals(snapshot, codec.decode(encoded).snapshot());
        assertEquals(Status.CLEAN, codec.decode(encoded).status());
    }

    @Test
    public void toTransactionList_foreignTransaction_keepsForeignAmount() {
        String text = "FINNUS|1\nOPENING|0\nTRANSACTION|WITHDRAWAL|10.00|UmFtZW4=|JPY|1100|110\n";
        Transaction loaded = codec.decode(text).snapshot().toTransactionList().get(0);
        assertEquals("[Withdrawal] -$10.00 (Ramen) [JPY 1100] #Others", loaded.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRANSACTION|WITHDRAWAL|9.00||JPY|1100|110", "TRANSACTION|WITHDRAWAL|10.00||JYP|1100|110",
        "TRANSACTION|WITHDRAWAL|10.00||SGD|1100|110", "TRANSACTION|WITHDRAWAL|10.00||JPY|0|110",
        "TRANSACTION|WITHDRAWAL|10.00||JPY|1100|0", "TRANSACTION|WITHDRAWAL|10.00||JPY|1100",
        "TRANSACTION|WITHDRAWAL|10.00||JPY|abc|110"})
    public void decode_badForeignTransactionRow_isSkipped(String badRow) {
        String text = "FINNUS|1\nOPENING|0\nTRANSACTION|DEPOSIT|2|\n" + badRow + "\n";
        LedgerCodec.DecodeResult result = codec.decode(text);
        assertEquals(Status.PARTIAL, result.status());
        assertEquals(1, result.snapshot().transactions().size());
    }

    //@@author nabijonzzz
    @Test
    public void encode_categories_roundTripsCustomCategoriesAndWithdrawalCategories() {
        List<StoredTransaction> transactions = List.of(
                new StoredTransaction(TransactionType.WITHDRAWAL, Money.of("4.50"), "Lunch", null, "Food"),
                new StoredTransaction(TransactionType.WITHDRAWAL, Money.of("30"), "Membership", null, "Gym"),
                new StoredTransaction(TransactionType.DEPOSIT, Money.of("500"), "Allowance"));
        LedgerSnapshot snapshot = new LedgerSnapshot(Money.ZERO, transactions, Map.of(), List.of("Gym"));

        String encoded = codec.encode(snapshot);

        assertTrue(encoded.contains("CATEGORY|Gym\n"));
        assertTrue(encoded.contains("TRANSACTION|WITHDRAWAL|4.50|THVuY2g=|Food\n"));
        assertEquals(snapshot, codec.decode(encoded).snapshot());
        assertEquals(Status.CLEAN, codec.decode(encoded).status());
    }

    @Test
    public void decode_rowsSavedBeforeCategories_loadWithdrawalsAsOthersWithoutWarning() {
        String text = "FINNUS|1\nOPENING|0\nTRANSACTION|WITHDRAWAL|4.50|THVuY2g=\n"
                + "TRANSACTION|WITHDRAWAL|10.00|UmFtZW4=|JPY|1100|110\nTRANSACTION|DEPOSIT|1|\n";

        LedgerCodec.DecodeResult result = codec.decode(text);

        assertEquals(Status.CLEAN, result.status());
        assertEquals("Others", result.snapshot().transactions().get(0).category());
        assertEquals("Others", result.snapshot().transactions().get(1).category());
        assertEquals(null, result.snapshot().transactions().get(2).category());
    }

    @Test
    public void decode_unknownCategory_loadsAsOthersWithWarning() {
        String text = "FINNUS|1\nOPENING|0\nTRANSACTION|WITHDRAWAL|4.50|THVuY2g=|Gym\n";

        LedgerCodec.DecodeResult result = codec.decode(text);

        assertEquals(Status.PARTIAL, result.status());
        assertEquals(1, result.snapshot().transactions().size());
        assertEquals("Others", result.snapshot().transactions().getFirst().category());
        assertTrue(result.warnings().getFirst().contains("unknown category 'Gym'"));
    }

    @Test
    public void decode_categoryInDifferentCaseOrListedLater_usesOriginalSpelling() {
        String text = "FINNUS|1\nOPENING|0\nTRANSACTION|WITHDRAWAL|4.50|THVuY2g=|food\n"
                + "TRANSACTION|WITHDRAWAL|30|THVuY2g=|gym\nCATEGORY|Gym\n";

        LedgerCodec.DecodeResult result = codec.decode(text);

        assertEquals(Status.CLEAN, result.status());
        assertEquals("Food", result.snapshot().transactions().get(0).category());
        assertEquals("Gym", result.snapshot().transactions().get(1).category());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CATEGORY|FOOD", "CATEGORY|two words", "CATEGORY|a/b", "CATEGORY|", "CATEGORY|Gym|extra",
        "TRANSACTION|DEPOSIT|1||Food"})
    public void decode_invalidCategoryRecord_skipsRowWithWarning(String badRow) {
        String text = "FINNUS|1\nOPENING|0\nCATEGORY|Gym\n" + badRow + "\n";

        LedgerCodec.DecodeResult result = codec.decode(text);

        assertEquals(Status.PARTIAL, result.status());
        assertEquals(List.of("Gym"), result.snapshot().customCategories());
        assertTrue(result.snapshot().transactions().isEmpty());
    }

    @Test
    public void capture_ledgerWithCategories_restoresCategories() {
        TransactionList ledger = new TransactionList();
        ledger.getCategories().add("Gym");
        ledger.add(new Transaction(TransactionType.WITHDRAWAL, Money.of("30"), "Membership", "Gym"));

        TransactionList restored = codec.decode(codec.encode(LedgerSnapshot.capture(ledger))).snapshot()
                .toTransactionList();

        assertEquals(List.of("Gym"), restored.getCategories().getCustom());
        assertEquals("[Withdrawal] -$30.00 (Membership) #Gym", restored.get(0).toString());
    }
    //@@author

}
