//@@author nabijonzzz
package finnus.category;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import finnus.exception.FinNusException;

/**
 * Stores the spending categories a withdrawal can be tagged with: a fixed set of
 * predefined categories plus any custom categories the user adds.
 *
 * <p>Names are matched ignoring case, but are always shown in the spelling they were
 * created with, e.g. {@code t/FOOD} is stored and shown as {@code Food}.
 */
public class CategoryList {
    /** The category used when a withdrawal is recorded without {@code t/}. */
    public static final String DEFAULT_CATEGORY = "Others";

    /** Categories that always exist, in the order they are listed. */
    public static final List<String> PREDEFINED = List.of("Food", "Transport", "Housing", "Bills",
            "Shopping", "Entertainment", "Education", "Health", DEFAULT_CATEGORY);

    /** Longest allowed category name, so names stay short enough to show on one line. */
    public static final int MAX_NAME_LENGTH = 20;

    /**
     * Letters, digits and '-' only. This keeps out spaces (a name is one word, so {@code t/}
     * never swallows the description), '/' (which marks a prefix) and '|' (the storage separator).
     */
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9-]{1," + MAX_NAME_LENGTH + "}");

    /** Custom categories in the order they were added. */
    private final List<String> customCategories = new ArrayList<>();

    /**
     * Returns whether {@code name} has an allowed format, regardless of whether it exists.
     */
    public static boolean isValidName(String name) {
        return name != null && VALID_NAME.matcher(name).matches();
    }

    /**
     * Finds a predefined or custom category, ignoring case.
     *
     * @return The category in the spelling it was created with, or empty if it does not exist.
     */
    public Optional<String> find(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String lowerName = name.toLowerCase(Locale.ROOT);
        return getAll().stream()
                .filter(category -> category.toLowerCase(Locale.ROOT).equals(lowerName))
                .findFirst();
    }

    /** Returns whether a predefined or custom category with this name exists, ignoring case. */
    public boolean contains(String name) {
        return find(name).isPresent();
    }

    /**
     * Adds a custom category.
     *
     * @param name A valid name that does not exist yet, ignoring case.
     */
    public void add(String name) {
        assert isValidName(name) : "Category name must be validated before it is added";
        assert !contains(name) : "Duplicate categories must be rejected before they are added";
        customCategories.add(name);
    }

    /**
     * Returns the category a withdrawal should be recorded under.
     *
     * @param name The name typed after {@code t/}, or null if no category was given.
     * @return The default category for null, otherwise the matching category's own spelling.
     * @throws FinNusException If no category with this name exists.
     */
    public String resolve(String name) throws FinNusException {
        if (name == null) {
            return DEFAULT_CATEGORY;
        }
        return find(name).orElseThrow(() -> new FinNusException("Unknown category '" + name
                + "'. Valid categories: " + String.join(", ", getAll())
                + "\nAdd a new one with: category NAME"));
    }

    /** Returns the custom categories in the order they were added. */
    public List<String> getCustom() {
        return List.copyOf(customCategories);
    }

    /** Returns all categories: the predefined ones first, then the custom ones. */
    public List<String> getAll() {
        List<String> all = new ArrayList<>(PREDEFINED);
        all.addAll(customCategories);
        return List.copyOf(all);
    }
}
