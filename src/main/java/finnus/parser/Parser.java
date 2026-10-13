package finnus.parser;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.logging.Logger;

import finnus.command.BalanceCommand;
import finnus.command.Command;
import finnus.command.DeleteCommand;
import finnus.command.DepositCommand;
import finnus.command.ExitCommand;
import finnus.command.HelpCommand;
import finnus.command.OpeningBalanceCommand;
import finnus.command.RateCommand;
import finnus.command.RecentTransactionCommand;
import finnus.command.WithdrawCommand;
import finnus.currency.CurrencyRates;
import finnus.exception.FinNusException;

/**
 * Turns a line typed by the user into a {@link Command} object.
 */
public class Parser {

    /**
     * Prefix that marks the start of a description, e.g. {@code d/Lunch}.
     */
    public static final String DESCRIPTION_PREFIX = "d/";

    /**
     * Amounts above this are almost certainly typos for a student budget.
     */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    /**
     * No real currency is anywhere near 1,000,000 units per SGD, so larger rates are typos.
     */
    public static final BigDecimal MAX_RATE = new BigDecimal("1000000");

    private static final String DEPOSIT_FORMAT = "deposit AMOUNT [d/DESCRIPTION]";
    private static final String WITHDRAW_FORMAT = "withdraw AMOUNT d/DESCRIPTION";
    private static final String OPENING_FORMAT = "opening AMOUNT";
    private static final String RATE_FORMAT = "rate [CURRENCY RATE]";

    private static final Logger logger = Logger.getLogger(Parser.class.getName());

    /**
     * Parses a full line of user input.
     * Extra arguments for commands that take no parameters, such as help and
     * exit, are ignored.
     *
     * @param input The raw text typed by the user.
     * @return The command to execute.
     * @throws FinNusException If the command word is unknown or its arguments
     *     are invalid.
     */
    public static Command parse(String input) throws FinNusException {
        String trimmed = input.trim();
        // Split into the command word and everything after it (which may be empty).
        String[] parts = trimmed.split("\\s+", 2);
        String commandWord = parts[0].toLowerCase();
        String arguments = parts.length > 1 ? parts[1].trim() : "";
        logger.fine("Parsing command word '" + commandWord + "' with arguments '" + arguments + "'");

        switch (commandWord) {
        case "deposit":
            return parseDeposit(arguments);
        case "withdraw":
            return parseWithdraw(arguments);
        case "help":
            return new HelpCommand();
        case "recent":
            return parseRecent(arguments);
        case "opening":
            return parseOpening(arguments);
        case "balance":
            return parseBalance();
        case "exit":
            return new ExitCommand();
        case "delete":
            return parseDelete(arguments);
        case "rate":
            return parseRate(arguments);
        default:
            throw new FinNusException("Unknown command: '" + commandWord + "'. Type 'help' to see all commands.");
        }
    }

    /**
     * Parses the arguments for the delete command.
     *
     * @param arguments The arguments for the delete command.
     * @return The delete command.
     * @throws FinNusException If the arguments are invalid.
     */
    private static Command parseDelete(String arguments) throws FinNusException {
        if (arguments.isBlank()) {
            throw new FinNusException("Missing index of transaction to delete.");
        }
        int index;
        try {
            index = Integer.parseInt(arguments);
        } catch (NumberFormatException e) {
            throw new FinNusException("Invalid argument for delete.");
        }
        if (index <= 0) {
            throw new FinNusException("The index must be positive.");
        }
        return new DeleteCommand(index);
    }

    /**
     * Returns RecentTransactionCommand instance
     * 
     * @param arguments Either a blank String, or a positive number parsed as a String
     */
    private static Command parseRecent(String arguments) throws FinNusException {
        // Create the RecentTransactionCommand with no length provided
        if (arguments.isBlank()) {
            return new RecentTransactionCommand();
        }

        // Catch invalid argument formats
        int length;
        try {
            length = Integer.parseInt(arguments);
        } catch (NumberFormatException e) {
            throw new FinNusException("Invalid argument for recent.");
        }

        if (length <= 0) {
            throw new FinNusException("The length must be positive.");
        }

        return new RecentTransactionCommand(length);
    }

    /**
     * Parses {@code opening AMOUNT}. Unlike deposits, 0 is allowed so the user can reset it.
     */
    private static Command parseOpening(String arguments) throws FinNusException {
        BigDecimal amount = parseNonNegativeAmount(arguments, OPENING_FORMAT);
        return new OpeningBalanceCommand(amount);
    }

    /**
     * Parses {@code rate} (list all rates) or {@code rate CURRENCY RATE} (set one rate).
     */
    private static Command parseRate(String arguments) throws FinNusException {
        if (arguments.isEmpty()) {
            return new RateCommand();
        }
        String[] parts = arguments.split("\\s+");
        if (parts.length != 2) {
            throw new FinNusException("Give a currency and a rate, e.g. rate JPY 110.\nFormat: " + RATE_FORMAT);
        }
        return new RateCommand(parseCurrencyCode(parts[0]), parseRateValue(parts[1]));
    }

    /**
     * Converts text such as {@code "jpy"} into an upper-case ISO 4217 code such as {@code "JPY"}.
     *
     * @throws FinNusException If the code is not a real currency, or is SGD.
     */
    static String parseCurrencyCode(String text) throws FinNusException {
        String code = text.toUpperCase();
        try {
            // Java's built-in currency list lets us reject typos such as "JYP" without
            // maintaining our own list.
            Currency.getInstance(code);
        } catch (IllegalArgumentException e) {
            throw new FinNusException("'" + text + "' is not a valid currency code, e.g. JPY, USD or MYR.");
        }
        if (code.equals(CurrencyRates.BASE_CURRENCY)) {
            throw new FinNusException("Amounts are already in SGD, so SGD does not need a rate.");
        }
        return code;
    }

