//@@author naieel04
package finnus.command;

import java.util.ArrayList;
//@@author lololol8484
import java.util.Arrays;
//@@author naieel04
import java.util.List;
import java.util.logging.Logger;

import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Displays the available commands and their syntax without changing transactions.
 */
public class HelpCommand extends Command {
    //@@author nkyjStar
    private static final Logger logger = Logger.getLogger(HelpCommand.class.getName());
    //@@author naieel04
    private static final String COMMAND_HEADER = "Command";
    private static final String DESCRIPTION_HEADER = "Description";
    private static final String SYNTAX_HEADER = "Syntax";
    private static final int COLUMN_PADDING = 2;

    /**
     * Usage instructions generated from {@link CommandWord}. Update the enum when
     * adding commands or changing their descriptions or syntax.
     */
    private static final String[] HELP_LINES = createHelpLines();

    /**
     * Builds an ASCII table whose columns grow to fit the headers and command metadata.
     */
    private static String[] createHelpLines() {
        //@@author lololol8484
        List<CommandWord> availableCommands = Arrays.stream(CommandWord.values())
                .filter(CommandWord::isAvailableAfterSetup)
                .toList();
        //@@author naieel04
        int commandWidth = COMMAND_HEADER.length();
        int descriptionWidth = DESCRIPTION_HEADER.length();
        int syntaxWidth = SYNTAX_HEADER.length();
        //@@author lololol8484
        for (CommandWord command : availableCommands) {
            //@@author naieel04
            commandWidth = Math.max(commandWidth, command.getWord().length());
            descriptionWidth = Math.max(descriptionWidth, command.getDescription().length());
            syntaxWidth = Math.max(syntaxWidth, command.getUsage().length());
        }
        String border = "+" + "-".repeat(commandWidth + COLUMN_PADDING)
                + "+" + "-".repeat(descriptionWidth + COLUMN_PADDING)
                + "+" + "-".repeat(syntaxWidth + COLUMN_PADDING) + "+";
        String rowFormat = "| %-" + commandWidth + "s | %-" + descriptionWidth
                + "s | %-" + syntaxWidth + "s |";
        List<String> lines = new ArrayList<>();
        lines.add("Available commands:");
        lines.add(border);
        lines.add(String.format(rowFormat, COMMAND_HEADER, DESCRIPTION_HEADER, SYNTAX_HEADER));
        lines.add(border);
        //@@author lololol8484
        for (CommandWord command : availableCommands) {
            lines.add(String.format(rowFormat, command.getWord(),
                    command.getDescription(), command.getUsage()));
        }
        //@@author naieel04
        lines.add(border);
        lines.add("");
        lines.add("[] means optional.");
        return lines.toArray(String[]::new);
    }

    //@@author lololol8484
    /** Displays the command reference generated from {@link CommandWord}. */
    //@@author naieel04
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        assert ui != null : "UI must not be null";
        ui.showMessage(HELP_LINES);
        //@@author nkyjStar
        logger.fine("Displayed help message");
    }
}
//@@author
