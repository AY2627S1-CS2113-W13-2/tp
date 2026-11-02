package finnus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.ScriptedUi;
import finnus.command.EditCommand;
import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;

//@@author naieel04
/** Exercises edit syntax through the public parser using a deterministic current date. */
class EditParserTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    @ParameterizedTest
    @ValueSource(strings = {"edit 1 a/5", "EDIT 1 d/New description", "eDiT 1 t/food",
        "edit 1 on/2026-11-03", "edit 1 d/", "edit 1 a/1000000", "edit 0001 a/1",
        "edit 1 r/110", "edit 1 a/110000000 r/110", "edit 1 r/0.000001", "edit 1 r/1000000"})
    void parse_validEdit_returnsCommand(String input) throws FinNusException {
        assertInstanceOf(EditCommand.class, Parser.parse(input, TODAY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"edit", "edit 1", "edit 0 a/5", "edit -1 a/5", "edit abc a/5",
        "edit 2147483648 a/5", "edit 1 2 a/5", "edit +1 a/5", "edit 1 extra",
        "edit 1 a/", "edit 1 t/", "edit 1 on/", "edit 1 c/SGD",
        "edit 1 a/1 a/2", "edit 1 d/A d/B", "edit 1 t/food t/health",
        "edit 1 on/2026-10-01 on/2026-10-02", "edit 1 a/5 junk", "edit 1 t/two words",
        "edit 1 a/0", "edit 1 a/-5", "edit 1 a/1.234", "edit 1 a/NaN",
        "edit 1 r/", "edit 1 r/0", "edit 1 r/-1", "edit 1 r/NaN", "edit 1 r/0.1234567",
        "edit 1 r/1000000.000001", "edit 1 r/110 r/112", "edit 1 r/ d/Income",
        "edit 1 a/$5", "edit 1 a/1e3", "edit 1 a/1,000", "edit 1 t/bad/name",
        "edit 1 on/2026-02-30", "edit 1 on/2026-11-04", "edit 1 on/1999-12-31",
        "edit 1 d/Lunch c/USD", "edit 1 a/ d/Lunch", "edit 1 on/ t/food"})
    void parse_invalidEdit_throwsWithoutExecuting(String input) {
        assertThrows(FinNusException.class, () -> Parser.parse(input, TODAY));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "edit 1 a/6.50 d/Chicken rice t/food on/2026-10-25",
        "edit 1 on/2026-10-25 t/food d/Chicken rice a/6.50",
        "edit 1 d/Chicken rice on/2026-10-25 a/6.50 t/food"})
    void parse_fieldsInDifferentOrders_appliesAllFields(String input) throws FinNusException {
        TransactionList ledger = new TransactionList();
        ledger.add(new Transaction(TransactionType.WITHDRAWAL, Money.of("4.50"), "Lunch"));
        Parser.parse(input, TODAY).execute(ledger, new ScriptedUi());
        Transaction updated = ledger.get(0);
        assertEquals(Money.of("6.50"), updated.getAmount());
        assertEquals("Chicken rice", updated.getDescription());
        assertEquals("Food", updated.getCategory().orElseThrow());
        assertEquals(LocalDate.of(2026, 10, 25), updated.getDate().orElseThrow());
    }

    @Test
    void parse_descriptionWithSlashInsideWord_preservesText() throws FinNusException {
        TransactionList ledger = new TransactionList();
        ledger.add(new Transaction(TransactionType.DEPOSIT, Money.of("5"), "Old"));
        Parser.parse("edit 1 d/Work and/or scholarship", TODAY).execute(ledger, new ScriptedUi());
        assertEquals("Work and/or scholarship", ledger.get(0).getDescription());
        //@@author
    }
}
