package finnus;

import finnus.command.Command;
import finnus.exception.FinNusException;
import finnus.parser.Parser;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Entry point of FinNUS: reads commands in a loop and executes them until the user exits.
 */
public class FinNUS {
    private final Ui ui = new Ui();
    private final TransactionList transactions = new TransactionList();

    /**
     * Main entry-point for the FinNUS application.
     */
    public static void main(String[] args) {
        new FinNUS().run();
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

