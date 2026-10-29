//@@author nabijonzzz
package finnus.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.currency.ForeignAmount;
import finnus.money.Money;
import finnus.storage.LedgerCodec.Status;
import finnus.storage.LedgerSnapshot.StoredTransaction;
import finnus.transaction.TransactionType;

class LedgerCodecDateTest {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 20);

    private final LedgerCodec codec = new LedgerCodec();

    @Test
    public void encode_datedTransactions_roundTripsDates() {
        ForeignAmount yen = new ForeignAmount("JPY", new BigDecimal("1100"), new BigDecimal("110"));
        List<StoredTransaction> transactions = List.of(
                new StoredTransaction(TransactionType.DEPOSIT, Money.of("500"), "Allowance", null, null, DATE),
                new StoredTransaction(TransactionType.WITHDRAWAL, yen.toSgd(), "Ramen", yen, "Food", DATE),
                new StoredTransaction(TransactionType.WITHDRAWAL, Money.of("1"), "Old", null, "Others", null));
        LedgerSnapshot snapshot = new LedgerSnapshot(Money.ZERO, transactions, Map.of());

        String encoded = codec.encode(snapshot);

        assertTrue(encoded.contains("TRANSACTION|DEPOSIT|500|QWxsb3dhbmNl||2026-10-20\n"));
        assertTrue(encoded.contains("TRANSACTION|WITHDRAWAL|10.00|UmFtZW4=|JPY|1100|110|Food|2026-10-20\n"));
        assertTrue(encoded.contains("|Others|\n"), "An unknown date is saved as an empty field");
        assertEquals(snapshot, codec.decode(encoded).snapshot());
        assertEquals(Status.CLEAN, codec.decode(encoded).status());
    }

    @Test
    public void decode_rowsWithoutDateField_loadWithUnknownDate() {
        String text = "FINNUS|1\nOPENING|0\nTRANSACTION|WITHDRAWAL|4.50|THVuY2g=\n"
                + "TRANSACTION|WITHDRAWAL|4.50|THVuY2g=|Food\n"
                + "TRANSACTION|WITHDRAWAL|10.00|UmFtZW4=|JPY|1100|110\n"
                + "TRANSACTION|WITHDRAWAL|10.00|UmFtZW4=|JPY|1100|110|Food\n";

        LedgerCodec.DecodeResult result = codec.decode(text);

        assertEquals(Status.CLEAN, result.status());
        assertEquals(4, result.snapshot().transactions().size());
        result.snapshot().transactions().forEach(transaction -> assertNull(transaction.date()));
    }

    @Test
    public void decode_datedRow_keepsDateAndCategory() {
        String text = "FINNUS|1\nOPENING|0\nTRANSACTION|WITHDRAWAL|4.50|THVuY2g=|Food|2026-10-20\n";

        StoredTransaction loaded = codec.decode(text).snapshot().transactions().getFirst();

        assertEquals(DATE, loaded.date());
        assertEquals("Food", loaded.category());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRANSACTION|WITHDRAWAL|4.50|THVuY2g=|Food|2026-02-30",
        "TRANSACTION|WITHDRAWAL|4.50|THVuY2g=|Food|20-10-2026", "TRANSACTION|WITHDRAWAL|4.50|THVuY2g=|Food|today",
        "TRANSACTION|WITHDRAWAL|10.00|UmFtZW4=|JPY|1100|110|Food|2026-13-01",
        "TRANSACTION|WITHDRAWAL|4.50|THVuY2g=|Food|2026-10-20|extra",
        "TRANSACTION|WITHDRAWAL|4.50|THVuY2g=|Food|-2026-01-01",
        "TRANSACTION|WITHDRAWAL|4.50|THVuY2g=|Food|+2026-01-01",
        "TRANSACTION|WITHDRAWAL|4.50|THVuY2g=|Food|0001-01-01", "TRANSACTION|WITHDRAWAL|4.50|THVuY2g=|Food|1999-12-31"})
    public void decode_invalidDate_skipsRowWithWarning(String badRow) {
        String text = "FINNUS|1\nOPENING|0\nTRANSACTION|DEPOSIT|2||\n" + badRow + "\n";

        LedgerCodec.DecodeResult result = codec.decode(text);

        assertEquals(Status.PARTIAL, result.status());
        assertEquals(1, result.snapshot().transactions().size());
    }
}
