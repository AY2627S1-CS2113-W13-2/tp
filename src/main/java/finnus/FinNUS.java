package finnus;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import finnus.command.Command;
import finnus.exception.FinNusException;
import finnus.parser.Parser;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Entry point of FinNUS: reads commands in a loop and executes them until the user exits.
 */
public class FinNUS {
    /** Log file created in the folder FinNUS is run from. */
    private static final String LOG_FILE = "finnus.log";

    private static final Logger logger = Logger.getLogger(FinNUS.class.getName());

    private final Ui ui = new Ui();
    private final TransactionList transactions = new TransactionList();

    /**
     * Main entry-point for the FinNUS application.
     */
    public static void main(String[] args) {
        setUpLogging();
        new FinNUS().run();
    }

    /**
     * Sends all log messages to {@value #LOG_FILE} instead of the console,
     * so they never mix with the output the user sees.
     * If the log file cannot be created, logging is turned off rather than stopping the app.
     */
    private static void setUpLogging() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        // By default Java prints logs to the console; remove that handler.
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        try {
            FileHandler fileHandler = new FileHandler(LOG_FILE, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.FINE);
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Level.FINE);
        } catch (IOException e) {
            rootLogger.setLevel(Level.OFF);
        }
    }

    /**
     * Runs the read-parse-execute loop until an exit command is given or input ends.
     */
    public void run() {
        logger.info("FinNUS started");
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
                logger.fine("Executing " + command.getClass().getSimpleName());
                command.execute(transactions, ui);
                isExit = command.isExit();
            } catch (FinNusException e) {
                logger.log(Level.WARNING, "Command failed for input '" + input + "': " + e.getMessage());
                ui.showError(e.getMessage());
            }
        }
        logger.info("FinNUS exited");
    }
}

