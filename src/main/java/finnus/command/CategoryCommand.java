//@@author nabijonzzz
package finnus.command;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import finnus.category.CategoryList;
import finnus.exception.FinNusException;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Lists all spending categories with {@code category}, or adds a custom category,
 * e.g. {@code category Gym}.
 */
public class CategoryCommand extends Command {
    private static final Logger logger = Logger.getLogger(CategoryCommand.class.getName());

    /** The category to add, or null to list all categories instead. */
    private final String nameToAdd;

    /**
     * Creates a command that lists all categories.
     */
    public CategoryCommand() {
        this.nameToAdd = null;
    }

    /**
     * Creates a command that adds a custom category.
     *
     * @param nameToAdd A name with a valid format; whether it already exists is checked when the command runs.
     */
    public CategoryCommand(String nameToAdd) {
        assert CategoryList.isValidName(nameToAdd) : "Category name must be validated by the parser";
        this.nameToAdd = nameToAdd;
    }

    /**
     * Lists the categories or adds the new one.
     *
     * @throws FinNusException If the category to add already exists, ignoring case.
     */
    @Override
    public void execute(TransactionList transactions, Ui ui) throws FinNusException {
        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        CategoryList categories = transactions.getCategories();
        if (nameToAdd == null) {
            listCategories(categories, ui);
            return;
        }
        // Comparing ignoring case also stops a custom category shadowing a predefined one, e.g. FOOD.
        String existing = categories.find(nameToAdd).orElse(null);
        if (existing != null) {
            throw new FinNusException("Category '" + existing + "' already exists.");
        }
        categories.add(nameToAdd);
        logger.info("Added custom category " + nameToAdd);
        ui.showMessage("Category added: #" + nameToAdd,
                "Use it with, e.g.: withdraw 4.50 d/Lunch t/" + nameToAdd);
    }

    /** Shows the predefined categories, then the custom ones. */
    private static void listCategories(CategoryList categories, Ui ui) {
        List<String> custom = categories.getCustom();
        List<String> lines = new ArrayList<>();
        lines.add("Categories:");
        lines.add("  Predefined: " + String.join(", ", CategoryList.PREDEFINED));
        lines.add(custom.isEmpty()
                ? "  Custom: none yet. Add one with: category NAME"
                : "  Custom: " + String.join(", ", custom));
        ui.showMessage(lines);
    }
}
