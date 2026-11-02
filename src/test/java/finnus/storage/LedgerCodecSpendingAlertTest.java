package finnus.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.alert.SpendingAlert;
import finnus.money.Money;
import finnus.storage.LedgerCodec.Status;

//@@author lololol8484
/** Checks encoding, decoding and recovery of optional spending-alert settings. */
class LedgerCodecSpendingAlertTest {
    private final LedgerCodec codec = new LedgerCodec();

    @Test
    public void encode_bothAlertSettings_roundTrips() {
        LedgerSnapshot snapshot = snapshot(new SpendingAlert(Money.of("100.50"), new BigDecimal("90")));

        LedgerCodec.DecodeResult decoded = codec.decode(codec.encode(snapshot));

        assertEquals(Status.CLEAN, decoded.status());
        assertEquals(snapshot, decoded.snapshot());
    }

    @Test
    public void encode_alertSettings_writesReadableRecord() {
        String encoded = codec.encode(snapshot(new SpendingAlert(Money.of("100"), new BigDecimal("90"))));

        assertTrue(encoded.contains("SPENDING_ALERT|100|90\n"));
    }

    @Test
    public void decode_legacyFile_defaultsAlertSettingsToUnset() {
        LedgerCodec.DecodeResult decoded = codec.decode("FINNUS|1\nOPENING|100\n");

        assertEquals(Status.CLEAN, decoded.status());
        assertTrue(decoded.snapshot().spendingAlert().balanceThreshold().isEmpty());
        assertTrue(decoded.snapshot().spendingAlert().allowancePercentage().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SPENDING_ALERT|0|", "SPENDING_ALERT|1000001|", "SPENDING_ALERT||0",
        "SPENDING_ALERT||101", "SPENDING_ALERT||90.123", "SPENDING_ALERT|abc|90", "SPENDING_ALERT|100"})
    public void decode_invalidSettings_skipsRowWithWarning(String row) {
        LedgerCodec.DecodeResult decoded = codec.decode("FINNUS|1\nOPENING|100\n" + row + "\n");

        assertEquals(Status.PARTIAL, decoded.status());
        assertTrue(decoded.snapshot().spendingAlert().balanceThreshold().isEmpty());
        assertTrue(decoded.snapshot().spendingAlert().allowancePercentage().isEmpty());
        assertTrue(decoded.warnings().getFirst().startsWith("Skipped row 3"));
    }

    @Test
    public void decode_duplicateSettings_keepsFirst() {
        LedgerCodec.DecodeResult decoded = codec.decode("FINNUS|1\nOPENING|100\n"
                + "SPENDING_ALERT|100|90\nSPENDING_ALERT|50|80\n");

        assertEquals(Status.PARTIAL, decoded.status());
        assertEquals(Money.of("100"), decoded.snapshot().spendingAlert().balanceThreshold().orElseThrow());
        assertEquals(new BigDecimal("90"), decoded.snapshot().spendingAlert().allowancePercentage().orElseThrow());
    }

    private static LedgerSnapshot snapshot(SpendingAlert spendingAlert) {
        return new LedgerSnapshot(Money.of("100"), List.of(), Map.of(), List.of(), null, spendingAlert);
    }
}
//@@author
