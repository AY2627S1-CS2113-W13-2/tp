package finnus.exception;

/**
 * Represents an error caused by invalid user input or an invalid application state.
 * The message is meant to be shown directly to the user.
 */
public class FinNusException extends Exception {
    public FinNusException(String message) {
        super(message);
    }
}
