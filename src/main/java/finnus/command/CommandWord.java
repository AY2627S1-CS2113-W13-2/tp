package finnus.command;

//@@author nkyjStar
import java.util.List;
//@@author
import java.util.Locale;
import java.util.Optional;

//@@author naieel04
/**
 * Defines command metadata shared by the parser and help table.
 * Declaration order determines the order of commands in help output.
 */
public enum CommandWord {
    //@@author
    DEPOSIT("deposit", "Record income", "AMOUNT [d/DESCRIPTION] [c/CURRENCY] [on/DATE]"),
    WITHDRAW("withdraw", "Record spending", "AMOUNT d/DESCRIPTION [c/CURRENCY] [t/CATEGORY] [on/DATE]"),
    RECENT("recent", "Recent activity", "[NUMBER]"),
    //@@author nkyjStar
    HISTORY("history", "Transaction history", "[n/NUMBER] [p/PAGE]"),
    //@@author
    //@@author nkyjStar
    CHART("chart", "Visualise finances",
            new HelpEntry("chart flow", "Cash flow by period",
                    "chart flow from/START to/END int/PERIOD [p/PAGE]"),
            new HelpEntry("chart spending", "Spending by category",
                    "chart spending [int/PERIOD on/DATE]")),
    //@@author
    FIND("find", "Search transactions", "[KEYWORD] [t/CATEGORY]"),
    //@@author naieel04
    DELETE("delete", "Delete a record", "INDEX"),
    EDIT("edit", "Edit a record", "INDEX [a/AMOUNT] [d/DESCRIPTION] [t/CATEGORY] [on/DATE] [r/RATE]"),
    //@@author
    OPENING("opening", "Opening balance (first-run setup only)", "AMOUNT", false),
    //@@author naieel04
    BALANCE("balance", "Current balance", ""),
    //@@author
    //@@author chancheese
    ALLOWANCE("allowance", "Monthly allowance", "[DAY [a/AMOUNT] [c/CURRENCY]] or off"),
    //@@author
    //@@author lololol8484
    ALERT("alert", "Manage spending alerts", "[balance AMOUNT or off] or [allowance PERCENT or off]"),
    //@@author
    //@@author naieel04
    RATE("rate", "Manage FX rates", "[CURRENCY RATE]"),
    //@@author
    CATEGORY("category", "Manage categories", "[NAME]"),
    //@@author naieel04
    HELP("help", "List commands", ""),
    EXIT("exit", "Close FinNUS", "");

    private final String word;
    private final String description;
    private final String argumentSyntax;
    //@@author
    private final boolean availableAfterSetup;
    //@@author nkyjStar
    private final List<HelpEntry> helpEntries;
    //@@author

    //@@author naieel04
    CommandWord(String word, String description, String argumentSyntax) {
        this(word, description, argumentSyntax, true);
    }
    //@@author

    //@@author naieel04
    CommandWord(String word, String description, String argumentSyntax, boolean availableAfterSetup) {
        this.word = word;
        this.description = description;
        this.argumentSyntax = argumentSyntax;
        //@@author
        this.availableAfterSetup = availableAfterSetup;
        //@@author nkyjStar
        this.helpEntries = List.of();
        //@@author
    }

    //@@author nkyjStar
    /**
     * Creates a command word with distinct help rows for its subcommands.
     *
     * @param word The command word typed by the user.
     * @param description A short description of the command.
     * @param helpEntries The separate help rows for its subcommands.
     */
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
    //@@author naieel04
    public String getWord() {
        return word;
    }

    /**
     * Returns the short description shown in the help table.
     */
    public String getDescription() {
        return description;
        //@@author
    }

    /**
     * Returns full usage syntax, combining subcommand forms when present.
     *
     * @return The command syntax, or subcommand syntaxes joined by "or".
     */
    //@@author naieel04
    public String getUsage() {
        //@@author
        //@@author nkyjStar
        if (!helpEntries.isEmpty()) {
            return String.join(" or ", helpEntries.stream().map(HelpEntry::usage).toList());
        }
        //@@author
        //@@author naieel04
        return argumentSyntax.isEmpty() ? word : word + " " + argumentSyntax;
        //@@author
    }

    //@@author nkyjStar
    /**
     * Returns the separate rows shown for this command in the help table.
     *
     * @return The configured subcommand rows, or one row for a simple command.
     */
    public List<HelpEntry> getHelpEntries() {
        return helpEntries.isEmpty()
                ? List.of(new HelpEntry(word, description, getUsage())) : helpEntries;
    }

    /**
     * One help-table row before it is wrapped to fit the console width.
     *
     * @param command The command or subcommand label.
     * @param description The short description shown beside it.
     * @param usage The full command syntax.
     */
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
    //@@author naieel04
    public static Optional<CommandWord> fromWord(String word) {
        String normalizedWord = word.toLowerCase(Locale.ROOT);
        for (CommandWord command : values()) {
            //@@author
            if (command.availableAfterSetup && command.word.equals(normalizedWord)) {
                //@@author naieel04
                return Optional.of(command);
            }
        }
        return Optional.empty();
        //@@author
    }
}
