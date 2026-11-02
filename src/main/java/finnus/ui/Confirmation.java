package finnus.ui;

import java.util.Locale;
import java.util.logging.Logger;

//@@author lololol8484
/** Reads a reusable yes/no confirmation without deciding what either answer means. */
public final class Confirmation {
    private static final Logger logger = Logger.getLogger(Confirmation.class.getName());

    /** The answer to a confirmation prompt, including end of input. */
    public enum Result {
        YES,
        NO,
        INPUT_ENDED
    }

    private Confirmation() {
    }

    /** Repeats the prompt for invalid answers and returns when the user answers yes, no, or EOF. */
    public static Result ask(Ui ui, String prompt) {
        assert ui != null : "UI must not be null";
        assert prompt != null && !prompt.isBlank() : "Confirmation prompt must not be blank";
        while (true) {
            String answer = ui.readCommand(prompt);
            if (answer == null) {
                logger.fine("Confirmation ended because input was exhausted");
                return Result.INPUT_ENDED;
            }
            switch (answer.trim().toLowerCase(Locale.ROOT)) {
            case "yes", "y" -> {
                logger.fine("User confirmed an operation");
                return Result.YES;
            }
            case "no", "n" -> {
                logger.fine("User declined an operation");
                return Result.NO;
            }
            default -> ui.showError("Please answer yes or no.");
            }
        }
    }
}
//@@author
