package finnus.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import finnus.command.Command;
import finnus.command.CommandWord;
import finnus.command.EditCommand;
import finnus.command.EditFields;
import finnus.exception.FinNusException;

//@@author naieel04
/** Parses optional edit fields without confusing an omitted description with an empty one. */
final class EditParser {
    /** Reserved prefixes delimit fields even when they occur after a description. */
    private static final Pattern FIELD_PATTERN = Pattern.compile("(?<!\\S)(a/|d/|t/|on/|c/|r/)");

    private EditParser() {
    }

    /** Validates syntax while leaving checks requiring an existing transaction to the command. */
    static Command parse(String arguments, LocalDate today) throws FinNusException {
        String[] parts = arguments.trim().split("\\s+", 2);
        int index = parseIndex(parts[0]);
        if (parts.length != 2) {
            throw invalid("Supply at least one field to edit.");
        }
        Map<String, String> values = extractFields(parts[1]);
        // The currency is only known at execution, so apply the SGD upper limit after conversion.
        BigDecimal amount = values.containsKey("a/") ? Parser.parseForeignAmount(values.get("a/"), usage()) : null;
        BigDecimal rate = values.containsKey("r/") ? Parser.parseRateValue(values.get("r/")) : null;
        String category = parseCategory(values.get("t/"));
        LocalDate date = values.containsKey("on/")
                ? Parser.extractDate("on/" + values.get("on/"), today).date() : null;
        return new EditCommand(index, new EditFields(amount, values.get("d/"), category, date, rate));
    }

    private static int parseIndex(String text) throws FinNusException {
        if (!text.matches("[0-9]+")) {
            throw invalid("Give a positive transaction index.");
        }
        try {
            int index = Integer.parseInt(text);
            if (index > 0) {
                return index;
            }
        } catch (NumberFormatException e) {
            throw invalid("Transaction index is out of range.");
        }
        throw invalid("Give a positive transaction index.");
    }

    /** Splits at reserved prefixes so duplicates and empty single-value fields cannot be swallowed. */
    private static Map<String, String> extractFields(String arguments) throws FinNusException {
        Matcher matcher = FIELD_PATTERN.matcher(arguments);
        if (!matcher.find() || !arguments.substring(0, matcher.start()).isBlank()) {
            throw invalid("Expected an edit field after the index.");
        }
        Map<String, String> values = new HashMap<>();
        String prefix = matcher.group(1);
        int start = matcher.end();
        while (matcher.find()) {
            addField(values, prefix, arguments.substring(start, matcher.start()).trim());
            prefix = matcher.group(1);
            start = matcher.end();
        }
        addField(values, prefix, arguments.substring(start).trim());
        return values;
    }

    private static void addField(Map<String, String> values, String prefix, String value) throws FinNusException {
        if (prefix.equals("c/")) {
            throw invalid("Editing cannot change a transaction's currency.");
        }
        if (values.containsKey(prefix)) {
            throw invalid("Give only one " + prefix + " field.");
        }
        if (!prefix.equals("d/") && (value.isEmpty() || value.matches(".*\\s+.*"))) {
            throw invalid("Give one value after " + prefix + ".");
        }
        values.put(prefix, value);
    }

    private static String parseCategory(String value) throws FinNusException {
        return value == null ? null : Parser.extractCategory("t/" + value)[0];
    }

    private static String usage() {
        return CommandWord.EDIT.getUsage();
    }

    private static FinNusException invalid(String message) {
        return new FinNusException(message + "\nFormat: " + usage());
        //@@author
    }
}
