package finnus;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import finnus.command.Command;
import finnus.command.CreditAllowanceCommand;
import finnus.command.DeleteCommand;
import finnus.command.EditCommand;
import finnus.command.HistoryCommand;
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

    //@@author naieel04
    private final Ui ui;
    private final Storage storage;
    //@@author
    //@@author chancheese
    /** Gives today's date; a fixed clock lets tests choose the day. */
    private final Clock clock;
    //@@author
    /** The mutable in-memory ledger restored at startup and updated by commands. */
    //@@author naieel04
    private TransactionList transactions;
    //@@author
    /** Tracks whether the current ledger differs from its last successful save. */
    //@@author naieel04
    private boolean hasUnsavedChanges;
    //@@author
    /** Prevents writes when loading could not establish that replacing storage is safe. */
    //@@author naieel04
    private boolean savingAllowed;
    //@@author
    //@@author nkyjStar
    /** Active paginated history view, or null when normal commands are available. */
    private HistoryCommand historyView;
    //@@author

    //@@author naieel04
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
        //@@author
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
                //@@author naieel04
                isExit = executeCommand(input);
            //@@author
            } catch (FinNusException e) {
                logger.log(Level.WARNING, "Command failed: " + e.getMessage());
                ui.showError(e.getMessage());
            }
        }
        //@@author naieel04
        savePendingChanges();
        if (hasUnsavedChanges) {
            ui.showError("Unsaved changes were not written to disk and will be lost after exit.");
            //@@author
        }
        logger.info("FinNUS exited");
    }

    /** Loads the saved ledger, initializes session flags, and shows startup diagnostics. */
    private boolean restoreSession() {
        //@@author naieel04
        LoadResult loaded = storage.load();
        transactions = loaded.snapshot().toTransactionList();
        //@@author
        //@@author nkyjStar
        historyView = null;
        //@@author
        //@@author naieel04
        savingAllowed = loaded.savingAllowed();
        hasUnsavedChanges = false;
        //@@author
        assert transactions != null : "Restored transaction list must exist";
        logger.fine("Session restored; saving allowed: " + savingAllowed
                + ", warnings: " + loaded.warnings().size());
        //@@author naieel04
        if (!loaded.warnings().isEmpty()) {
            ui.showMessage(loaded.warnings());
            //@@author
        }
        if (loaded.setupRequired()) {
            //@@author naieel04
            // The callback records the checkpoint so optional steps only trigger a save when state changes.
            AtomicReference<LedgerSnapshot> setupCheckpoint = new AtomicReference<>();
            if (!new Setup().run(ui, transactions, today(), () -> {
                setupCheckpoint.set(LedgerSnapshot.capture(transactions));
                //@@author
                hasUnsavedChanges = true;
                savePendingChanges();
                //@@author naieel04
            })) {
                return false;
            }
            hasUnsavedChanges |= !setupCheckpoint.get().equals(LedgerSnapshot.capture(transactions));
            savePendingChanges();
            //@@author
        }
        ui.showWelcome();
        //@@author chancheese
        // Adds any allowance that arrived while FinNUS was closed, and warns if one cannot be added yet.
        creditAllowance(today(), true);
        savePendingChanges();
        //@@author
        return true;
    }

    //@@author chancheese
    /**
     * Adds any allowance deposits that are due, and marks the ledger as changed if any were added.
     *
     * @param date The date captured for startup or the current command.
     * @param showsProblems Whether to warn about a due allowance that cannot be added yet.
     */
    private void creditAllowance(LocalDate date, boolean showsProblems) {
        LedgerSnapshot before = LedgerSnapshot.capture(transactions);
        new CreditAllowanceCommand(date, showsProblems).execute(transactions, ui);
        hasUnsavedChanges |= !before.equals(LedgerSnapshot.capture(transactions));
    }
    //@@author

    /**
     * Runs a normal command or handles navigation while history mode is active.
     * Detects ledger changes without requiring each command to declare whether it writes data.
     *
     * @param input The non-blank line entered by the user.
     * @return Whether an exit command ended the application session.
     * @throws FinNusException If parsing, history navigation, or command execution fails.
     */
    //@@author naieel04
    private boolean executeCommand(String input) throws FinNusException {
        //@@author
        //@@author nkyjStar
        if (historyView != null) {
            boolean stopped = historyView.handleInput(input, transactions, ui);
            if (stopped) {
                historyView = null;
            }
            // History navigation is read-only, but a previously failed save may still need retrying.
            savePendingChanges();
            return false;
        }
        //@@author
        //@@author naieel04
        LocalDate commandDate = today();
        Command command = Parser.parse(input, commandDate);
        //@@author
        assert command != null : "Parser must return a command";
        //@@author naieel04
        prepareAllowance(command, commandDate);
        LedgerSnapshot before = LedgerSnapshot.capture(transactions);
        //@@author
        logger.fine("Executing " + command.getClass().getSimpleName());
        command.execute(transactions, ui);
        if (command instanceof HistoryCommand historyCommand) {
            historyView = historyCommand;
        //@@author naieel04
        } else if (!command.isExit()) {
            //@@author
            //@@author chancheese
            // A new rate can release a waiting allowance; indexed commands consume their displayed index first.
            creditAllowance(commandDate, false);
            //@@author
        }
        boolean changed = !before.equals(LedgerSnapshot.capture(transactions));
        hasUnsavedChanges |= changed;
        if (changed) {
            logger.fine("Command changed ledger state");
        }
        //@@author naieel04
        if (!command.isExit()) {
            savePendingChanges();
        }
        return command.isExit();
    }

    /**
     * Settles due allowances before commands display results, without shifting indices used by edit or delete.
     * Saving here preserves credited deposits even when the following command fails.
     */
    private void prepareAllowance(Command command, LocalDate date) {
        if (command.isExit() || command instanceof DeleteCommand || command instanceof EditCommand) {
            return;
        }
        creditAllowance(date, false);
        savePendingChanges();
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
            //@@author
            assert transactions != null : "Transactions must be initialized before saving";
            //@@author naieel04
            storage.save(LedgerSnapshot.capture(transactions));
            hasUnsavedChanges = false;
        } catch (IOException e) {
            //@@author
            logger.log(Level.WARNING, "Automatic save failed", e);
            //@@author naieel04
            ui.showError("Automatic save failed. Changes remain in memory and will be retried after a later "
                    + "successful command and before exit.");
            //@@author
        }
    }
}

