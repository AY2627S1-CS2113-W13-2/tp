package finnus;

/**
 * Regular expressions shared by input and stored-data validation.
 */
public final class ValidationRegex {
    /** One or two digits for an allowance day; the valid calendar range is checked separately. */
    public static final String ALLOWANCE_DAY_REGEX = "\\d{1,2}";

    /** One or more ASCII digits; positive range checks are performed separately. */
    public static final String UNSIGNED_INTEGER_REGEX = "[0-9]+";

    /** One to three digits for a whole percentage; the 1 to 100 range is checked separately. */
    public static final String PERCENTAGE_REGEX = "\\d{1,3}";

    private ValidationRegex() {
    }
}
