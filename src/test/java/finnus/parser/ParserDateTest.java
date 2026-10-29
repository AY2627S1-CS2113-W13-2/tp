//@@author nabijonzzz
package finnus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.exception.FinNusException;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Checks parsing of {@code on/DATE}, using a fixed "today" so the tests never depend on the real date.
 */
class ParserDateTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 30);

    @Test
    public void parse_noDate_usesToday() throws FinNusException {
        assertEquals(Optional.of(TODAY), runAndGetTransaction("withdraw 4.50 d/Lunch").getDate());
        assertEquals(Optional.of(TODAY), runAndGetTransaction("deposit 500").getDate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"withdraw 4.50 d/Lunch on/2026-10-20", "withdraw on/2026-10-20 4.50 d/Lunch",
        "withdraw 4.50 on/2026-10-20 d/Lunch t/food", "deposit 500 on/2026-10-20 d/Allowance"})
    public void parse_dateAnywhere_usesThatDate(String input) throws FinNusException {
        Transaction transaction = runAndGetTransaction(input);

        assertEquals(Optional.of(LocalDate.of(2026, 10, 20)), transaction.getDate());
        assertTrue(transaction.getDescription().equals("Lunch") || transaction.getDescription().equals("Allowance"));
    }

    @Test
    public void parse_dateWithCurrencyAndCategory_usesAllOfThem() throws FinNusException {
        TransactionList transactions = new TransactionList();
        transactions.getRates().setRate("JPY", new BigDecimal("110"));
        Parser.parse("withdraw 1100 c/JPY d/Ramen t/food on/2026-10-20", TODAY).execute(transactions, new Ui());

        assertEquals("[20 Oct 2026] [Withdrawal] -$10.00 (Ramen) [JPY 1100] #Food", transactions.get(0).toString());
    }

    @Test
    public void parse_today_isAllowed() throws FinNusException {
        assertEquals(Optional.of(TODAY), runAndGetTransaction("withdraw 4.50 d/Lunch on/2026-10-30").getDate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"withdraw 4.50 d/Lunch on/", "withdraw 4.50 d/Lunch on/2026-10-20 on/2026-10-21",
        "withdraw 4.50 d/Lunch on/2026-2-3", "withdraw 4.50 d/Lunch on/2026-02-30",
        "withdraw 4.50 d/Lunch on/30-10-2026",
        "withdraw 4.50 d/Lunch on/yesterday", "withdraw 4.50 d/Lunch on/2026-10-31", "deposit 500 on/2027-01-01",
        "withdraw 4.50 d/Lunch on/-2026-01-01", "withdraw 4.50 d/Lunch on/+2026-01-01",
        "withdraw 4.50 d/Lunch on/0001-01-01", "withdraw 4.50 d/Lunch on/1999-12-31",
        "withdraw 4.50 d/Lunch on/02026-01-01", "withdraw 4.50 d/Lunch on/2026-1-01"})
    public void parse_invalidDate_throwsException(String input) {
        assertThrows(FinNusException.class, () -> Parser.parse(input, TODAY));
    }

    @Test
    public void parse_futureDate_explainsWhy() {
        FinNusException e = assertThrows(FinNusException.class,
                () -> Parser.parse("withdraw 4.50 d/Lunch on/2026-10-31", TODAY));
        assertTrue(e.getMessage().contains("in the future"));
    }

    @Test
    public void extractDate_dateAnywhere_removesItFromArguments() throws FinNusException {
        Parser.DateAndRest result = Parser.extractDate("4.50 on/2026-10-20 d/Lunch", TODAY);
        assertEquals(LocalDate.of(2026, 10, 20), result.date());
        assertEquals("4.50 d/Lunch", result.rest());

        // on/ only counts at the start of a word, so it can appear inside a description.
        Parser.DateAndRest inside = Parser.extractDate("5 d/salon/spa", TODAY);
        assertEquals(TODAY, inside.date());
        assertEquals("5 d/salon/spa", inside.rest());
    }

    /** Parses and runs one command on an empty ledger, and returns the transaction it added. */
    private Transaction runAndGetTransaction(String input) throws FinNusException {
        TransactionList transactions = new TransactionList();
        Parser.parse(input, TODAY).execute(transactions, new Ui());
        assertEquals(1, transactions.size());
        return transactions.get(0);
    }

    @Test
    public void parse_earliestAllowedDate_isAccepted() throws FinNusException {
        assertEquals(Optional.of(LocalDate.of(2000, 1, 1)),
                runAndGetTransaction("withdraw 4.50 d/Lunch on/2000-01-01").getDate());
    }

    @Test
    public void parse_signedYear_isRejectedInsteadOfShownAsAnotherYear() {
        // LocalDate.parse alone accepts "-2026-01-01", which was then shown as [1 Jan 2027].
        FinNusException e = assertThrows(FinNusException.class,
                () -> Parser.parse("withdraw 4.50 d/Lunch on/-2026-01-01", TODAY));
        assertTrue(e.getMessage().contains("not a valid date"));
    }

    @Test
    public void parse_dateBefore2000_explainsWhy() {
        FinNusException e = assertThrows(FinNusException.class,
                () -> Parser.parse("withdraw 4.50 d/Lunch on/1999-12-31", TODAY));
        assertTrue(e.getMessage().contains("too early"));
    }
}
