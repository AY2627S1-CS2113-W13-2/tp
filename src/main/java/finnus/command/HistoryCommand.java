//@@author nkyjStar
package finnus.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import finnus.exception.FinNusException;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Displays a paginated, newest-first transaction history and holds its viewing position.
 * The application keeps this command only while the user is in history mode.
 */
public class HistoryCommand extends Command {
    public static final int DEFAULT_PAGE_SIZE = 5;

    private static final Logger logger = Logger.getLogger(HistoryCommand.class.getName());

    private int pageSize;
    /** Zero-based position in the newest-first list of the first visible transaction. */
    private int firstIndex;

    /**
     * Creates a history view at the requested page using the requested page size.
     *
     * @param pageSize The positive number of transactions to show per page.
     * @param page The positive, one-based page number to show first.
     * @throws IllegalArgumentException If either number is not positive.
     */
    public HistoryCommand(int pageSize, int page) {
        if (pageSize <= 0 || page <= 0) {
            throw new IllegalArgumentException("Page size and page must be positive");
        }
        this.pageSize = pageSize;
        long requestedFirst = ((long) page - 1) * pageSize;
        // A position beyond the int range cannot be a valid position in TransactionList.
        this.firstIndex = requestedFirst > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) requestedFirst;
    }

    /**
     * Shows the initial page, rejecting a page beyond the available transactions.
     *
     * @param transactions The ledger to read without modifying it.
     * @param ui Displays the page or empty-list message.
     * @throws FinNusException If the requested page is beyond the available transactions.
     */
    @Override
    public void execute(TransactionList transactions, Ui ui) throws FinNusException {
        assert transactions != null: "Transaction list must not be null";
        assert ui != null: "Ui must not be null";
        if (firstIndex >= transactions.size() && (firstIndex != 0 || transactions.size() != 0)) {
            throw new FinNusException("History page is out of range.");
        }
        showPage(transactions, ui);
        logger.fine("Opened history at transaction index " + firstIndex + " with page size " + pageSize);
    }

    /**
     * Handles one input while history mode is active.
     *
     * @param input The navigation input: {@code next}, {@code prev}, {@code p/PAGE},
     *     {@code n/NUMBER}, or {@code stop}.
     * @param transactions The ledger used to determine page boundaries and display entries.
     * @param ui Displays the resulting page or stop message.
     * @return Whether the user chose {@code stop} and should leave history mode.
     * @throws FinNusException If the input is invalid or requests a page outside the ledger.
     */
    public boolean handleInput(String input, TransactionList transactions, Ui ui) throws FinNusException {
        // Preliminary error checks
        assert input != null: "Input must not be null";
        assert transactions != null: "Transaction list must not be null";
        assert ui != null: "Ui must not be null";

        // Parse input
        String trimmed = input.trim().toLowerCase(Locale.ROOT);
        if (trimmed.equals("stop")) {
            ui.showMessage("Stopped viewing transaction history.");
            logger.fine("Stopped viewing history");
            return true;
        }
        if (trimmed.equals("next")) {
            long nextIndex = (long) firstIndex + pageSize;            
            if (nextIndex >= transactions.size()) {
                throw new FinNusException("You are already on the last history page.");
            }

            firstIndex = (int) nextIndex;
            assert firstIndex >= 0: "The index of the first item must be non-negative";
            logger.fine("Moved to next history page at transaction index " + firstIndex);
        } else if (trimmed.equals("prev")) {
            if (firstIndex == 0) {
                throw new FinNusException("You are already on the first history page.");
            }
            firstIndex = Math.max(0, firstIndex - pageSize);
            logger.fine("Moved to previous history page at transaction index " + firstIndex);
        } else if (trimmed.startsWith("p/")) {
            int page = parsePositiveNumber(trimmed.substring(2), "page");
            long requestedFirst = ((long) page - 1) * pageSize;
            if (requestedFirst >= transactions.size() && (requestedFirst != 0 || transactions.size() != 0)) {
                throw new FinNusException("History page is out of range.");
            }
            firstIndex = (int) requestedFirst;
            logger.fine("Jumped to history page " + page + " at transaction index " + firstIndex);
        } else if (trimmed.startsWith("n/")) {
            // Keeping firstIndex preserves the first transaction currently on screen.
            int newPageSize = parsePositiveNumber(trimmed.substring(2), "page size");
            assert newPageSize > 0: "Page size should be positive";

            logger.fine("Changed history page size from " + pageSize + " to " + newPageSize);
            pageSize = newPageSize;
        } else {
            throw new FinNusException("While viewing history, use next, prev, p/PAGE, n/NUMBER, or stop.");
        }
        showPage(transactions, ui);
        return false;
    }

    /**
     * Parses a positive decimal integer without signs or extra text.
     *
     * @param value The text following {@code n/} or {@code p/}.
     * @param name A label for the number, used in validation errors.
     * @return The parsed positive integer.
     * @throws FinNusException If the text is missing, non-numeric, zero, or too large for an int.
     */
    public static int parsePositiveNumber(String value, String name) throws FinNusException {
        assert !name.isEmpty(): "There should be a label";
        assert !value.isEmpty(): "There should a parsed value from the user";

        if (!value.matches("[0-9]+")) {
            throw new FinNusException("The " + name + " must be a positive whole number.");
        }
        try {
            int number = Integer.parseInt(value);
            if (number > 0) {
                return number;
            }
        } catch (NumberFormatException e) {
            // Values larger than int can represent are rejected below.
        }
        throw new FinNusException("The " + name + " must be a positive whole number.");
    }

    /**
     * Renders one page with global transaction numbers shared with recent and delete.
     *
     * @param transactions The ledger to read in newest-first order.
     * @param ui Displays the page or empty-list message.
     */
    private void showPage(TransactionList transactions, Ui ui) {
        // Preliminary checks
        assert transactions != null: "Transaction list must not be null";
        assert ui != null: "Ui must not be null";

        int total = transactions.size();
        assert total >= 0: "Size should be non-negative";
        if (total == 0) {
            ui.showMessage("Your list of transactions is currently empty.",
                    "History controls: next, prev, p/PAGE, n/NUMBER, stop.");
            return;
        }

        int end = (int) Math.min((long) total, (long) firstIndex + pageSize);
        assert end >= 0 && end <= total: "Calculated end is out of bounds";

        List<String> lines = new ArrayList<>();
        lines.add("Transaction history: " + (firstIndex + 1) + "-" + end + " of " + total
                + " (" + pageSize + " per page):");
        for (int index = firstIndex; index < end; index++) {
            lines.add((index + 1) + ". " + transactions.get(total - 1 - index));
        }
        lines.add("History controls: next, prev, p/PAGE, n/NUMBER, stop.");
        assert !lines.isEmpty(): "Message to show should not be empty";

        ui.showMessage(lines);
    }
}
