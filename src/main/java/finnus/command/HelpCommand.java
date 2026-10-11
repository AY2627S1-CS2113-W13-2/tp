package finnus.command;

import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Displays the available commands and their syntax without changing transactions.
 */
public class HelpCommand extends Command {
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
        "| help     | List commands   | help                           |",
        "| exit     | Close FinNUS    | exit                           |",
        "+----------+-----------------+--------------------------------+",
        "",
        "[] means optional."
    };

    @Override
    public void execute(TransactionList transactions, Ui ui) {
        ui.showMessage(HELP_LINES);
    }
}
