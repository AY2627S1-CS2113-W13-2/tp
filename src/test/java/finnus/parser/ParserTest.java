package finnus.parser;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import finnus.command.DepositCommand;
import finnus.command.ExitCommand;
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
}
