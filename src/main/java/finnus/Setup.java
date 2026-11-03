package finnus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import finnus.alert.SpendingAlert;
import finnus.command.AllowanceCommand;
import finnus.command.Command;
import finnus.command.OpeningBalanceCommand;
import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.parser.Parser;
import finnus.transaction.TransactionList;
import finnus.ui.Confirmation;
import finnus.ui.Confirmation.Result;
import finnus.ui.Ui;

/**
 * Guides a first-time user through choosing and confirming the opening balance,
 * then optionally setting up their monthly allowance.
 */
//@@author lololol8484
public class Setup {
    //@@author nkyjStar
    private static final Logger logger = Logger.getLogger(Setup.class.getName());
    //@@author lololol8484

    //@@author chancheese
    /**
     * Runs setup using the real current date.
     *
     * @see #run(Ui, TransactionList, LocalDate)
     */
    public boolean run(Ui ui, TransactionList transactions) {
        return run(ui, transactions, LocalDate.now());
    }
    //@@author lololol8484

    //@@author chancheese
    /**
     * Prompts for an opening balance until the user confirms it or input ends, then asks
     * for the allowance, which the user may skip.
     *
     * @param ui The console interface used for prompts and responses.
     * @param transactions The ledger whose opening balance is being initialized.
     * @param today The current date, used to work out the allowance cycle.
     * @return {@code true} if the user confirmed a balance; {@code false} if input ended first.
     */
    public boolean run(Ui ui, TransactionList transactions, LocalDate today) {
        return run(ui, transactions, today, () -> {
            // Standalone callers do not need a persistence checkpoint.
        });
    }

    /**
     * Runs all setup steps, allowing the application to persist the balance before optional prompts.
     * Keep optional setup steps here so every application entry point follows the same workflow.
     *
     * @param ui The console interface used for prompts and responses.
     * @param transactions The ledger being initialized.
     * @param today The current date used for the allowance cycle.
     * @param onBalanceConfirmed Called once after balance confirmation, before optional setup.
     * @return Whether an opening balance was confirmed.
     */
    public boolean run(Ui ui, TransactionList transactions, LocalDate today, Runnable onBalanceConfirmed) {
        Objects.requireNonNull(onBalanceConfirmed, "Balance confirmation callback must not be null");
        if (!initializeOpeningBalance(ui, transactions)) {
            return false;
        }
        onBalanceConfirmed.run();
        configureOptionalSettings(ui, transactions, today);
        return true;
    }

    /** Prompts until an opening balance is confirmed or input ends. */
    boolean initializeOpeningBalance(Ui ui, TransactionList transactions) {
        //@@author lololol8484
        Command.assertExecutionContext(transactions, ui);
        assert transactions.size() == 0 : "First-run setup must not start with transactions";
        ui.showMessage("First-time setup", "Enter your current opening balance in SGD.",
                "This is your starting balance and will not be recorded as a transaction.");
        while (true) {
            String input = ui.readCommand("Opening balance (SGD): ");
            if (input == null) {
                //@@author nkyjStar
                logger.fine("First-run setup ended before an opening balance was entered");
                //@@author lololol8484
                return false;
            }
            Money amount;
            try {
                amount = Parser.parseNonNegativeAmount(input.trim(), "AMOUNT");
            } catch (FinNusException e) {
                //@@author nkyjStar
                logger.fine("Rejected invalid opening balance during setup");
                //@@author lololol8484
                ui.showError(e.getMessage());
                continue;
            }
            Result confirmation = Confirmation.ask(ui,
                    "Confirm opening balance " + amount.format() + " SGD? [yes/no]: ");
            if (confirmation == Result.INPUT_ENDED) {
                return false;
            }
            if (confirmation == Result.NO) {
                continue;
            }

            new OpeningBalanceCommand(amount).execute(transactions, ui);
            assert transactions.getOpeningBalance().compareTo(amount) == 0
                    : "Setup must store the entered opening balance";
            //@@author nkyjStar
            logger.info("First-run opening balance confirmed");
            //@@author lololol8484
            return true;
        }
    }

