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
        "  deposit AMOUNT [d/DESCRIPTION] — Record money received.",
        "  withdraw AMOUNT d/DESCRIPTION — Record money spent.",
        "  help — Show available commands.",
        "  exit — Close FinNUS.",
        "",
        "[] means optional."
    };

    @Override
    public void execute(TransactionList transactions, Ui ui) {
        ui.showMessage(HELP_LINES);
    }
}
