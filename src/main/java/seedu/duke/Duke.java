package seedu.duke;

import seedu.duke.command.Command;
import seedu.duke.exception.FinNusException;
import seedu.duke.parser.Parser;
import seedu.duke.transaction.TransactionList;
import seedu.duke.ui.Ui;

/**
 * Entry point of FinNUS: reads commands in a loop and executes them until the user exits.
 */
public class Duke {
    private final Ui ui = new Ui();
    private final TransactionList transactions = new TransactionList();

    /**
     * Main entry-point for the FinNUS application.
     */
    public static void main(String[] args) {
        new Duke().run();
    }

    /**
     * Runs the read-parse-execute loop until an exit command is given or input ends.
     */
    public void run() {
        ui.showWelcome();
        boolean isExit = false;
        while (!isExit) {
            String input = ui.readCommand();
            if (input == null) {
                break;
            }
            if (input.isBlank()) {
                continue;
            }
            try {
                Command command = Parser.parse(input);
                command.execute(transactions, ui);
                isExit = command.isExit();
            } catch (FinNusException e) {
                ui.showError(e.getMessage());
            }
        }
    }
}
