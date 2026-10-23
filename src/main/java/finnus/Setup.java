package finnus;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.logging.Logger;

import finnus.command.OpeningBalanceCommand;
import finnus.exception.FinNusException;
import finnus.parser.Parser;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/** Guides a first-time user through choosing and confirming the opening balance. */
public class Setup {
    private static final Logger LOGGER = Logger.getLogger(Setup.class.getName());

    private enum Confirmation {
        CONFIRMED, REENTER, INPUT_ENDED
    }

    /**
     * Prompts for an opening balance until the user confirms it or input ends.
     *
     * @param ui The console interface used for prompts and responses.
     * @param transactions The ledger whose opening balance is being initialized.
     * @return {@code true} if the user confirmed a balance; {@code false} if input ended first.
     */
    public boolean run(Ui ui, TransactionList transactions) {
        assert ui != null : "UI must not be null";
        assert transactions != null : "Transaction list must not be null";
        assert transactions.size() == 0 : "First-run setup must not start with transactions";
        ui.showMessage("First-time setup", "Enter your current opening balance in SGD.",
                "This is your starting balance and will not be recorded as a transaction.");
        while (true) {
            String input = ui.readCommand("Opening balance (SGD): ");
            if (input == null) {
                LOGGER.fine("First-run setup ended before an opening balance was entered");
                return false;
            }
            BigDecimal amount;
            try {
                amount = Parser.parseNonNegativeAmount(input.trim(), "AMOUNT");
            } catch (FinNusException e) {
                LOGGER.fine("Rejected invalid opening balance during setup");
                ui.showError(e.getMessage());
                continue;
            }
            new OpeningBalanceCommand(amount).execute(transactions, ui);
            assert transactions.getOpeningBalance().compareTo(amount) == 0
                    : "Setup must store the entered opening balance";

            Confirmation confirmation = confirmBalance(ui, amount);
            if (confirmation == Confirmation.INPUT_ENDED) {
                return false;
            }
            if (confirmation == Confirmation.CONFIRMED) {
                LOGGER.info("First-run opening balance confirmed");
                return true;
            }
        }
    }

    /** Repeats the confirmation prompt until the user answers yes or no. */
    private Confirmation confirmBalance(Ui ui, BigDecimal amount) {
        while (true) {
            String answer = ui.readCommand("Confirm opening balance $"
                    + amount.toPlainString() + " SGD? [yes/no]: ");
            if (answer == null) {
                LOGGER.fine("First-run setup ended before the opening balance was confirmed");
                return Confirmation.INPUT_ENDED;
            }
            switch (answer.trim().toLowerCase(Locale.ROOT)) {
            case "yes", "y" -> {
                return Confirmation.CONFIRMED;
            }
            case "no", "n" -> {
                LOGGER.fine("User requested another opening balance entry");
                return Confirmation.REENTER;
            }
            default -> ui.showError("Please answer yes or no.");
            }
        }
    }
}
