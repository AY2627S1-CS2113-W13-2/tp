package seedu.duke.parser;

import seedu.duke.command.Command;
import seedu.duke.command.DepositCommand;
import seedu.duke.command.ExitCommand;
import seedu.duke.command.WithdrawCommand;
import seedu.duke.exception.FinNusException;

/**
 * Turns a line typed by the user into a {@link Command} object.
 */
public class Parser {
    /** Prefix that marks the start of a description, e.g. {@code d/Lunch}. */
    public static final String DESCRIPTION_PREFIX = "d/";

    /** Amounts above this are almost certainly typos for a student budget. */
    public static final double MAX_AMOUNT = 1_000_000;

    private static final String DEPOSIT_FORMAT = "deposit AMOUNT [d/DESCRIPTION]";
    private static final String WITHDRAW_FORMAT = "withdraw AMOUNT d/DESCRIPTION";

    /**
     * Parses a full line of user input.
     *
     * @param input The raw text typed by the user.
     * @return The command to execute.
     * @throws FinNusException If the command word is unknown or its arguments are invalid.
     */
    public static Command parse(String input) throws FinNusException {
        String trimmed = input.trim();
        // Split into the command word and everything after it (which may be empty).
        String[] parts = trimmed.split("\\s+", 2);
        String commandWord = parts[0].toLowerCase();
        String arguments = parts.length > 1 ? parts[1].trim() : "";

        switch (commandWord) {
        case "deposit":
            return parseDeposit(arguments);
        case "withdraw":
            return parseWithdraw(arguments);
        case "exit":
            return new ExitCommand();
        default:
            throw new FinNusException("Unknown command: '" + commandWord + "'. Type 'help' to see all commands.");
        }
    }

    private static Command parseDeposit(String arguments) throws FinNusException {
        String[] amountAndDescription = splitAmountAndDescription(arguments, DEPOSIT_FORMAT);
        double amount = parseAmount(amountAndDescription[0], DEPOSIT_FORMAT);
        return new DepositCommand(amount, amountAndDescription[1]);
    }

    private static Command parseWithdraw(String arguments) throws FinNusException {
        String[] amountAndDescription = splitAmountAndDescription(arguments, WITHDRAW_FORMAT);
        double amount = parseAmount(amountAndDescription[0], WITHDRAW_FORMAT);
        String description = amountAndDescription[1];
        if (description.isEmpty()) {
            throw new FinNusException("A withdrawal needs a description so you know what you spent on.\n"
                    + "Format: " + WITHDRAW_FORMAT);
        }
        return new WithdrawCommand(amount, description);
    }

    /**
     * Splits arguments such as {@code "4.50 d/Lunch"} into {@code ["4.50", "Lunch"]}.
     * The amount and the {@code d/} part may appear in either order.
     * If there is no {@code d/} part, the description is an empty string.
     */
    static String[] splitAmountAndDescription(String arguments, String format) throws FinNusException {
        int prefixIndex = findDescriptionPrefix(arguments);
        if (prefixIndex == -1) {
            return new String[] {arguments.trim(), ""};
        }

        String beforePrefix = arguments.substring(0, prefixIndex).trim();
        String afterPrefix = arguments.substring(prefixIndex + DESCRIPTION_PREFIX.length()).trim();

        if (!beforePrefix.isEmpty()) {
            // "AMOUNT d/DESCRIPTION": everything after d/ is the description.
            return new String[] {beforePrefix, afterPrefix};
        }
        // "d/DESCRIPTION AMOUNT": the last word is the amount, the rest is the description.
        int lastSpace = afterPrefix.lastIndexOf(' ');
        if (lastSpace == -1) {
            throw new FinNusException("Missing amount.\nFormat: " + format);
        }
        return new String[] {afterPrefix.substring(lastSpace + 1), afterPrefix.substring(0, lastSpace).trim()};
    }

    /**
     * Returns the index of the first {@code d/} that starts a word, or -1 if there is none.
     * This avoids treating text like "and/or" inside a description as a prefix.
     */
    private static int findDescriptionPrefix(String arguments) {
        int index = arguments.indexOf(DESCRIPTION_PREFIX);
        while (index > 0 && !Character.isWhitespace(arguments.charAt(index - 1))) {
            index = arguments.indexOf(DESCRIPTION_PREFIX, index + 1);
        }
        return index;
    }

    /**
     * Converts text such as {@code "4.50"} into a positive amount with at most 2 decimal places.
     *
     * @throws FinNusException If the text is missing, not a number, not positive,
     *     has more than 2 decimal places, or is unreasonably large.
     */
    static double parseAmount(String text, String format) throws FinNusException {
        if (text.isEmpty()) {
            throw new FinNusException("Missing amount.\nFormat: " + format);
        }
        // Only plain decimals like 4, 4.5 or 4.50 are accepted; this rejects
        // "1e3", "NaN", "Infinity", "-5" and "$5" in one simple check.
        if (!text.matches("\\d+(\\.\\d{1,2})?")) {
            throw new FinNusException("'" + text + "' is not a valid amount. "
                    + "Use a positive number with at most 2 decimal places, e.g. 4.50");
        }
        double amount = Double.parseDouble(text);
        if (amount <= 0) {
            throw new FinNusException("Amount must be greater than 0.");
        }
        if (amount > MAX_AMOUNT) {
            throw new FinNusException(String.format("Amount must not exceed %,.0f.", MAX_AMOUNT));
        }
        return amount;
    }
}
