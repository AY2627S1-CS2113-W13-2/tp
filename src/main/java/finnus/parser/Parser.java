package finnus.parser;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import finnus.category.CategoryList;
import finnus.command.BalanceCommand;
import finnus.command.CategoryCommand;
import finnus.command.Command;
import finnus.command.CommandWord;
import finnus.command.DeleteCommand;
import finnus.command.DepositCommand;
import finnus.command.ExitCommand;
import finnus.command.ForeignTransactionCommand;
import finnus.command.HelpCommand;
import finnus.command.RateCommand;
import finnus.command.RecentTransactionCommand;
import finnus.command.WithdrawCommand;
import finnus.currency.CurrencyRates;
import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.TransactionType;

/**
 * Turns a line typed by the user into a {@link Command} object.
 */
public class Parser {

    /**
     * Prefix that marks the start of a description, e.g. {@code d/Lunch}.
     */
    public static final String DESCRIPTION_PREFIX = "d/";

    /**
     * No real currency is anywhere near 1,000,000 units per SGD, so larger
     * rates are typos.
     */
    public static final BigDecimal MAX_RATE = new BigDecimal("1000000");

    //@@author chancheese
    /**
     * Prefix that marks the currency of an amount, e.g. {@code c/JPY}.
     */
    public static final String CURRENCY_PREFIX = "c/";
    //@@author

    //@@author nabijonzzz
    /**
     * Prefix that marks the category of a withdrawal, e.g. {@code t/food}.
     * {@code c/} is already used for currency, so {@code t/} (tag) is used instead.
     */
    public static final String CATEGORY_PREFIX = "t/";
    //@@author

    //@@author chancheese
    /**
     * Matches {@code c/CODE} at the start of a word, capturing the code (which may be empty).
     */
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("(?<!\\S)" + CURRENCY_PREFIX + "(\\S*)");
    //@@author

    //@@author nabijonzzz
    /**
     * Matches {@code t/NAME} at the start of a word, capturing the name (which may be empty).
     */
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("(?<!\\S)" + CATEGORY_PREFIX + "(\\S*)");

    private static final String CATEGORY_LIST = "list";
    private static final String CATEGORY_ADD = "add";
    //@@author

    private static final Logger logger = Logger.getLogger(Parser.class.getName());

    /**
     * Parses a full line of user input. Extra arguments for commands that take
     * no parameters, such as help and exit, are ignored.
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
        String commandWord = parts[0].toLowerCase(Locale.ROOT);
        String arguments = parts.length > 1 ? parts[1].trim() : "";
        logger.fine("Parsing command word '" + commandWord + "'");

        CommandWord command = CommandWord.fromWord(commandWord).orElseThrow(()
                -> new FinNusException("Unknown command: '" + commandWord + "'. Type '"
                        + CommandWord.HELP.getWord() + "' to see all commands."));

        // Setup-only command words are filtered out by CommandWord.fromWord.
        Command parsed = switch (command) {
        case DEPOSIT ->
            parseDeposit(arguments);
        case WITHDRAW ->
            parseWithdraw(arguments);
        case HELP ->
            new HelpCommand();
        case RECENT ->
            parseRecent(arguments);
        case BALANCE ->
            new BalanceCommand();
        case EXIT ->
            new ExitCommand();
        case DELETE ->
            parseDelete(arguments);
        case RATE ->
            parseRate(arguments);
        case CATEGORY ->
            parseCategory(arguments);
        default ->
            throw new FinNusException("Unknown command: '" + commandWord + "'. Type '"
                    + CommandWord.HELP.getWord() + "' to see all commands.");
        };
        logger.fine("Parsed command " + parsed.getClass().getSimpleName());
        return parsed;
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
     * @param arguments Either a blank String, or a positive number parsed as a
     *     String
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
     * Parses {@code rate} (list all rates) or {@code rate CURRENCY RATE} (set
     * one rate).
     */
    private static Command parseRate(String arguments) throws FinNusException {
        if (arguments.isEmpty()) {
            return new RateCommand();
        }
        String[] parts = arguments.split("\\s+");
        if (parts.length != 2) {
            throw new FinNusException("Give a currency and a rate, e.g. " + CommandWord.RATE.getWord()
                    + " JPY 110.\nFormat: " + CommandWord.RATE.getUsage());
        }
        return new RateCommand(parseCurrencyCode(parts[0]), parseRateValue(parts[1]));
    }

