package finnus;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import finnus.command.Command;
import finnus.command.CreditAllowanceCommand;
import finnus.exception.FinNusException;
import finnus.parser.Parser;
import finnus.storage.FileStorage;
import finnus.storage.LedgerSnapshot;
import finnus.storage.LoadResult;
import finnus.storage.Storage;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Entry point of FinNUS: reads commands in a loop and executes them until the user exits.
 */
public class FinNUS {
    /** Log file created in the folder FinNUS is run from. */
    private static final String LOG_FILE = "finnus.log";

    private static final Logger logger = Logger.getLogger(FinNUS.class.getName());

    private final Ui ui;
    private final Storage storage;
    //@@author chancheese
    /** Gives today's date; a fixed clock lets tests choose the day. */
    private final Clock clock;
    //@@author
    /** The mutable in-memory ledger restored at startup and updated by commands. */
    private TransactionList transactions;
    /** Tracks whether the current ledger differs from its last successful save. */
    private boolean hasUnsavedChanges;
    /** Prevents writes when loading could not establish that replacing storage is safe. */
    private boolean savingAllowed;

    /**
     * Creates the console application with persistent storage beside the packaged JAR.
     */
    public FinNUS() {
        this(new Ui(), new FileStorage(FileStorage.defaultPath()));
    }

    /**
     * Supplies independent UI and storage implementations for launchers and isolated tests.
     */
    public FinNUS(Ui ui, Storage storage) {
        this(ui, storage, Clock.systemDefaultZone());
    }

    //@@author chancheese
    /**
     * Also supplies the clock, so tests can run on a fixed date.
     */
    public FinNUS(Ui ui, Storage storage, Clock clock) {
        this.ui = Objects.requireNonNull(ui);
        this.storage = Objects.requireNonNull(storage);
        this.clock = Objects.requireNonNull(clock);
    }

    /** Returns today's date according to the clock. */
    private LocalDate today() {
        return LocalDate.now(clock);
    }
    //@@author

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
        if (!restoreSession()) {
            logger.info("FinNUS stopped during setup");
            return;
        }
        boolean isExit = false;
        while (!isExit) {
            String input = ui.readCommand();
            if (input == null) {
                logger.fine("Input ended; leaving command loop");
                break;
            }
            if (input.isBlank()) {
                continue;
            }
            try {
                isExit = executeCommand(input);
            } catch (FinNusException e) {
                logger.log(Level.WARNING, "Command failed: " + e.getMessage());
                ui.showError(e.getMessage());
            }
        }
        savePendingChanges();
        if (hasUnsavedChanges) {
            ui.showError("Unsaved changes were not written to disk and will be lost after exit.");
        }
        logger.info("FinNUS exited");
    }

    /** Loads the saved ledger, initializes session flags, and shows startup diagnostics. */
    private boolean restoreSession() {
        LoadResult loaded = storage.load();
        transactions = loaded.snapshot().toTransactionList();
        savingAllowed = loaded.savingAllowed();
        hasUnsavedChanges = false;
        assert transactions != null : "Restored transaction list must exist";
        logger.fine("Session restored; saving allowed: " + savingAllowed
                + ", warnings: " + loaded.warnings().size());
        if (!loaded.warnings().isEmpty()) {
            ui.showMessage(loaded.warnings());
        }
        if (loaded.setupRequired()) {
            if (!new Setup().run(ui, transactions, today())) {
                return false;
            }
            hasUnsavedChanges = true;
            savePendingChanges();
        }
        ui.showWelcome();
        //@@author chancheese
        // Adds any allowance that arrived while FinNUS was closed, and warns if one cannot be added yet.
        creditAllowance(true);
        savePendingChanges();
        //@@author
        return true;
    }

    //@@author chancheese
    /**
     * Adds any allowance deposits that are due, and marks the ledger as changed if any were added.
     *
     * @param showsProblems Whether to warn about a due allowance that cannot be added yet.
     */
    private void creditAllowance(boolean showsProblems) {
        LedgerSnapshot before = LedgerSnapshot.capture(transactions);
        new CreditAllowanceCommand(today(), showsProblems).execute(transactions, ui);
        hasUnsavedChanges |= !before.equals(LedgerSnapshot.capture(transactions));
    }
    //@@author

    /**
     * Detects actual changes without requiring every command to declare whether it writes data.
     */
    private boolean executeCommand(String input) throws FinNusException {
        Command command = Parser.parse(input, today());
        assert command != null : "Parser must return a command";
        LedgerSnapshot before = LedgerSnapshot.capture(transactions);
        logger.fine("Executing " + command.getClass().getSimpleName());
        command.execute(transactions, ui);
        //@@author chancheese
        // A new rate or a date change during the session can make a waiting allowance addable.
        if (!command.isExit()) {
            new CreditAllowanceCommand(today(), false).execute(transactions, ui);
        }
        //@@author
        boolean changed = !before.equals(LedgerSnapshot.capture(transactions));
        hasUnsavedChanges |= changed;
        if (changed) {
            logger.fine("Command changed ledger state");
        }
        if (!command.isExit()) {
            savePendingChanges();
        }
        return command.isExit();
    }

    /**
     * Keeps failed saves dirty so a later successful command or shutdown can retry them.
     */
    private void savePendingChanges() {
        if (!hasUnsavedChanges) {
            return;
        }
        if (!savingAllowed) {
            ui.showError("Changes are in memory only. Saving is disabled for this session to protect existing data.");
            return;
        }
        try {
            assert transactions != null : "Transactions must be initialized before saving";
            storage.save(LedgerSnapshot.capture(transactions));
            hasUnsavedChanges = false;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Automatic save failed", e);
            ui.showError("Automatic save failed. Changes remain in memory and will be retried after a later "
                    + "successful command and before exit.");
        }
    }
}