    /**
     * Converts text such as {@code "110"} or {@code "0.5425"} into a positive rate
     * with at most 6 decimal places.
     *
     * @throws FinNusException If the text is not a number, not positive, has more
     *     than 6 decimal places, or is unreasonably large.
     */
    static BigDecimal parseRateValue(String text) throws FinNusException {
        // Rates need more precision than money, e.g. 1 SGD = 0.5425 GBP.
        if (!text.matches("\\d+(\\.\\d{1,6})?")) {
            throw new FinNusException("'" + text + "' is not a valid rate. "
                    + "Use a number with at most 6 decimal places, e.g. 110 or 0.5425");
        }
        BigDecimal rate = new BigDecimal(text);
        if (rate.signum() == 0) {
            throw new FinNusException("Rate must be greater than 0.");
        }
        if (rate.compareTo(MAX_RATE) > 0) {
            throw new FinNusException(String.format("Rate must not exceed %,.0f.", MAX_RATE));
        }
        return rate;
    }

    /**
     * Returns BalanceCommand instance
     * 
     * @param arguments is expected to be blank
     */
    private static Command parseBalance() {
        return new BalanceCommand();
    }

    private static Command parseDeposit(String arguments) throws FinNusException {
        String[] amountAndDescription = splitAmountAndDescription(arguments, DEPOSIT_FORMAT);
        BigDecimal amount = parseAmount(amountAndDescription[0], DEPOSIT_FORMAT);
        return new DepositCommand(amount, amountAndDescription[1]);
    }

    private static Command parseWithdraw(String arguments) throws FinNusException {
        String[] amountAndDescription = splitAmountAndDescription(arguments, WITHDRAW_FORMAT);
        BigDecimal amount = parseAmount(amountAndDescription[0], WITHDRAW_FORMAT);
        String description = amountAndDescription[1];
        if (description.isEmpty()) {
            throw new FinNusException("A withdrawal needs a description so you know what you spent on.\n"
                    + "Format: " + WITHDRAW_FORMAT);
        }
        return new WithdrawCommand(amount, description);
    }

    /**
     * Splits arguments such as {@code "4.50 d/Lunch"} into
     * {@code ["4.50", "Lunch"]}. The amount and the {@code d/} part may appear
     * in either order. If there is no {@code d/} part, the description is an
     * empty string.
     */
    static String[] splitAmountAndDescription(String arguments, String format) throws FinNusException {
        int prefixIndex = findDescriptionPrefix(arguments);
        if (prefixIndex == -1) {
            return new String[]{arguments.trim(), ""};
        }

        String beforePrefix = arguments.substring(0, prefixIndex).trim();
        String afterPrefix = arguments.substring(prefixIndex + DESCRIPTION_PREFIX.length()).trim();

        if (!beforePrefix.isEmpty()) {
            // "AMOUNT d/DESCRIPTION": everything after d/ is the description.
            return new String[]{beforePrefix, afterPrefix};
        }
        // "d/DESCRIPTION AMOUNT": the last word is the amount, the rest is the
        // description.
        int lastSpace = afterPrefix.lastIndexOf(' ');
        if (lastSpace == -1) {
            throw new FinNusException("Missing amount.\nFormat: " + format);
        }
        return new String[]{afterPrefix.substring(lastSpace + 1), afterPrefix.substring(0, lastSpace).trim()};
    }

    /**
     * Returns the index of the first {@code d/} that starts a word, or -1 if
     *     there is none. This avoids treating text like "and/or" inside a
     *     description as a prefix.
     */
    private static int findDescriptionPrefix(String arguments) {
        int index = arguments.indexOf(DESCRIPTION_PREFIX);
        while (index > 0 && !Character.isWhitespace(arguments.charAt(index - 1))) {
            index = arguments.indexOf(DESCRIPTION_PREFIX, index + 1);
        }
        return index;
    }

    /**
     * Converts text such as {@code "4.50"} into an exact positive amount with
     * at most 2 decimal places.
     *
     * @throws FinNusException If the text is missing, not a number, not
     *     positive, has more than 2 decimal places, or is unreasonably large.
     */
    static BigDecimal parseAmount(String text, String format) throws FinNusException {
        BigDecimal amount = parseNonNegativeAmount(text, format);
        if (amount.signum() == 0) {
            throw new FinNusException("Amount must be greater than 0.");
        }
        return amount;
    }

    /**
     * Converts text such as {@code "4.50"} into an exact amount with at most
     * 2 decimal places, allowing 0. Used directly where 0 is meaningful, such
     * as an opening balance.
     *
     * @throws FinNusException If the text is missing, not a number, negative,
     *     has more than 2 decimal places, or is unreasonably large.
     */
    static BigDecimal parseNonNegativeAmount(String text, String format) throws FinNusException {
        if (text.isEmpty()) {
            throw new FinNusException("Missing amount.\nFormat: " + format);
        }
        // Only plain decimals like 4, 4.5 or 4.50 are accepted; this rejects
        // "1e3", "NaN", "Infinity", "-5" and "$5" in one simple check.
        if (!text.matches("\\d+(\\.\\d{1,2})?")) {
            throw new FinNusException("'" + text + "' is not a valid amount. "
                    + "Use a number with at most 2 decimal places, e.g. 4.50");
        }
        // The String constructor keeps the exact value typed, e.g. "4.50" stays 4.50.
        BigDecimal amount = new BigDecimal(text);
        // BigDecimal is compared with compareTo, not with < or >.
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new FinNusException(String.format("Amount must not exceed %,.0f.", MAX_AMOUNT));
        }
        return amount;
    }
}
