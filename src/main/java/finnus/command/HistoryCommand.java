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

    /** Creates a history view at the requested page using the requested page size. */
    public HistoryCommand(int pageSize, int page) {
        if (pageSize <= 0 || page <= 0) {
            throw new IllegalArgumentException("Page size and page must be positive");
        }
        this.pageSize = pageSize;
        long requestedFirst = ((long) page - 1) * pageSize;
        // A position beyond the int range cannot be a valid position in TransactionList.
        this.firstIndex = requestedFirst > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) requestedFirst;
    }

    /** Shows the initial page, rejecting a page beyond the available transactions. */
    @Override
    public void execute(TransactionList transactions, Ui ui) throws FinNusException {
        if (firstIndex >= transactions.size() && (firstIndex != 0 || transactions.size() != 0)) {
            throw new FinNusException("History page is out of range.");
        }
        showPage(transactions, ui);
        logger.fine("Opened history at transaction index " + firstIndex + " with page size " + pageSize);
    }

    /**
     * Handles one input while history mode is active.
     *
     * @return Whether the user chose {@code stop} and should leave history mode.
     */
    public boolean handleInput(String input, TransactionList transactions, Ui ui) throws FinNusException {
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
            logger.fine("Changed history page size from " + pageSize + " to " + newPageSize);
            pageSize = newPageSize;
        } else {
            throw new FinNusException("While viewing history, use next, prev, p/PAGE, n/NUMBER, or stop.");
        }
        showPage(transactions, ui);
        return false;
    }

    /** Parses a positive decimal integer without signs or extra text. */
    public static int parsePositiveNumber(String value, String name) throws FinNusException {
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

    /** Renders one page with global transaction numbers shared with recent and delete. */
    private void showPage(TransactionList transactions, Ui ui) {
        int total = transactions.size();
        if (total == 0) {
            ui.showMessage("Your list of transactions is currently empty.",
                    "History controls: next, prev, p/PAGE, n/NUMBER, stop.");
            return;
        }
        int end = (int) Math.min((long) total, (long) firstIndex + pageSize);
        List<String> lines = new ArrayList<>();
        lines.add("Transaction history: " + (firstIndex + 1) + "-" + end + " of " + total
                + " (" + pageSize + " per page):");
        for (int index = firstIndex; index < end; index++) {
            lines.add((index + 1) + ". " + transactions.get(total - 1 - index));
        }
        lines.add("History controls: next, prev, p/PAGE, n/NUMBER, stop.");
        ui.showMessage(lines);
    }
}
