package finnus.command;

import finnus.exception.FinNusException;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Represents one user command that has already been parsed and is ready to run.
 * To add a new feature, create a subclass and return it from {@code Parser#parse}.
 */
public abstract class Command {
    /**
     * Checks that a command has the transaction list and UI it needs to execute.
     *
     * @param transactions The transaction list passed to the command.
     * @param ui The UI passed to the command.
     */
    public static void assertExecutionContext(TransactionList transactions, Ui ui) {
        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
    }

    /**
     * Carries out the command.
     *
     * @param transactions The list of transactions the command may read or modify.
     * @param ui Used to show the result to the user.
     * @throws FinNusException If the command cannot be carried out.
     */
    public abstract void execute(TransactionList transactions, Ui ui) throws FinNusException;

    /**
     * Returns true if the application should stop after this command.
     */
    public boolean isExit() {
        return false;
    }
}
