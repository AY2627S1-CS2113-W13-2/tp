package finnus.command;

import finnus.transaction.TransactionList;
import finnus.ui.Ui;

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
