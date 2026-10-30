package finnus.command;

import java.util.Locale;
import java.util.Optional;

/**
 * Defines command metadata shared by the parser and help table.
 * Declaration order determines the order of commands in help output.
 */
public enum CommandWord {
    DEPOSIT("deposit", "Record income", "AMOUNT [d/DESCRIPTION] [c/CURRENCY] [on/DATE]"),
    WITHDRAW("withdraw", "Record spending", "AMOUNT d/DESCRIPTION [c/CURRENCY] [t/CATEGORY] [on/DATE]"),
    RECENT("recent", "Recent activity", "[NUMBER]"),
    FIND("find", "Search descriptions", "KEYWORD"),
    DELETE("delete", "Delete a record", "INDEX"),
    OPENING("opening", "Opening balance (first-run setup only)", "AMOUNT", false),
    BALANCE("balance", "Current balance", ""),
    //@@author chancheese
    ALLOWANCE("allowance", "Monthly allowance", "[DAY [a/AMOUNT] [c/CURRENCY]] or off"),
    //@@author
    RATE("rate", "Manage FX rates", "[CURRENCY RATE]"),
    CATEGORY("category", "Manage categories", "[NAME]"),
    HELP("help", "List commands", ""),
    EXIT("exit", "Close FinNUS", "");

    private final String word;
    private final String description;
    private final String argumentSyntax;
    private final boolean availableAfterSetup;

    CommandWord(String word, String description, String argumentSyntax) {
        this(word, description, argumentSyntax, true);
    }

    CommandWord(String word, String description, String argumentSyntax, boolean availableAfterSetup) {
        this.word = word;
        this.description = description;
        this.argumentSyntax = argumentSyntax;
        this.availableAfterSetup = availableAfterSetup;
    }

    /**
     * Returns the configured text for this command word.
     */
    public String getWord() {
        return word;
    }

    /**
     * Returns the short description shown in the help table.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the full usage syntax without repeating the command word in metadata.
     */
    public String getUsage() {
        return argumentSyntax.isEmpty() ? word : word + " " + argumentSyntax;
    }

    /** Returns whether the command is accepted after first-run setup. */
    public boolean isAvailableAfterSetup() {
        return availableAfterSetup;
    }

    /**
     * Looks up a normal command word independently of the machine's default locale.
     * Setup-only words are treated as unknown in the regular command loop.
     *
     * @param word The command word without surrounding whitespace or arguments; must not be null.
     * @return The matching command, or an empty optional if the word is unknown.
     */
    public static Optional<CommandWord> fromWord(String word) {
        String normalizedWord = word.toLowerCase(Locale.ROOT);
        for (CommandWord command : values()) {
            if (command.availableAfterSetup && command.word.equals(normalizedWord)) {
                return Optional.of(command);
            }
        }
        return Optional.empty();
    }
}
