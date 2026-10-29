//@@author chancheese
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

import finnus.allowance.Allowance;
import finnus.money.Money;
import finnus.storage.LedgerCodec.Status;

class LedgerCodecAllowanceTest {
    private static final LocalDate CREDITED = LocalDate.of(2026, 10, 5);

    private final LedgerCodec codec = new LedgerCodec();

    private static LedgerSnapshot withAllowance(Allowance allowance) {
        return new LedgerSnapshot(Money.ZERO, List.of(), Map.of(), List.of(), allowance);
    }

    @Test
    public void encode_allowanceVariants_roundTrips() {
        for (Allowance allowance : List.of(
                new Allowance(5, new BigDecimal("500.50"), null, CREDITED),
                new Allowance(31, new BigDecimal("60000"), "JPY", CREDITED),
                new Allowance(1, null, null, CREDITED))) {
            LedgerCodec.DecodeResult decoded = codec.decode(codec.encode(withAllowance(allowance)));

            assertEquals(Status.CLEAN, decoded.status());
            assertEquals(allowance, decoded.snapshot().allowance());
        }
    }

    @Test
    public void encode_allowance_writesReadableRecord() {
        String encoded = codec.encode(withAllowance(new Allowance(5, new BigDecimal("500"), null, CREDITED)));

        assertTrue(encoded.contains("ALLOWANCE|5|500||2026-10-05\n"));
    }

    @Test
    public void decode_fileWithoutAllowance_loadsWithAllowanceUnset() {
        LedgerCodec.DecodeResult decoded = codec.decode("FINNUS|1\nOPENING|0\n");

        assertEquals(Status.CLEAN, decoded.status());
        assertNull(decoded.snapshot().allowance());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALLOWANCE|0|500||2026-10-05", "ALLOWANCE|32|500||2026-10-05",
        "ALLOWANCE|5|-500||2026-10-05", "ALLOWANCE|5|0||2026-10-05", "ALLOWANCE|5|1.234||2026-10-05",
        "ALLOWANCE|5|1000001||2026-10-05", "ALLOWANCE|5||JPY|2026-10-05", "ALLOWANCE|5|500|JYP|2026-10-05",
        "ALLOWANCE|5|500|SGD|2026-10-05", "ALLOWANCE|5|500||", "ALLOWANCE|5|500||1999-12-31",
        "ALLOWANCE|5|500||-2026-10-05", "ALLOWANCE|5|500", "ALLOWANCE|x|500||2026-10-05"})
    public void decode_invalidAllowance_skipsRowWithWarning(String row) {
        LedgerCodec.DecodeResult decoded = codec.decode("FINNUS|1\nOPENING|0\n" + row + "\n");

        assertEquals(Status.PARTIAL, decoded.status());
        assertNull(decoded.snapshot().allowance());
        assertTrue(decoded.warnings().getFirst().startsWith("Skipped row 3"));
    }

    @Test
    public void decode_duplicateAllowance_keepsFirst() {
        LedgerCodec.DecodeResult decoded = codec.decode("FINNUS|1\nOPENING|0\n"
                + "ALLOWANCE|5|500||2026-10-05\nALLOWANCE|10|||2026-10-10\n");

        assertEquals(Status.PARTIAL, decoded.status());
        assertEquals(5, decoded.snapshot().allowance().day());
    }
}
//@@author
