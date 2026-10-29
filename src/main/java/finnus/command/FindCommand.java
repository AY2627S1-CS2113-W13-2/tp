//@@author nabijonzzz
package finnus.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Lists the transactions whose description contains a keyword, e.g. {@code find lunch}.
 *
 * <p>Each result is shown with the same index as in {@code recent}, so the user can pass it
 * straight to {@code delete} without having to look the transaction up again.
 */
public class FindCommand extends Command {
    private static final Logger logger = Logger.getLogger(FindCommand.class.getName());

    private final String keyword;

    /**
     * Creates the command.
     *
     * @param keyword The text to look for; must not be blank. Matching ignores case.
     */
    public FindCommand(String keyword) {
        assert keyword != null && !keyword.isBlank() : "Keyword must be validated by the parser";
        this.keyword = keyword;
    }

    /** Shows the matching transactions, most recent first, without changing the list. */
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        int sizeBefore = transactions.size();
        List<String> matches = findMatches(transactions);
        assert transactions.size() == sizeBefore : "Searching must not change the transactions";
        logger.fine("Found " + matches.size() + " transaction(s) for a keyword search");

        if (matches.isEmpty()) {
            ui.showMessage("No transactions match \"" + keyword + "\".");
            return;
        }
        List<String> lines = new ArrayList<>();
        lines.add("Found " + matches.size() + " transaction(s) matching \"" + keyword + "\":");
        lines.addAll(matches);
        lines.add("Use the number with " + CommandWord.DELETE.getWord() + " to remove a transaction.");
        ui.showMessage(lines);
    }

    /**
     * Returns the matching transactions as numbered lines, from the most recent to the oldest.
     * A transaction at position {@code i} (oldest first) has index {@code size - i}, which is
     * the same index that {@code recent} shows and {@code delete} expects.
     */
    private List<String> findMatches(TransactionList transactions) {
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        int size = transactions.size();
        for (int position = size - 1; position >= 0; position--) {
            Transaction transaction = transactions.get(position);
            if (transaction.getDescription().toLowerCase(Locale.ROOT).contains(lowerKeyword)) {
                matches.add((size - position) + ". " + transaction);
            }
        }
        return matches;
    }
}
