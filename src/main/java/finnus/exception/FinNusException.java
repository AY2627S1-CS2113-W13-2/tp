package finnus.exception;

/**
 * Represents an error caused by invalid user input or an invalid application state.
 * The message is meant to be shown directly to the user.
 */
public class FinNusException extends Exception {
    /**
     * Creates an application error with a message safe to show to the user.
     *
     * @param message The user-facing explanation of the error.
     */
    public FinNusException(String message) {
        super(message);
    }
}
