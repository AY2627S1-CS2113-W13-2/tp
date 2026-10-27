package finnus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.command.BalanceCommand;
import finnus.command.Command;
import finnus.command.CommandWord;
import finnus.command.DeleteCommand;
import finnus.command.DepositCommand;
import finnus.command.ExitCommand;
import finnus.command.HelpCommand;
import finnus.command.RateCommand;
import finnus.command.RecentTransactionCommand;
import finnus.command.WithdrawCommand;
import finnus.exception.FinNusException;
import finnus.money.Money;

/**
 * Checks command recognition and argument validation in the parser.
 */
class ParserTest {
    private static final String FORMAT = "test format";

    /**
     * Independent user inputs and expected types ensure every enum entry has parser coverage.
     */
    private static final Map<CommandWord, CommandExample> COMMAND_EXAMPLES = Map.of(
            CommandWord.DEPOSIT, new CommandExample("deposit 500 d/Allowance", DepositCommand.class),
            CommandWord.WITHDRAW, new CommandExample("withdraw 4.50 d/Lunch", WithdrawCommand.class),
            CommandWord.RECENT, new CommandExample("recent 3", RecentTransactionCommand.class),
            CommandWord.DELETE, new CommandExample("delete 1", DeleteCommand.class),
            CommandWord.BALANCE, new CommandExample("balance", BalanceCommand.class),
            CommandWord.RATE, new CommandExample("rate JPY 110", RateCommand.class),
            CommandWord.HELP, new CommandExample("help", HelpCommand.class),
            CommandWord.EXIT, new CommandExample("exit", ExitCommand.class));

    /**
     * Pairs literal user input with the concrete command the parser must produce.
     */
    private record CommandExample(String input, Class<? extends Command> expectedClass) {
    }

    @ParameterizedTest
    @EnumSource(value = CommandWord.class, names = "OPENING", mode = EnumSource.Mode.EXCLUDE)
    public void parse_registeredCommand_returnsExpectedType(CommandWord command) throws FinNusException {
        CommandExample example = COMMAND_EXAMPLES.get(command);
        assertNotNull(example, "Add a valid input example for " + command);
        assertInstanceOf(example.expectedClass(), Parser.parse(example.input()));

        String[] parts = example.input().split(" ", 2);
        String arguments = parts.length > 1 ? parts[1] : "";
        String mixedCaseWord = parts[0].substring(0, 1).toUpperCase(Locale.ROOT) + parts[0].substring(1);
        String[] wordVariations = {parts[0], parts[0].toUpperCase(Locale.ROOT), mixedCaseWord};
        for (String word : wordVariations) {
            assertInstanceOf(example.expectedClass(), Parser.parse(" \t" + word + "\t" + arguments + " \t"));
        }
    }

    @Test
    @ResourceLock("java.util.Locale")
    public void parse_turkishLocale_acceptsCommandsAndCurrency() throws FinNusException {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertInstanceOf(ExitCommand.class, Parser.parse("EXIT extra"));
            assertInstanceOf(WithdrawCommand.class, Parser.parse("WITHDRAW 5 d/Lunch"));
            assertInstanceOf(DepositCommand.class, Parser.parse("DEPOSIT 5"));
            assertInstanceOf(RateCommand.class, Parser.parse("RATE inr 65"));
            assertEquals("INR", Parser.parseCurrencyCode("inr"));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @ParameterizedTest
    @EnumSource(value = CommandWord.class, names = {"DEPOSIT", "WITHDRAW", "RATE"})
    public void parse_invalidArguments_reportsRegisteredUsage(CommandWord command) {
        Map<CommandWord, String> invalidInputs = Map.of(
                CommandWord.DEPOSIT, "deposit",
                CommandWord.WITHDRAW, "withdraw 1",
                CommandWord.RATE, "rate JPY");
        FinNusException exception = assertThrows(FinNusException.class,
                () -> Parser.parse(invalidInputs.get(command)));
        assertTrue(exception.getMessage().contains("Format: " + command.getUsage()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"delete", "delete 0", "delete -1", "delete abc", "delete 1 2",
        "delete 2147483648", "recent 2147483648"})
    public void parse_invalidIntegerArguments_throwsException(String input) {
        assertThrows(FinNusException.class, () -> Parser.parse(input));
    }

    @Test
    public void parse_unknownCommand_preservesErrorMessage() {
        FinNusException exception = assertThrows(FinNusException.class, () -> Parser.parse("  NOPE 123  "));
        assertEquals("Unknown command: 'nope'. Type 'help' to see all commands.", exception.getMessage());
    }

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
    public void parse_help_returnsHelpCommand() throws FinNusException {
        assertInstanceOf(HelpCommand.class, Parser.parse("help"));
    }

    @Test
    public void parse_helpInUpperCase_returnsHelpCommand() throws FinNusException {
        assertInstanceOf(HelpCommand.class, Parser.parse("HELP"));
    }

    @Test
    public void parse_helpWithSurroundingWhitespace_returnsHelpCommand() throws FinNusException {
        assertInstanceOf(HelpCommand.class, Parser.parse("  help  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"help 123", "help deposit", "help anything else", "  HELP\t123  "})
    public void parse_helpWithArguments_returnsHelpCommand(String input) throws FinNusException {
        assertInstanceOf(HelpCommand.class, Parser.parse(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"exit 123", "exit anything else", "  EXIT\t123  "})
    public void parse_exitWithArguments_returnsExitCommand(String input) throws FinNusException {
        assertInstanceOf(ExitCommand.class, Parser.parse(input));
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
        assertEquals(Money.of("4.50"), Parser.parseAmount("4.50", FORMAT));
        assertEquals(Money.of("500"), Parser.parseAmount("500", FORMAT));
        assertEquals(Money.of("0.01"), Parser.parseAmount("0.01", FORMAT));
        assertEquals(Money.of("1000000"), Parser.parseAmount("1000000", FORMAT));
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

    @Test
    public void parse_openingOutsideSetup_isUnknownCommand() {
        FinNusException exception = assertThrows(FinNusException.class, () -> Parser.parse("opening 100"));
        assertTrue(exception.getMessage().contains("Unknown command"));
    }

    @Test
    public void parseNonNegativeAmount_zero_isAccepted() throws FinNusException {
        assertEquals(Money.ZERO, Parser.parseNonNegativeAmount("0", FORMAT));
        assertEquals(Money.ZERO, Parser.parseNonNegativeAmount("0.00", FORMAT));
    }

    @Test
    public void parseAmount_zero_throwsGreaterThanZeroMessage() {
        FinNusException e = assertThrows(FinNusException.class, () -> Parser.parseAmount("0", FORMAT));
        assertEquals("Amount must be greater than 0.", e.getMessage());
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

    @ParameterizedTest
    @ValueSource(strings = {"   ", " abc", " 123 abc"})
    public void parse_validBalanceCommands_returnsBalanceCommand(String str) {
        Command cmd = assertDoesNotThrow(() -> Parser.parse("balance" + str));
        assertInstanceOf(BalanceCommand.class, cmd);
    }

    @Test
    public void parse_balanceCommandWithLeadingWhitespace_returnsBalanceCommand() {
        Command cmd = assertDoesNotThrow(() -> Parser.parse("  balance"));
        assertInstanceOf(BalanceCommand.class, cmd);
    }
}
