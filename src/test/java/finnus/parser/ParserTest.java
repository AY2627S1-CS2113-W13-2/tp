package finnus.parser;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.command.DepositCommand;
import finnus.command.ExitCommand;
import finnus.command.RateCommand;
import finnus.command.WithdrawCommand;
import finnus.exception.FinNusException;

class ParserTest {
    private static final String FORMAT = "test format";

    @Test
    public void parse_depositWithDescription_returnsDepositCommand() throws FinNusException {
        assertInstanceOf(DepositCommand.class, Parser.parse("deposit 500 d/Monthly Allowance"));
    }

    @Test
    public void parse_depositWithoutDescription_returnsDepositCommand() throws FinNusException {
        assertInstanceOf(DepositCommand.class, Parser.parse("deposit 500"));
    }

    @Test
    public void parse_commandWordInUpperCase_isAccepted() throws FinNusException {
        assertInstanceOf(DepositCommand.class, Parser.parse("  DEPOSIT 500  "));
    }

    @Test
    public void parse_validWithdraw_returnsWithdrawCommand() throws FinNusException {
        assertInstanceOf(WithdrawCommand.class, Parser.parse("withdraw 4.50 d/Lunch"));
    }

    @Test
    public void parse_withdrawWithoutDescription_throwsException() {
        FinNusException e = assertThrows(FinNusException.class, () -> Parser.parse("withdraw 4.50"));
        assertTrue(e.getMessage().contains("description"));
    }

    @Test
    public void parse_withdrawWithBlankDescription_throwsException() {
        assertThrows(FinNusException.class, () -> Parser.parse("withdraw 4.50 d/   "));
    }

    @Test
    public void parse_exit_returnsExitCommand() throws FinNusException {
        assertInstanceOf(ExitCommand.class, Parser.parse("exit"));
    }

    @Test
    public void parse_unknownCommand_throwsException() {
        assertThrows(FinNusException.class, () -> Parser.parse("spend 5"));
    }

    @Test
    public void splitAmountAndDescription_amountFirst_splitsCorrectly() throws FinNusException {
        assertArrayEquals(new String[] {"4.50", "Chicken rice"},
                Parser.splitAmountAndDescription("4.50 d/Chicken rice", FORMAT));
    }

    @Test
    public void splitAmountAndDescription_descriptionFirst_splitsCorrectly() throws FinNusException {
        assertArrayEquals(new String[] {"4.50", "Chicken rice"},
                Parser.splitAmountAndDescription("d/Chicken rice 4.50", FORMAT));
    }

    @Test
    public void splitAmountAndDescription_noPrefix_returnsEmptyDescription() throws FinNusException {
        assertArrayEquals(new String[] {"500", ""}, Parser.splitAmountAndDescription("500", FORMAT));
    }

    @Test
    public void splitAmountAndDescription_slashInsideWord_notTreatedAsPrefix() throws FinNusException {
        assertArrayEquals(new String[] {"10", "food and/or drinks"},
                Parser.splitAmountAndDescription("10 d/food and/or drinks", FORMAT));
    }

    @Test
    public void parseAmount_validAmounts_parsedCorrectly() throws FinNusException {
        assertEquals(new BigDecimal("4.50"), Parser.parseAmount("4.50", FORMAT));
        assertEquals(new BigDecimal("500"), Parser.parseAmount("500", FORMAT));
        assertEquals(new BigDecimal("0.01"), Parser.parseAmount("0.01", FORMAT));
        assertEquals(new BigDecimal("1000000"), Parser.parseAmount("1000000", FORMAT));
    }

    @Test
    public void parseAmount_invalidAmounts_throwException() {
        String[] invalidAmounts = {"", "abc", "-5", "0", "0.00", "4.555", "1e3", "NaN", "Infinity", "$5",
            "1000000.01"};
        for (String amount : invalidAmounts) {
            assertThrows(FinNusException.class, () -> Parser.parseAmount(amount, FORMAT),
                    "Expected an exception for amount: '" + amount + "'");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"1 2", "abc", "4.5", "1e3", "NaN", "Infinity"})
    public void parseRecent_invalidArgumentFormat_throwException(String str) {
        FinNusException e = assertThrows(FinNusException.class, () -> Parser.parse("recent " + str));
        assertEquals("Invalid argument for recent.", e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5"})
    public void parseRecent_invalidArgumentValue_throwsException(String str) {
        FinNusException e = assertThrows(FinNusException.class, () -> Parser.parse("recent "+ str));
        assertEquals("The length must be positive.", e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"rate", "rate JPY 110", "rate jpy 0.5425", "  RATE  MYR   3.45  "})
    public void parse_validRate_returnsRateCommand(String input) throws FinNusException {
        assertInstanceOf(RateCommand.class, Parser.parse(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"rate JPY", "rate 110", "rate JPY 110 extra"})
    public void parse_rateWithWrongArgumentCount_throwsException(String input) {
        FinNusException e = assertThrows(FinNusException.class, () -> Parser.parse(input));
        assertTrue(e.getMessage().contains("Format: rate [CURRENCY RATE]"));
    }

    @Test
    public void parseCurrencyCode_lowerCase_returnsUpperCase() throws FinNusException {
        assertEquals("JPY", Parser.parseCurrencyCode("jpy"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"JYP", "YEN", "JP", "1234", ""})
    public void parseCurrencyCode_unknownCode_throwsException(String code) {
        assertThrows(FinNusException.class, () -> Parser.parseCurrencyCode(code));
    }

    @Test
    public void parseCurrencyCode_sgd_throwsException() {
        FinNusException e = assertThrows(FinNusException.class, () -> Parser.parseCurrencyCode("sgd"));
        assertEquals("Amounts are already in SGD, so SGD does not need a rate.", e.getMessage());
    }

    @Test
    public void parseRateValue_validRates_parsedCorrectly() throws FinNusException {
        assertEquals(new BigDecimal("110"), Parser.parseRateValue("110"));
        assertEquals(new BigDecimal("0.5425"), Parser.parseRateValue("0.5425"));
        assertEquals(new BigDecimal("0.000001"), Parser.parseRateValue("0.000001"));
        assertEquals(new BigDecimal("1000000"), Parser.parseRateValue("1000000"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.000000", "-1", "abc", "1.1234567", "1e3", "1000000.1"})
    public void parseRateValue_invalidRates_throwException(String rate) {
        assertThrows(FinNusException.class, () -> Parser.parseRateValue(rate));
    }
}
