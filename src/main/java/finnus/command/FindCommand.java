//@@author nabijonzzz
package finnus.command;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Lists the transactions whose description contains a keyword, e.g. {@code find lunch}, and/or
 * the withdrawals in one category, e.g. {@code find t/food}.
 *
 * <p>Each result is shown with the same index as in {@code recent}, so the user can pass it
 * straight to {@code delete} without having to look the transaction up again.
 */
public class FindCommand extends Command {
    private static final Logger logger = Logger.getLogger(FindCommand.class.getName());

    /** The text to look for in descriptions, or null to match any description. */
    private final String keyword;
    //@@author chancheese
    /** The category name as typed after {@code t/}, or null to match any category. */
    private final String categoryName;
    //@@author nabijonzzz

    /**
     * Creates a command that searches descriptions only.
     *
     * @param keyword The text to look for; must not be blank. Matching ignores case.
     */
    public FindCommand(String keyword) {
        this(keyword, null);
    }

    //@@author chancheese
    /**
     * Creates a command that searches by keyword, by category, or by both.
     *
     * @param keyword The text to look for in descriptions, or null to match any description.
     * @param categoryName The category typed after {@code t/}, or null to match any category.
     *     Whether it exists is checked when the command runs, since custom categories live in
     *     the transaction list.
     */
    public FindCommand(String keyword, String categoryName) {
        assert keyword == null || !keyword.isBlank() : "A keyword must not be blank";
        assert keyword != null || categoryName != null : "The parser must require a keyword or a category";
        this.keyword = keyword;
        this.categoryName = categoryName;
    }
    //@@author nabijonzzz

    /**
     * Shows the matching transactions, most recent first, without changing the list.
     *
     * @throws FinNusException If the category does not exist.
     */
    @Override
    public void execute(TransactionList transactions, Ui ui) throws FinNusException {
        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        //@@author chancheese
        // Resolved to its own spelling, e.g. "food" becomes "Food", as when recording a withdrawal.
        String category = categoryName == null ? null : transactions.getCategories().resolve(categoryName);
        //@@author nabijonzzz
        int sizeBefore = transactions.size();
        List<Integer> positions = findMatches(transactions, category);
        assert transactions.size() == sizeBefore : "Searching must not change the transactions";
        //@@author chancheese
        logger.fine("Found " + positions.size() + " transaction(s) for a search");

        if (positions.isEmpty()) {
            ui.showMessage("No transactions " + describeCriteria("match", category) + ".");
            return;
        }
        int size = transactions.size();
        List<String> lines = new ArrayList<>();
        lines.add("Found " + positions.size() + " transaction(s) " + describeCriteria("matching", category) + ":");
        for (int position : positions) {
            lines.add((size - position) + ". " + transactions.get(position));
        }
        if (category != null) {
            // Only withdrawals have categories, so this is money spent, in SGD.
            lines.add("Total spent: " + totalAmount(transactions, positions).format());
        }
        //@@author nabijonzzz
        lines.add("Use the number with " + CommandWord.DELETE.getWord() + " to remove a transaction.");
        ui.showMessage(lines);
    }

    //@@author chancheese
    /**
     * Returns the positions (oldest first, starting from 0) of the matching transactions, from
     * the most recent to the oldest. A transaction at position {@code i} has index
     * {@code size - i}, which is the same index that {@code recent} shows and {@code delete} expects.
     */
    private List<Integer> findMatches(TransactionList transactions, String category) {
        List<Integer> positions = new ArrayList<>();
        for (int position = transactions.size() - 1; position >= 0; position--) {
            if (matches(transactions.get(position), category)) {
                positions.add(position);
            }
        }
        return positions;
    }

    /**
     * Returns whether a transaction meets every criterion given. Deposits have no category,
     * so they never match a category search.
     *
     * @param category The resolved category, or null to match any category.
     */
    private boolean matches(Transaction transaction, String category) {
        boolean categoryMatches = category == null
                || transaction.getCategory().map(category::equals).orElse(false);
        boolean keywordMatches = keyword == null || containsIgnoreCase(transaction.getDescription(), keyword);
        return categoryMatches && keywordMatches;
    }

    /**
     * Describes the search for the output, e.g. {@code matching "lunch" in #Food}, or
     * {@code in #Food} without a keyword. The category is shown with {@code #}, as in the list.
     *
     * @param verb The word before the keyword, e.g. {@code matching}.
     * @param category The resolved category, or null if none was given.
     */
    private String describeCriteria(String verb, String category) {
        List<String> parts = new ArrayList<>();
        if (keyword != null) {
            parts.add(verb + " \"" + keyword + "\"");
        }
        if (category != null) {
            parts.add("in #" + category);
        }
        return String.join(" ", parts);
    }

    /** Adds up the SGD amounts of the transactions at the given positions. */
    private static Money totalAmount(TransactionList transactions, List<Integer> positions) {
        Money total = Money.ZERO;
        for (int position : positions) {
            total = total.add(transactions.get(position).getAmount());
        }
        return total;
    }
    //@@author nabijonzzz

    /**
     * Returns whether {@code text} contains {@code keyword}, ignoring case.
     *
     * <p>This compares one character at a time with {@code regionMatches} instead of lowercasing
     * both strings, because lowercasing can change the length of some text. For example, the Turkish
     * capital {@code İ} lowercases to two characters ({@code i} plus a dot), so
     * {@code "İstanbul".toLowerCase().contains("istanbul")} is false.
     */
    static boolean containsIgnoreCase(String text, String keyword) {
        for (int start = 0; start + keyword.length() <= text.length(); start++) {
            if (text.regionMatches(true, start, keyword, 0, keyword.length())) {
                return true;
            }
        }
        return false;
    }
}
