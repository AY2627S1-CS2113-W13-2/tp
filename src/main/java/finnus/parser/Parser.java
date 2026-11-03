package finnus.parser;

import java.math.BigDecimal;
//@@author nkyjStar
import java.math.BigInteger;
//@@author
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Currency;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import finnus.ValidationRegex;
import finnus.allowance.Allowance;
import finnus.category.CategoryList;
import finnus.command.AlertCommand;
import finnus.command.AllowanceCommand;
import finnus.command.BalanceCommand;
import finnus.command.CategoryCommand;
//@@author nkyjStar
import finnus.command.ChartFlowCommand;
import finnus.command.ChartPeriod;
import finnus.command.ChartSpendingCommand;
//@@author
import finnus.command.Command;
import finnus.command.CommandWord;
import finnus.command.DeleteCommand;
import finnus.command.DepositCommand;
import finnus.command.ExitCommand;
import finnus.command.FindCommand;
import finnus.command.ForeignTransactionCommand;
import finnus.command.HelpCommand;
import finnus.command.HistoryCommand;
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

    /**
     * Prefix that marks the amount of an allowance, e.g. {@code a/500}. A prefix is used so that
     * the day and the amount cannot be swapped by mistake, e.g. {@code allowance 25 5}.
     */
    public static final String AMOUNT_PREFIX = "a/";
    //@@author

    //@@author nabijonzzz
    /**
     * Prefix that marks the category of a withdrawal, e.g. {@code t/food}.
     * {@code c/} is already used for currency, so {@code t/} (tag) is used instead.
     */
    public static final String CATEGORY_PREFIX = "t/";

    /**
     * Prefix that marks the date of a transaction, e.g. {@code on/2026-10-30}.
     */
    public static final String DATE_PREFIX = "on/";

    /**
     * The earliest date a transaction can have. FinNUS tracks a student's current spending, so
     * anything earlier is almost certainly a typo, e.g. 0001-01-01.
     */
    public static final LocalDate EARLIEST_DATE = LocalDate.of(2000, 1, 1);

    /**
     * Exactly {@code YYYY-MM-DD}. Checked before parsing because {@code LocalDate.parse} also accepts
     * signed years such as {@code -2026-01-01} and {@code +2026-01-01}.
     */
    public static final String DATE_FORMAT_REGEX = "\\d{4}-\\d{2}-\\d{2}";
    //@@author

    /** Separates command arguments at one or more whitespace characters. */
    private static final String WHITESPACE_SEPARATOR_REGEX = "\\s+";

    /** Non-negative amount with at most two decimal places. */
    private static final String AMOUNT_REGEX = "\\d+(\\.\\d{1,2})?";

    /** Exchange rate with at most six decimal places. */
    private static final String RATE_REGEX = "\\d+(\\.\\d{1,6})?";

    /** A negative plain decimal, used to give a specific validation error. */
    private static final String NEGATIVE_AMOUNT_REGEX = "-\\d+(\\.\\d+)?";

    //@@author chancheese
    /**
     * Matches {@code c/CODE} at the start of a word, allowing whitespace after {@code c/}.
     * Captures the code, which may be empty.
     */
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("(?<!\\S)" + CURRENCY_PREFIX
            + "\\s*(\\S*)");

    /**
     * Matches {@code a/AMOUNT} at the start of a word, allowing whitespace after {@code a/} like
     * {@code c/} and {@code t/}. Captures the amount, which may be empty.
     */
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?<!\\S)" + AMOUNT_PREFIX
            + "\\s*(\\S*)");

    /** Turns off the allowance, as in {@code allowance off}. */
    private static final String ALLOWANCE_OFF = "off";
    //@@author

    //@@author nabijonzzz
    /**
     * Matches {@code t/NAME} at the start of a word, allowing whitespace after {@code t/}.
     * Captures the name, which may be empty.
     */
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("(?<!\\S)" + CATEGORY_PREFIX
            + "\\s*(\\S*)");

    /** Matches {@code on/DATE} at the start of a word, capturing the date (which may be empty). */
    private static final Pattern DATE_PATTERN = Pattern.compile("(?<!\\S)" + DATE_PREFIX + "\\s*(\\S*)");

    /** A date safely in the past, so the example remains valid on future runs. */
    private static final String DATE_EXAMPLE = DATE_PREFIX + "2026-01-15";

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
        return parse(input, LocalDate.now());
    }

    //@@author nabijonzzz
    /**
     * Parses a full line of user input, using {@code today} as the date of transactions
     * without {@code on/DATE}. Taking the date as a parameter lets tests use a fixed day.
     *
     * @param input The raw text typed by the user.
     * @param today The current date.
     * @return The command to execute.
     * @throws FinNusException If the command word is unknown or its arguments are invalid.
     */
    public static Command parse(String input, LocalDate today) throws FinNusException {
        assert today != null : "Today's date must not be null";
        String trimmed = input.trim();
        // Split into the command word and everything after it (which may be empty).
        String[] parts = trimmed.split(WHITESPACE_SEPARATOR_REGEX, 2);
        String commandWord = parts[0].toLowerCase(Locale.ROOT);
        String arguments = parts.length > 1 ? parts[1].trim() : "";
        logger.fine("Parsing command word '" + commandWord + "'");

        CommandWord command = CommandWord.fromWord(commandWord).orElseThrow(()
                -> new FinNusException("Unknown command: '" + commandWord + "'. Type '"
                        + CommandWord.HELP.getWord() + "' to see all commands."));

        // Setup-only command words are filtered out by CommandWord.fromWord.
        Command parsed = switch (command) {
        case DEPOSIT ->
            parseDeposit(arguments, today);
        case WITHDRAW ->
            parseWithdraw(arguments, today);
        case HELP ->
            new HelpCommand();
        case RECENT ->
            parseRecent(arguments);
        //@@author nkyjStar
        case HISTORY ->
            parseHistory(arguments);
        //@@author
        //@@author nkyjStar
        case CHART ->
            parseChart(arguments, today);
        //@@author
        case FIND ->
            parseFind(arguments);
        case BALANCE ->
            new BalanceCommand(today);
        //@@author chancheese
        case ALLOWANCE ->
            parseAllowance(arguments, today);
        //@@author
        //@@author lololol8484
        case ALERT ->
            parseAlert(arguments);
        //@@author
        case EXIT ->
            new ExitCommand();
        case DELETE ->
            parseDelete(arguments);
        //@@author naieel04
        case EDIT ->
            EditParser.parse(arguments, today);
        //@@author
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

    //@@author chancheese
    /**
     * Parses {@code find [KEYWORD] [t/CATEGORY]}. Everything after the command word, apart from
     * an optional {@code t/CATEGORY} part, is the keyword, so a phrase such as
     * {@code find chicken rice} is searched as a whole.
     *
     * @throws FinNusException If neither a keyword nor a category is given, or the category
     *     part is invalid.
     */
    private static Command parseFind(String arguments) throws FinNusException {
        // t/ is read exactly as for withdraw, so the same category names and errors apply.
        String[] categoryAndRest = extractCategory(arguments);
        String categoryName = categoryAndRest[0];
        String keyword = categoryAndRest[1].trim();
        if (categoryName == null && keyword.isEmpty()) {
            throw new FinNusException("Missing keyword or category to search for, e.g. "
                    + CommandWord.FIND.getWord() + " lunch or " + CommandWord.FIND.getWord() + " "
                    + CATEGORY_PREFIX + "food.\nFormat: " + CommandWord.FIND.getUsage());
        }
        return new FindCommand(keyword.isEmpty() ? null : keyword, categoryName);
    }
    //@@author

    //@@author chancheese
    /**
     * Parses {@code allowance} (show the summary), {@code allowance off} (stop it), or
     * {@code allowance DAY [a/AMOUNT] [c/CURRENCY]} (set it). Like {@code c/}, the
     * {@code a/} part may appear anywhere after the command word.
     *
     * @throws FinNusException If the day, amount or currency is invalid, or {@code c/} is
     *     given without an amount.
     */
    private static Command parseAllowance(String arguments, LocalDate today) throws FinNusException {
        if (arguments.isEmpty()) {
            return AllowanceCommand.show(today);
        }
        if (arguments.equalsIgnoreCase(ALLOWANCE_OFF)) {
            return AllowanceCommand.stop(today);
        }
        String format = "\nFormat: " + CommandWord.ALLOWANCE.getUsage();
        //@@author lololol8484
        validatePrefixedValue(arguments, CURRENCY_PATTERN,
                "Missing currency after " + CURRENCY_PREFIX + ", e.g. c/JPY.");
        validatePrefixedValue(arguments, AMOUNT_PATTERN,
                "Missing amount after " + AMOUNT_PREFIX + ", e.g. " + AllowanceCommand.EXAMPLE + "." + format);
        //@@author
        String[] currencyAndRest = extractCurrency(arguments);
        String currencyCode = currencyAndRest[0];
        String[] amountAndRest = extractPrefixedValue(currencyAndRest[1], AMOUNT_PATTERN,
                "Give only one amount, e.g. " + AllowanceCommand.EXAMPLE + "." + format,
                "Missing amount after " + AMOUNT_PREFIX + ", e.g. " + AllowanceCommand.EXAMPLE + "." + format);
        String amountText = amountAndRest[0];
        // c/SGD is treated like no currency, so only a real foreign currency needs a/.
        if (currencyCode != null && amountText == null) {
            throw new FinNusException("A currency needs an amount, e.g. allowance 5 a/60000 c/JPY." + format);
        }
        // Anything left besides the day, e.g. "5 d/x" or "5 A/500", is not part of the format.
        if (amountAndRest[1].contains(" ")) {
            throw new FinNusException("Give the day as one number, optionally with a/AMOUNT and c/CURRENCY, e.g. "
                    + AllowanceCommand.EXAMPLE + "." + format);
        }
        int day = parseAllowanceDay(amountAndRest[1]);
        if (amountText == null) {
            return AllowanceCommand.set(day, null, null, today);
        }
        BigDecimal amount = currencyCode == null
                ? parseAmount(amountText, CommandWord.ALLOWANCE.getUsage()).toBigDecimal()
                : parseForeignAmount(amountText, CommandWord.ALLOWANCE.getUsage());
        return AllowanceCommand.set(day, amount, currencyCode, today);
    }

    //@@author lololol8484
    /** Parses {@code alert} and the optional balance or allowance threshold settings. */
    private static Command parseAlert(String arguments) throws FinNusException {
        if (arguments.isBlank()) {
            return new AlertCommand();
        }
        String[] parts = arguments.trim().split(WHITESPACE_SEPARATOR_REGEX);
        if (parts.length != 2) {
            throw invalidAlertFormat();
        }
        String setting = parts[0].toLowerCase(Locale.ROOT);
        String value = parts[1];
        if (setting.equals("balance")) {
            return value.equalsIgnoreCase("off")
                    ? AlertCommand.clearBalance()
                    : AlertCommand.setBalance(parseAmount(value, CommandWord.ALERT.getUsage()));
        }
        if (setting.equals("allowance")) {
            if (value.equalsIgnoreCase("off")) {
                return AlertCommand.clearAllowancePercentage();
            }
            if (!value.matches(ValidationRegex.PERCENTAGE_REGEX)) {
                throw invalidAllowancePercentage();
            }
            int percentage = Integer.parseInt(value);
            if (percentage < 1 || percentage > 100) {
                throw invalidAllowancePercentage();
            }
            return AlertCommand.setAllowancePercentage(BigDecimal.valueOf(percentage));
        }
        throw invalidAlertFormat();
    }

    /** Builds the shared syntax error for malformed alert commands. */
    private static FinNusException invalidAlertFormat() {
        return new FinNusException("Use alert to view settings, or set one with: "
                + "alert balance AMOUNT or off, or alert allowance PERCENT or off.\nFormat: "
                + CommandWord.ALERT.getUsage());
    }

    /** Builds the validation error for an allowance alert percentage. */
    private static FinNusException invalidAllowancePercentage() {
        return new FinNusException("Allowance alert percentage must be a whole number from 1 to 100.");
    }
    //@@author

    /**
     * Converts text such as {@code "5"} into an allowance day from 1 to 31.
     * Also used by first-run setup.
     *
     * @throws FinNusException If the text is not a whole number from 1 to 31.
     */
    public static int parseAllowanceDay(String text) throws FinNusException {
        String day = text.trim();
        if (day.isEmpty()) {
            throw new FinNusException("Missing allowance day, e.g. " + AllowanceCommand.EXAMPLE + ".\nFormat: "
                    + CommandWord.ALLOWANCE.getUsage());
        }
        // At most 2 digits, so a huge number cannot overflow Integer.parseInt.
        if (!day.matches(ValidationRegex.ALLOWANCE_DAY_REGEX) || Integer.parseInt(day) < Allowance.MIN_DAY
                || Integer.parseInt(day) > Allowance.MAX_DAY) {
            throw new FinNusException("'" + day + "' is not a valid allowance day. Use a whole number from "
                    + Allowance.MIN_DAY + " to " + Allowance.MAX_DAY + ", e.g. 5.");
        }
        return Integer.parseInt(day);
    }
    //@@author

    //@@author nkyjStar
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
     * Parses optional page size and page fields, allowing either order but no duplicates.
     *
     * @param arguments The text after {@code history}, or blank for the default page and size.
     * @return A history command configured with the requested page and page size.
     * @throws FinNusException If a field is invalid, duplicated, or unrecognized.
     */
    private static Command parseHistory(String arguments) throws FinNusException {
        assert arguments != null : "History arguments must not be null";
        int pageSize = HistoryCommand.DEFAULT_PAGE_SIZE;
        int page = 1;
        boolean hasPageSize = false;
        boolean hasPage = false;
        if (arguments.isBlank()) {
            return new HistoryCommand(pageSize, page);
        }

        for (String rawField : arguments.split(WHITESPACE_SEPARATOR_REGEX)) {
            String field = rawField.toLowerCase(Locale.ROOT);
            if (field.startsWith("n/") && !hasPageSize) {
                pageSize = HistoryCommand.parsePositiveNumber(field.substring(2), "page size");
                hasPageSize = true;
            } else if (field.startsWith("p/") && !hasPage) {
                page = HistoryCommand.parsePositiveNumber(field.substring(2), "page");
                hasPage = true;
            } else {
                throw new FinNusException("Invalid history arguments. Format: "
                        + CommandWord.HISTORY.getUsage());
            }
        }

        return new HistoryCommand(pageSize, page);
    }
    //@@author

    //@@author nkyjStar
    /**
     * Parses the two chart forms and rejects missing, duplicate, or unrecognized fields.
     *
     * @param arguments The chart subcommand and its fields.
     * @param today The latest date accepted in a chart request.
     * @return The parsed flow or spending command.
     * @throws FinNusException If the subcommand or any field is invalid.
     */
    private static Command parseChart(String arguments, LocalDate today) throws FinNusException {
        String[] parts = arguments.split(WHITESPACE_SEPARATOR_REGEX, 2);
        String mode = parts[0].toLowerCase(Locale.ROOT);
        String fields = parts.length == 2 ? parts[1].trim() : "";
        try {
            return switch (mode) {
            case "flow" -> parseChartFlow(fields, today);
            case "spending" -> parseChartSpending(fields, today);
            default -> throw new FinNusException("Invalid chart command. Format: " + CommandWord.CHART.getUsage());
            };
        } catch (FinNusException e) {
            String chartMode = mode.equals("flow") || mode.equals("spending") ? mode : "unknown";
            logger.warning("Rejected " + chartMode + " chart command: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Parses an inclusive date range, a shared chart period, and an optional page.
     *
     * @param arguments The fields following {@code chart flow}.
     * @param today The latest date accepted in the range.
     * @return A flow command for the requested range and page.
     * @throws FinNusException If a field, date, range, period, or page is invalid.
     */
    private static Command parseChartFlow(String arguments, LocalDate today) throws FinNusException {
        String fromText = null;
        String toText = null;
        String periodText = null;
        String pageText = null;
        for (String field : arguments.split(WHITESPACE_SEPARATOR_REGEX)) {
            int slash = field.indexOf('/');
            String prefix = slash < 0 ? "" : field.substring(0, slash).toLowerCase(Locale.ROOT);
            String value = slash < 0 ? "" : field.substring(slash + 1);
            switch (prefix) {
            case "from" -> {
                if (fromText != null) {
                    throw invalidChartArguments("flow");
                }
                fromText = value;
            }
            case "to" -> {
                if (toText != null) {
                    throw invalidChartArguments("flow");
                }
                toText = value;
            }
            case "int" -> {
                if (periodText != null) {
                    throw invalidChartArguments("flow");
                }
                periodText = value;
            }
            case "p" -> {
                if (pageText != null) {
                    throw invalidChartArguments("flow");
                }
                pageText = value;
            }
            default -> throw invalidChartArguments("flow");
            }
        }
        if (fromText == null || toText == null || periodText == null) {
            throw invalidChartArguments("flow");
        }
        LocalDate from = parseChartDate(fromText, today);
        LocalDate to = parseChartDate(toText, today);
        if (from.isAfter(to)) {
            throw new FinNusException("Chart start date must be on or before end date.");
        }
        int page = pageText == null ? 1 : parseChartPage(pageText);
        return new ChartFlowCommand(from, to, ChartPeriod.parse(periodText), page);
    }

    /**
     * Parses overall spending or one day, week, month, or year containing a date.
     *
     * @param arguments The optional fields following {@code chart spending}.
     * @param today The latest date accepted for a dated chart.
     * @return A spending command for all records or the selected calendar period.
     * @throws FinNusException If the optional fields, period, or date are invalid.
     */
    private static Command parseChartSpending(String arguments, LocalDate today) throws FinNusException {
        if (arguments.isBlank()) {
            return new ChartSpendingCommand(null, null);
        }
        String periodText = null;
        String dateText = null;
        for (String field : arguments.split(WHITESPACE_SEPARATOR_REGEX)) {
            int slash = field.indexOf('/');
            String prefix = slash < 0 ? "" : field.substring(0, slash).toLowerCase(Locale.ROOT);
            String value = slash < 0 ? "" : field.substring(slash + 1);
            switch (prefix) {
            case "int" -> {
                if (periodText != null) {
                    throw invalidChartArguments("spending");
                }
                periodText = value;
            }
            case "on" -> {
                if (dateText != null) {
                    throw invalidChartArguments("spending");
                }
                dateText = value;
            }
            default -> throw invalidChartArguments("spending");
            }
        }
        if (periodText == null || dateText == null) {
            throw invalidChartArguments("spending");
        }
        return new ChartSpendingCommand(ChartPeriod.parse(periodText), parseChartDate(dateText, today));
    }

    /**
     * Checks chart dates using the same limits as transaction dates.
     *
     * @param text The date in YYYY-MM-DD format.
     * @param today The latest date permitted.
     * @return The validated date.
     * @throws FinNusException If the date is malformed or outside the permitted range.
     */
    private static LocalDate parseChartDate(String text, LocalDate today) throws FinNusException {
        if (!text.matches(DATE_FORMAT_REGEX)) {
            throw new FinNusException("'" + text + "' is not a valid date. Use YYYY-MM-DD.");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new FinNusException("'" + text + "' is not a valid date. Use YYYY-MM-DD.");
        }
        if (date.isBefore(EARLIEST_DATE) || date.isAfter(today)) {
            throw new FinNusException("Chart dates must be from " + EARLIEST_DATE + " through " + today + ".");
        }
        return date;
    }

    /**
     * Converts a one-based page number without overflowing an int.
     *
     * @param text The page number as decimal digits.
     * @return The positive page number.
     * @throws FinNusException If the number is invalid, zero, or too large for an int.
     */
    private static int parseChartPage(String text) throws FinNusException {
        if (!text.matches(ValidationRegex.UNSIGNED_INTEGER_REGEX) || new BigInteger(text).signum() == 0) {
            throw new FinNusException("Chart page must be a positive whole number.");
        }
        BigInteger number = new BigInteger(text);
        if (number.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new FinNusException("Chart page is out of range.");
        }
        return number.intValue();
    }

    /**
     * Gives a form-specific syntax hint for invalid chart fields.
     *
     * @param mode The chart form, either flow or spending.
     * @return An exception with the matching usage syntax.
     */
    private static FinNusException invalidChartArguments(String mode) {
        String syntax = mode.equals("flow")
                ? "chart flow from/START to/END int/PERIOD [p/PAGE]"
                : "chart spending [int/PERIOD on/DATE]";
        return new FinNusException("Invalid chart arguments. Format: " + syntax);
    }
    //@@author

    /**
     * Parses {@code rate} (list all rates) or {@code rate CURRENCY RATE} (set
     * one rate).
     */
    private static Command parseRate(String arguments) throws FinNusException {
        if (arguments.isEmpty()) {
            return new RateCommand();
        }
        String[] parts = arguments.split(WHITESPACE_SEPARATOR_REGEX);
        if (parts.length != 2) {
            //@@author naieel04
            throw new FinNusException("Give a currency and a rate, e.g. " + CommandWord.RATE.getWord()
                    + " JPY 110.\nFormat: " + CommandWord.RATE.getUsage());
            //@@author
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
        //@@author naieel04
        String code = text.toUpperCase(Locale.ROOT);
        //@@author
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
        if (!text.matches(RATE_REGEX)) {
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
    private static Command parseDeposit(String arguments, LocalDate today) throws FinNusException {
        //@@author lololol8484
        validatePrefixedValue(arguments, CURRENCY_PATTERN,
                "Missing currency after " + CURRENCY_PREFIX + ", e.g. c/JPY.");
        validatePrefixedValue(arguments, DATE_PATTERN,
                "Missing date after " + DATE_PREFIX + ", e.g. " + DATE_EXAMPLE + ".");
        //@@author
        //@@author nabijonzzz
        if (CATEGORY_PATTERN.matcher(arguments).find()) {
            throw new FinNusException("Categories are only for spending, so " + CATEGORY_PREFIX
                    + " cannot be used with " + CommandWord.DEPOSIT.getWord() + ".");
        }
        DateAndRest dateAndRest = extractDate(arguments, today);
        LocalDate date = dateAndRest.date();
        //@@author chancheese
        String[] currencyAndRest = extractCurrency(dateAndRest.rest());
        String currencyCode = currencyAndRest[0];
        String[] amountAndDescription = splitAmountAndDescription(currencyAndRest[1],
                CommandWord.DEPOSIT.getUsage());
        if (currencyCode != null) {
            BigDecimal amount = parseForeignAmount(amountAndDescription[0], CommandWord.DEPOSIT.getUsage());
            return new ForeignTransactionCommand(TransactionType.DEPOSIT, amount, currencyCode,
                    amountAndDescription[1], null, date);
        }
        //@@author
        Money amount = parseAmount(amountAndDescription[0], CommandWord.DEPOSIT.getUsage());
        return new DepositCommand(amount, amountAndDescription[1], date);
    }

    /**
     * Parses a positive withdrawal amount and requires a non-empty description.
     */
    private static Command parseWithdraw(String arguments, LocalDate today) throws FinNusException {
        //@@author lololol8484
        validatePrefixedValue(arguments, CATEGORY_PATTERN,
                "Missing category after " + CATEGORY_PREFIX + ", e.g. " + CATEGORY_PREFIX + "food.");
        validatePrefixedValue(arguments, DATE_PATTERN,
                "Missing date after " + DATE_PREFIX + ", e.g. " + DATE_EXAMPLE + ".");
        validatePrefixedValue(arguments, CURRENCY_PATTERN,
                "Missing currency after " + CURRENCY_PREFIX + ", e.g. c/JPY.");
        //@@author
        //@@author nabijonzzz
        String[] categoryAndRest = extractCategory(arguments);
        String categoryName = categoryAndRest[0];
        DateAndRest dateAndRest = extractDate(categoryAndRest[1], today);
        LocalDate date = dateAndRest.date();
        //@@author chancheese
        String[] currencyAndRest = extractCurrency(dateAndRest.rest());
        String currencyCode = currencyAndRest[0];
        String[] amountAndDescription = splitAmountAndDescription(currencyAndRest[1],
                CommandWord.WITHDRAW.getUsage());
        //@@author
        String description = amountAndDescription[1];
        if (description.isEmpty()) {
            throw new FinNusException("A withdrawal needs a description so you know what you spent on.\n"
                    //@@author naieel04
                    + "Format: " + CommandWord.WITHDRAW.getUsage());
            //@@author
        }
        //@@author chancheese
        if (currencyCode != null) {
            BigDecimal amount = parseForeignAmount(amountAndDescription[0], CommandWord.WITHDRAW.getUsage());
            return new ForeignTransactionCommand(TransactionType.WITHDRAWAL, amount, currencyCode, description,
                    categoryName, date);
        }
        //@@author
        Money amount = parseAmount(amountAndDescription[0], CommandWord.WITHDRAW.getUsage());
        return new WithdrawCommand(amount, description, categoryName, date);
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
        //@@author lololol8484
        if (value.isEmpty() || startsWithRecognisedPrefix(value)) {
            throw new FinNusException(missingMessage);
        }
        //@@author
        return new String[]{value, rest};
    }

    //@@author lololol8484
    /**
     * Checks a prefixed field before any other field is removed from the arguments.
     * This keeps a later prefix from being mistaken for the value of an earlier one.
     *
     * @param arguments The complete argument string before extraction.
     * @param pattern The pattern for the prefix being validated.
     * @param missingMessage The message to show when its value is missing.
     * @throws FinNusException If a prefix is followed by another recognised prefix or no value.
     */
    private static void validatePrefixedValue(String arguments, Pattern pattern, String missingMessage)
            throws FinNusException {
        Matcher matcher = pattern.matcher(arguments);
        while (matcher.find()) {
            String value = matcher.group(1);
            if (value.isEmpty() || startsWithRecognisedPrefix(value)) {
                throw new FinNusException(missingMessage);
            }
        }
    }
    //@@author

    //@@author lololol8484
    /**
     * Recognises a prefix that was supplied where the current prefix's value should be.
     * This prevents input such as {@code t/ d/Lunch} from reporting {@code d/Lunch} as an
     * invalid category instead of reporting the missing category value.
     */
    private static boolean startsWithRecognisedPrefix(String value) {
        return value.startsWith(DESCRIPTION_PREFIX) || value.startsWith(CURRENCY_PREFIX)
                || value.startsWith(AMOUNT_PREFIX) || value.startsWith(CATEGORY_PREFIX)
                || value.startsWith(DATE_PREFIX);
    }
    //@@author

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

    /** The date of a transaction and the arguments left after removing {@code on/DATE}. */
    record DateAndRest(LocalDate date, String rest) {
    }

    /**
     * Removes an optional {@code on/DATE} part from deposit or withdraw arguments,
     * e.g. {@code "4.50 on/2026-10-20 d/Lunch"} becomes {@code [2026-10-20, "4.50 d/Lunch"]}.
     *
     * @param today The date to use if no {@code on/} is given, and the latest date allowed.
     * @return The date (today if none was given) and the remaining arguments.
     * @throws FinNusException If {@code on/} is given more than once, has no date, is not a real
     *     date in {@code YYYY-MM-DD} format, is before {@link #EARLIEST_DATE}, or is in the future.
     */
    static DateAndRest extractDate(String arguments, LocalDate today) throws FinNusException {
        String[] dateAndRest = extractPrefixedValue(arguments, DATE_PATTERN,
                "Give only one date, e.g. " + DATE_EXAMPLE + ".",
                "Missing date after " + DATE_PREFIX + ", e.g. " + DATE_EXAMPLE + ".");
        if (dateAndRest[0] == null) {
            return new DateAndRest(today, arguments);
        }
        String invalidDateMessage = "'" + dateAndRest[0] + "' is not a valid date. Use YYYY-MM-DD, e.g. "
                + DATE_EXAMPLE + ".";
        if (!dateAndRest[0].matches(DATE_FORMAT_REGEX)) {
            throw new FinNusException(invalidDateMessage);
        }
        LocalDate date;
        try {
            // The default ISO format is strict, so it also rejects dates that do not exist, e.g. 2026-02-30.
            date = LocalDate.parse(dateAndRest[0]);
        } catch (DateTimeParseException e) {
            throw new FinNusException(invalidDateMessage);
        }
        if (date.isBefore(EARLIEST_DATE)) {
            throw new FinNusException("The date " + date + " is too early. Transactions can only be recorded from "
                    + EARLIEST_DATE + " onwards.");
        }
        if (date.isAfter(today)) {
            throw new FinNusException("The date " + date + " is in the future. Transactions can only be "
                    + "recorded for today or earlier.");
        }
        return new DateAndRest(date, dateAndRest[1]);
    }

    //@@author lololol8484
    /**
     * Parses {@code category} to list categories or {@code category NAME} to add one.
     */
    private static Command parseCategory(String arguments) throws FinNusException {
        if (arguments.isBlank()) {
            return new CategoryCommand();
        }
        String name = arguments.trim();
        checkCategoryName(name);
        return new CategoryCommand(name);
    }
    //@@author

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
        rejectNegativeAmount(text);
        if (!text.matches(AMOUNT_REGEX)) {
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
        //@@author lololol8484
        int separator = findLastWhitespace(afterPrefix);
        if (separator == -1) {
            throw new FinNusException("Missing amount.\nFormat: " + format);
        }
        String amount = afterPrefix.substring(separator + 1);
        String description = afterPrefix.substring(0, separator).trim();
        //@@author
        return new String[]{amount, description};
    }

    //@@author lololol8484
    /** Returns the final whitespace position, including tabs and other Java whitespace. */
    private static int findLastWhitespace(String text) {
        for (int index = text.length() - 1; index >= 0; index--) {
            if (Character.isWhitespace(text.charAt(index))) {
                return index;
            }
        }
        return -1;
    }
    //@@author

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
    public static Money parseAmount(String text, String format) throws FinNusException {
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
        //@@author chancheese
        rejectNegativeAmount(text);
        //@@author
        // Only plain decimals like 4, 4.5 or 4.50 are accepted; this rejects
        // "1e3", "NaN", "Infinity" and "$5" in one simple check.
        if (!text.matches(AMOUNT_REGEX)) {
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

    //@@author chancheese
    /**
     * Rejects amounts typed with a minus sign, such as {@code "-5"}, with their own
     * message, so users know the sign is the problem rather than the number format.
     * Amounts are always entered as positive numbers; the command decides the sign.
     *
     * @throws FinNusException If the text is a number with a leading minus sign.
     */
    private static void rejectNegativeAmount(String text) throws FinNusException {
        if (text.matches(NEGATIVE_AMOUNT_REGEX)) {
            throw new FinNusException("Amount must not be negative.");
        }
    }
    //@@author
}
