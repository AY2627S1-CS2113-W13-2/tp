//@@author chancheese
package finnus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.allowance.Allowance;
import finnus.exception.FinNusException;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Checks parsing of {@code allowance}, using a fixed "today" so the tests never depend on the real date.
 */
class ParserAllowanceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

    /** Parses and runs the input, returning the allowance it saved. */
    private static Allowance runAndGetAllowance(String input) throws FinNusException {
        TransactionList transactions = new TransactionList();
        Parser.parse(input, TODAY).execute(transactions, new Ui());
        return transactions.getAllowance().orElseThrow();
    }

    @Test
    public void parse_dayOnly_setsDayWithoutAmount() throws FinNusException {
        Allowance allowance = runAndGetAllowance("allowance 5");

        assertEquals(5, allowance.day());
        assertEquals(null, allowance.amount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"allowance 5 a/500", "allowance a/500 5", "ALLOWANCE 5 a/500 c/sgd", "allowance 5 a/ 500",
        "allowance 5   a/   500   c/ SGD"})
    public void parse_sgdAmountAnyOrder_setsAmount(String input) throws FinNusException {
        Allowance allowance = runAndGetAllowance(input);

        assertEquals(5, allowance.day());
        assertEquals(new BigDecimal("500"), allowance.amount());
        assertEquals(null, allowance.currencyCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"allowance 5 c/jpy a/60000", "allowance 5 a/ 60000 c/ JPY"})
    public void parse_foreignAmount_setsCurrency(String input) throws FinNusException {
        Allowance allowance = runAndGetAllowance(input);

        assertEquals(new BigDecimal("60000"), allowance.amount());
        assertEquals("JPY", allowance.currencyCode());
    }

    @Test
    public void parse_off_removesAllowance() throws FinNusException {
        TransactionList transactions = new TransactionList();
        Parser.parse("allowance 5", TODAY).execute(transactions, new Ui());

        Parser.parse("allowance OFF", TODAY).execute(transactions, new Ui());

        assertTrue(transactions.getAllowance().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"allowance 0", "allowance 32", "allowance -5", "allowance 5.5", "allowance five",
        "allowance 99999999999", "allowance 5 6", "allowance a/500", "allowance 5 a/", "allowance 5 a/0",
        "allowance 5 a/abc", "allowance 5 a/1000001", "allowance 5 a/500 a/600", "allowance 5 c/JPY",
        "allowance 5 a/500 c/JYP", "allowance off now", "allowance a/ 5", "allowance 5 a/ c/JPY",
        "allowance 5 a/500 a/ 600"})
    public void parse_invalidAllowance_throwsException(String input) {
        assertThrows(FinNusException.class, () -> Parser.parse(input, TODAY));
    }

    @Test
    public void parse_invalidDay_explainsValidRange() {
        FinNusException e = assertThrows(FinNusException.class, () -> Parser.parse("allowance 32", TODAY));

        assertEquals("'32' is not a valid allowance day. Use a whole number from 1 to 31, e.g. 5.", e.getMessage());
    }

    @Test
    public void parse_extraWords_explainsFormat() {
        FinNusException e = assertThrows(FinNusException.class, () -> Parser.parse("allowance 5 d/x", TODAY));

        assertTrue(e.getMessage().startsWith("Give the day as one number"));
    }

    @Test
    public void parse_currencyWithoutAmount_explainsWhy() {
        FinNusException e = assertThrows(FinNusException.class, () -> Parser.parse("allowance 5 c/JPY", TODAY));

        assertTrue(e.getMessage().startsWith("A currency needs an amount"));
    }
}
//@@author
