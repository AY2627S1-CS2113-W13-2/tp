//@@author nkyjStar
package finnus.command;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Displays the user's balance rounded to two decimal places.
 */
public class BalanceCommand extends Command {
    private static final Logger logger = Logger.getLogger(BalanceCommand.class.getName());

    //@@author chancheese
    /** Used to count the days until the next allowance, if one is set. */
    private final LocalDate today;

    /** Creates the command for the real current date. */
    public BalanceCommand() {
        this(LocalDate.now());
    }

    /**
     * Creates the command.
     *
     * @param today The current date, passed in so tests do not depend on the real date.
     */
    public BalanceCommand(LocalDate today) {
        assert today != null : "Today's date must not be null";
        this.today = today;
    }
    //@@author

    /**
     * Prints the user's balance to 2 decimal places
     * Use half-up rounding (typical financial rounding strategy)
     *
     * @param transactions The list of transactions to get the balance from
     * @param ui The Ui instance
     */
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        logger.fine("Getting balance");

        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        //@@author chancheese
        List<String> lines = new ArrayList<>();
        lines.add("Your current balance is: " + transactions.getBalance().format());
        // Only shown once an allowance is set, so balance looks the same as before otherwise.
        transactions.getAllowance().ifPresent(allowance -> lines.add(AllowanceCommand.countdown(allowance, today)));
        ui.showMessage(lines);
        //@@author
    }
}
//@@author
