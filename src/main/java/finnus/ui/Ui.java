package finnus.ui;

import java.util.List;
import java.util.Scanner;

/**
 * Handles all interaction with the user through the console:
 * reading commands and printing responses.
 */
public class Ui {
    private static final String DIVIDER = "____________________________________________________________";

    private final Scanner in = new Scanner(System.in);

    public void showWelcome() {
        showMessage("Welcome to FinNUS! Type a command to get started.");
    }

    /**
     * Reads the next command typed by the user, or returns {@code null} if the input has ended.
     */
    public String readCommand() {
        if (!in.hasNextLine()) {
            return null;
        }
        return in.nextLine();
    }

    /**
     * Prints one or more lines between two dividers.
     * 
     * @param lines A String[] of messages to show
     */
    public void showMessage(String... lines) {
        System.out.println(DIVIDER);
        for (String line : lines) {
            System.out.println(line);
        }
        System.out.println(DIVIDER);
    }

    /**
     * Overload showMessage to accept  a List as input parameter
     * 
     * @param lines A List of messages to show
     */
    public void showMessage(List<String> lines) {
        System.out.println(DIVIDER);
        for (String line: lines) {
            System.out.println(line);
        }
        System.out.println(DIVIDER);
    }

    public void showError(String message) {
        showMessage("Error: " + message);
    }
}
