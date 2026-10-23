package finnus.command;

import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Ends the application.
 */
public class ExitCommand extends Command {
    /** Displays the exit message before the application stops. */
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        ui.showMessage("Goodbye! Spend wisely.");
    }

    /** Marks this command as the signal to stop the command loop. */
    @Override
    public boolean isExit() {
        return true;
    }
}