    /**
     * Converts text such as {@code "jpy"} into an upper-case ISO 4217 code such
     * as {@code "JPY"}.
     *
     * @throws FinNusException If the code is not a real currency, or is SGD.
     */
    static String parseCurrencyCode(String text) throws FinNusException {
        String code = text.toUpperCase(Locale.ROOT);
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
     * Converts text such as {@code "110"} or {@code "0.5425"} into a positive
     * rate with at most 6 decimal places.
     *
     * @throws FinNusException If the text is not a number, not positive, has
     *     more than 6 decimal places, or is unreasonably large.
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
     * Parses a positive deposit amount and its optional description.
     */
    private static Command parseDeposit(String arguments) throws FinNusException {
        //@@author nabijonzzz
        if (CATEGORY_PATTERN.matcher(arguments).find()) {
            throw new FinNusException("Categories are only for spending, so " + CATEGORY_PREFIX
                    + " cannot be used with " + CommandWord.DEPOSIT.getWord() + ".");
        }
        //@@author chancheese
        String[] currencyAndRest = extractCurrency(arguments);
        String currencyCode = currencyAndRest[0];
        String[] amountAndDescription = splitAmountAndDescription(currencyAndRest[1],
                CommandWord.DEPOSIT.getUsage());
        if (currencyCode != null) {
            BigDecimal amount = parseForeignAmount(amountAndDescription[0], CommandWord.DEPOSIT.getUsage());
            return new ForeignTransactionCommand(TransactionType.DEPOSIT, amount, currencyCode,
                    amountAndDescription[1]);
        }
        //@@author
        Money amount = parseAmount(amountAndDescription[0], CommandWord.DEPOSIT.getUsage());
        return new DepositCommand(amount, amountAndDescription[1]);
    }

    /**
     * Parses a positive withdrawal amount and requires a non-empty description.
     */
    private static Command parseWithdraw(String arguments) throws FinNusException {
        //@@author nabijonzzz
        String[] categoryAndRest = extractCategory(arguments);
        String categoryName = categoryAndRest[0];
        //@@author chancheese
        String[] currencyAndRest = extractCurrency(categoryAndRest[1]);
        String currencyCode = currencyAndRest[0];
        String[] amountAndDescription = splitAmountAndDescription(currencyAndRest[1],
                CommandWord.WITHDRAW.getUsage());
        //@@author
        String description = amountAndDescription[1];
        if (description.isEmpty()) {
            throw new FinNusException("A withdrawal needs a description so you know what you spent on.\n"
                    + "Format: " + CommandWord.WITHDRAW.getUsage());
        }
        //@@author chancheese
        if (currencyCode != null) {
            BigDecimal amount = parseForeignAmount(amountAndDescription[0], CommandWord.WITHDRAW.getUsage());
            return new ForeignTransactionCommand(TransactionType.WITHDRAWAL, amount, currencyCode, description,
                    categoryName);
        }
        //@@author
        Money amount = parseAmount(amountAndDescription[0], CommandWord.WITHDRAW.getUsage());
        return new WithdrawCommand(amount, description, categoryName);
    }

    //@@author chancheese
    /**
     * Removes an optional {@code c/CURRENCY} part from deposit or withdraw arguments,
     * e.g. {@code "1100 c/JPY d/Ramen"} becomes {@code [JPY, "1100 d/Ramen"]}.
     * Like {@code d/}, the {@code c/} prefix only counts when it starts a word, and it
     * may appear anywhere. {@code c/SGD} is treated the same as giving no currency.
     *
     * @return The upper-case currency code (or null for SGD), and the remaining arguments.
     * @throws FinNusException If {@code c/} is given more than once, has no code, or the
     *     code is not a valid currency.
     */
    static String[] extractCurrency(String arguments) throws FinNusException {
        //@@author nabijonzzz
        String[] codeAndRest = extractPrefixedValue(arguments, CURRENCY_PATTERN,
                "Give only one currency, e.g. c/JPY.",
                "Missing currency after " + CURRENCY_PREFIX + ", e.g. c/JPY.");
        String codeText = codeAndRest[0];
        String rest = codeAndRest[1];
        if (codeText == null) {
            return new String[]{null, arguments};
        }
        //@@author chancheese
        if (codeText.equalsIgnoreCase(CurrencyRates.BASE_CURRENCY)) {
            return new String[]{null, rest};
        }
        return new String[]{parseCurrencyCode(codeText), rest};
    }
    //@@author

    //@@author nabijonzzz
    /**
     * Removes one {@code PREFIX/VALUE} word from the arguments. This is shared by {@code c/}
     * and {@code t/}, which both mark a one-word value that may appear anywhere.
     *
     * @param pattern Matches the prefix at the start of a word and captures the value.
     * @param duplicateMessage Error shown if the prefix appears more than once.
     * @param missingMessage Error shown if the prefix has no value after it.
     * @return The value (or null if the prefix is absent), and the remaining arguments.
     * @throws FinNusException If the prefix appears more than once or has no value.
     */
    private static String[] extractPrefixedValue(String arguments, Pattern pattern, String duplicateMessage,
            String missingMessage) throws FinNusException {
        Matcher matcher = pattern.matcher(arguments);
        if (!matcher.find()) {
            return new String[]{null, arguments};
        }
        String value = matcher.group(1);
        String rest = (arguments.substring(0, matcher.start()).trim() + " "
                + arguments.substring(matcher.end()).trim()).trim();
        if (pattern.matcher(rest).find()) {
            throw new FinNusException(duplicateMessage);
        }
        if (value.isEmpty()) {
            throw new FinNusException(missingMessage);
        }
        return new String[]{value, rest};
    }

    /**
     * Removes an optional {@code t/CATEGORY} part from withdraw arguments,
     * e.g. {@code "4.50 t/food d/Lunch"} becomes {@code ["food", "4.50 d/Lunch"]}.
     * Whether the category exists is checked when the command runs, since custom
     * categories live in the transaction list.
     *
     * @return The category name as typed (or null if none was given), and the remaining arguments.
     * @throws FinNusException If {@code t/} is given more than once, has no name, or the
     *     name has an invalid format.
     */
    static String[] extractCategory(String arguments) throws FinNusException {
        String[] nameAndRest = extractPrefixedValue(arguments, CATEGORY_PATTERN,
                "Give only one category, e.g. " + CATEGORY_PREFIX + "food.",
                "Missing category after " + CATEGORY_PREFIX + ", e.g. " + CATEGORY_PREFIX + "food.");
        if (nameAndRest[0] != null) {
            checkCategoryName(nameAndRest[0]);
        }
        return nameAndRest;
    }

    /**
     * Parses {@code category list} or {@code category add NAME}.
     */
    private static Command parseCategory(String arguments) throws FinNusException {
        String[] parts = arguments.split("\\s+", 2);
        String subCommand = parts[0].toLowerCase(Locale.ROOT);
        if (subCommand.equals(CATEGORY_LIST)) {
            return new CategoryCommand();
        }
        if (!subCommand.equals(CATEGORY_ADD)) {
            throw new FinNusException("Use " + CommandWord.CATEGORY.getWord() + " " + CATEGORY_ADD
                    + " NAME or " + CommandWord.CATEGORY.getWord() + " " + CATEGORY_LIST
                    + ".\nFormat: " + CommandWord.CATEGORY.getUsage());
        }
        String name = parts.length > 1 ? parts[1].trim() : "";
        if (name.isEmpty()) {
            throw new FinNusException("Missing category name, e.g. " + CommandWord.CATEGORY.getWord() + " "
                    + CATEGORY_ADD + " Gym.");
        }
        checkCategoryName(name);
        return new CategoryCommand(name);
    }

    /**
     * Checks that a category name is one word made of letters, digits and '-', so it
     * cannot swallow the description or break the save file.
     *
     * @throws FinNusException If the name has an invalid format.
     */
    private static void checkCategoryName(String name) throws FinNusException {
        if (!CategoryList.isValidName(name)) {
            throw new FinNusException("'" + name + "' is not a valid category name. Use one word of letters, "
                    + "digits and '-', at most " + CategoryList.MAX_NAME_LENGTH + " characters.");
        }
    }
    //@@author

    //@@author chancheese

    /**
     * Converts text such as {@code "1100"} into a positive amount in a foreign currency.
     * It uses the same format as SGD amounts, but has no upper limit of its own: the
     * limit is checked on the SGD value once the amount is converted.
     *
     * @throws FinNusException If the text is missing, not a number, not positive, or
     *     has more than 2 decimal places.
     */
    static BigDecimal parseForeignAmount(String text, String format) throws FinNusException {
        if (text.isEmpty()) {
            throw new FinNusException("Missing amount.\nFormat: " + format);
        }
        if (!text.matches("\\d+(\\.\\d{1,2})?")) {
            throw new FinNusException("'" + text + "' is not a valid amount. "
                    + "Use a number with at most 2 decimal places, e.g. 1100");
        }
        BigDecimal amount = new BigDecimal(text);
        if (amount.signum() == 0) {
            throw new FinNusException("Amount must be greater than 0.");
        }
        return amount;
    }
    //@@author

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
     * there is none. This avoids treating text like "and/or" inside a
     * description as a prefix.
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
    static Money parseAmount(String text, String format) throws FinNusException {
        Money amount = parseNonNegativeAmount(text, format);
        if (amount.isZero()) {
            throw new FinNusException("Amount must be greater than 0.");
        }
        return amount;
    }

    /**
     * Converts text such as {@code "4.50"} into an exact amount with at most 2
     * decimal places, allowing 0. Used directly where 0 is meaningful, such as
     * an opening balance.
     *
     * @throws FinNusException If the text is missing, not a number, negative,
     *     has more than 2 decimal places, or is unreasonably large.
     */
    public static Money parseNonNegativeAmount(String text, String format) throws FinNusException {
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
        Money amount = Money.of(new BigDecimal(text));
        if (amount.compareTo(Money.MAX_AMOUNT) > 0) {
            throw new FinNusException("Amount must not exceed " + Money.MAX_AMOUNT.format());
        }
        return amount;
    }
}
