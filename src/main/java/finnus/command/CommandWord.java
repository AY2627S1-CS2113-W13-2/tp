package finnus.command;

import java.util.Locale;
import java.util.Optional;

/**
 * Defines command metadata shared by the parser and help table.
 * Declaration order determines the order of commands in help output.
 */
public enum CommandWord {
    DEPOSIT("deposit", "Record income", "AMOUNT [d/DESCRIPTION]"),
    WITHDRAW("withdraw", "Record spending", "AMOUNT d/DESCRIPTION"),
    RECENT("recent", "Recent activity", "[NUMBER]"),
    DELETE("delete", "Delete a record", "INDEX"),
    OPENING("opening", "Opening balance", "AMOUNT"),
    BALANCE("balance", "Current balance", ""),
    RATE("rate", "Manage FX rates", "[CURRENCY RATE]"),
    HELP("help", "List commands", ""),
    EXIT("exit", "Close FinNUS", "");

    private final String word;
    private final String description;
    private final String argumentSyntax;

    CommandWord(String word, String description, String argumentSyntax) {
        this.word = word;
        this.description = description;
        this.argumentSyntax = argumentSyntax;
    }

    /**
     * Returns the command word accepted by the parser.
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

    /**
     * Looks up a command word independently of the machine's default locale.
     *
     * @param word The command word without surrounding whitespace or arguments; must not be null.
     * @return The matching command, or an empty optional if the word is unknown.
     */
    public static Optional<CommandWord> fromWord(String word) {
        String normalizedWord = word.toLowerCase(Locale.ROOT);
        for (CommandWord command : values()) {
            if (command.word.equals(normalizedWord)) {
                return Optional.of(command);
            }
        }
        return Optional.empty();
    }
}
