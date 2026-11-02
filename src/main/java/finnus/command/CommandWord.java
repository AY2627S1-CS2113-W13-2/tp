package finnus.command;

//@@author nkyjStar
import java.util.List;
//@@author
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
    //@@author nkyjStar
    HISTORY("history", "Transaction history", "[n/NUMBER] [p/PAGE]"),
    //@@author
    //@@author nkyjStar
    CHART("chart", "Visualise finances",
            new HelpEntry("chart flow", "Deposits and withdrawals",
                    "chart flow from/START to/END int/PERIOD [p/PAGE]"),
            new HelpEntry("chart spending", "Spending by category",
                    "chart spending [int/PERIOD on/DATE]")),
    //@@author
    FIND("find", "Search transactions", "[KEYWORD] [t/CATEGORY]"),
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
    //@@author nkyjStar
    private final List<HelpEntry> helpEntries;
    //@@author

    CommandWord(String word, String description, String argumentSyntax) {
        this(word, description, argumentSyntax, true);
    }

    CommandWord(String word, String description, String argumentSyntax, boolean availableAfterSetup) {
        this.word = word;
        this.description = description;
        this.argumentSyntax = argumentSyntax;
        this.availableAfterSetup = availableAfterSetup;
        //@@author nkyjStar
        this.helpEntries = List.of();
        //@@author
    }

    //@@author nkyjStar
    /** Creates a command word with distinct help rows for its subcommands. */
    CommandWord(String word, String description, HelpEntry... helpEntries) {
        this.word = word;
        this.description = description;
        this.argumentSyntax = "";
        this.availableAfterSetup = true;
        this.helpEntries = List.of(helpEntries);
    }
    //@@author

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
     * Returns full usage syntax, combining subcommand forms when present.
     */
    public String getUsage() {
        //@@author nkyjStar
        if (!helpEntries.isEmpty()) {
            return String.join(" or ", helpEntries.stream().map(HelpEntry::usage).toList());
        }
        //@@author
        return argumentSyntax.isEmpty() ? word : word + " " + argumentSyntax;
    }

    //@@author nkyjStar
    /** Returns the separate rows shown for this command in the help table. */
    public List<HelpEntry> getHelpEntries() {
        return helpEntries.isEmpty()
                ? List.of(new HelpEntry(word, description, getUsage())) : helpEntries;
    }

    /** One help-table row before it is wrapped to fit the console width. */
    public record HelpEntry(String command, String description, String usage) {
    }
    //@@author

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
