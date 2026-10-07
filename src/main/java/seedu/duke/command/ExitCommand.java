package seedu.duke.command;

import seedu.duke.transaction.TransactionList;
import seedu.duke.ui.Ui;

/**
 * Ends the application.
 */
public class ExitCommand extends Command {
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        ui.showMessage("Goodbye! Spend wisely.");
    }

    @Override
    public boolean isExit() {
        return true;
    }
}
