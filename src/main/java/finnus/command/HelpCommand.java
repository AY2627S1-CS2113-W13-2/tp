package finnus.command;

import java.util.logging.Logger;

import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Displays the available commands and their syntax without changing transactions.
 */
public class HelpCommand extends Command {
    private static final Logger LOGGER = Logger.getLogger(HelpCommand.class.getName());

    /**
     * Manually maintained usage instructions. Update this list whenever a command
     * is added, removed, or its syntax changes.
     */
    private static final String[] HELP_LINES = {
        "Available commands:",
        "+----------+-----------------+--------------------------------+",
        "| Command  | Description     | Syntax                         |",
        "+----------+-----------------+--------------------------------+",
        "| deposit  | Record income   | deposit AMOUNT [d/DESCRIPTION] |",
        "| withdraw | Record spending | withdraw AMOUNT d/DESCRIPTION  |",
        "| recent   | Recent activity | recent [NUMBER]                |",
        "| delete   | Delete a record | delete INDEX                   |",
        "| opening  | Opening balance | opening AMOUNT                 |",
        "| balance  | Current balance | balance                        |",
        "| rate     | Manage FX rates | rate [CURRENCY RATE]           |",
        "| help     | List commands   | help                           |",
        "| exit     | Close FinNUS    | exit                           |",
        "+----------+-----------------+--------------------------------+",
        "",
        "[] means optional."
    };

    @Override
    public void execute(TransactionList transactions, Ui ui) {
        ui.showMessage(HELP_LINES);
        LOGGER.fine("Displayed help message");
    }
}