    /** Runs optional alert and allowance questions after the confirmed balance has been checkpointed. */
    private void configureOptionalSettings(Ui ui, TransactionList transactions, LocalDate today) {
        Optional<Money> balanceThreshold = askForBalanceThreshold(ui);
        boolean hasAllowanceAmount = askForAllowance(ui, transactions, today);
        Optional<BigDecimal> allowancePercentage = hasAllowanceAmount
                ? askForAllowancePercentage(ui) : Optional.empty();
        transactions.setSpendingAlert(new SpendingAlert(balanceThreshold.orElse(null),
                allowancePercentage.orElse(null)));
        logger.info("Spending alert settings recorded during setup");
    }

    //@@author chancheese
    /**
     * Asks for the allowance day and then its amount in SGD. Pressing Enter skips either one,
     * and so does ending the input, since the opening balance has already been confirmed.
     * Foreign currencies need a rate first, so they can only be set later with the command.
     */
    //@@author lololol8484
    private boolean askForAllowance(Ui ui, TransactionList transactions, LocalDate today) {
        //@@author chancheese
        ui.showMessage("Optional: when does your monthly allowance arrive?",
                "FinNUS can count the days until it arrives and add it for you each month.",
                "Press Enter to skip; you can set it later with: " + AllowanceCommand.EXAMPLE);
        Optional<Integer> day = askUntilValid(ui, "Allowance day (1-31): ", Parser::parseAllowanceDay);
        if (day.isEmpty()) {
            logger.fine("Allowance skipped during setup");
            //@@author lololol8484
            return false;
            //@@author chancheese
        }
        Optional<Money> amount = askUntilValid(ui, "Allowance amount in SGD (Enter to only count days): ",
                text -> Parser.parseAmount(text, "AMOUNT"));
        BigDecimal value = amount.map(Money::toBigDecimal).orElse(null);
        try {
            AllowanceCommand.set(day.get(), value, null, today).execute(transactions, ui);
        } catch (FinNusException e) {
            // Only foreign amounts can fail when set, and setup only takes SGD.
            assert false : "Setting an SGD allowance must not fail: " + e.getMessage();
            ui.showError(e.getMessage());
            //@@author lololol8484
            return false;
            //@@author chancheese
        }
        logger.info("Allowance set during setup");
        //@@author lololol8484
        return amount.isPresent();
        //@@author chancheese
    }

    //@@author lololol8484
    /** Asks for an optional balance floor that should trigger a withdrawal alert. */
    private Optional<Money> askForBalanceThreshold(Ui ui) {
        ui.showMessage("Optional: set a balance threshold for spending alerts.",
                "FinNUS will ask before a withdrawal takes your balance below it.",
                "Press Enter to skip.");
        return askUntilValid(ui, "Balance alert threshold in SGD: ", text -> {
            Money amount = Parser.parseNonNegativeAmount(text, "AMOUNT");
            if (!amount.isPositive()) {
                throw new FinNusException("The balance alert threshold must be greater than $0.00.");
            }
            return amount;
        });
    }

    /** Asks for an optional alert percentage of the configured monthly allowance. */
    private Optional<BigDecimal> askForAllowancePercentage(Ui ui) {
        ui.showMessage("Optional: set a percentage of your monthly allowance for spending alerts.",
                "FinNUS will ask before withdrawals in an allowance cycle exceed this percentage.",
                "Enter a whole percentage from 1 to 100, or press Enter to skip.");
        return askUntilValid(ui, "Allowance alert percentage (1-100): ", text -> {
            if (!text.matches(ValidationRegex.PERCENTAGE_REGEX)) {
                throw new FinNusException("Enter a whole percentage from 1 to 100.");
            }
            int percentage = Integer.parseInt(text);
            if (percentage < 1 || percentage > 100) {
                throw new FinNusException("Enter a whole percentage from 1 to 100.");
            }
            return BigDecimal.valueOf(percentage);
        });
    }
    //@@author

    /**
     * Reads one answer, asking again until it is valid.
     *
     * @return The parsed answer, or an empty Optional if the user pressed Enter or input ended.
     */
    private <T> Optional<T> askUntilValid(Ui ui, String prompt, AnswerParser<T> parser) {
        while (true) {
            String input = ui.readCommand(prompt);
            if (input == null || input.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(parser.parse(input.trim()));
            } catch (FinNusException e) {
                ui.showError(e.getMessage());
            }
        }
    }

    /** Converts one answer, like a parser method that may reject the text. */
    @FunctionalInterface
    private interface AnswerParser<T> {
        T parse(String text) throws FinNusException;
    }
    //@@author lololol8484

}
//@@author
