package seedu.duke.command;

import seedu.duke.exception.FinNusException;
import seedu.duke.transaction.TransactionList;
import seedu.duke.ui.Ui;

/**
 * Represents one user command that has already been parsed and is ready to run.
 * To add a new feature, create a subclass and return it from {@code Parser#parse}.
 */
public abstract class Command {
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
