//@@author chancheese
package finnus.command;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import finnus.allowance.Allowance;
import finnus.currency.ForeignAmount;
import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

/**
 * Adds the allowance as a deposit for each allowance date that has arrived but has not been
 * added yet. FinNUS runs this itself when it starts and after each command, so the user never
 * types it; it is not listed in {@link CommandWord}.
 *
 * <p>FinNUS only runs while it is open, so a missed allowance date (e.g. the app was closed
 * for two months) is added the next time it runs, dated on the allowance day itself.
 */
public class CreditAllowanceCommand extends Command {
    private static final Logger logger = Logger.getLogger(CreditAllowanceCommand.class.getName());

    private final LocalDate today;
    /** Whether to explain why a due allowance could not be added, e.g. a missing rate. */
    private final boolean showsProblems;

    /**
     * Creates the command.
     *
     * @param today The current date; allowance dates up to and including it are added.
     * @param showsProblems Whether to warn if a due allowance cannot be added yet. FinNUS warns
     *     once at startup rather than after every command.
     */
    public CreditAllowanceCommand(LocalDate today, boolean showsProblems) {
        assert today != null : "Today's date must not be null";
        this.today = today;
        this.showsProblems = showsProblems;
    }

    @Override
    public void execute(TransactionList transactions, Ui ui) {
        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        Optional<Allowance> setting = transactions.getAllowance();
        if (setting.isEmpty() || !setting.get().hasAmount()) {
            return;
        }
        Allowance allowance = setting.get();
        List<String> lines = new ArrayList<>();
        while (!allowance.nextDueDate().isAfter(today)) {
            LocalDate dueDate = allowance.nextDueDate();
            Transaction deposit;
            try {
                deposit = createDeposit(allowance, transactions, dueDate);
            } catch (FinNusException e) {
                // Keep the date as due, so it is added once the problem is fixed, e.g. a rate is set.
                logger.fine("Allowance for " + dueDate + " not added yet: " + e.getMessage());
                if (showsProblems) {
                    lines.add("Your allowance for " + Transaction.formatDate(dueDate) + " was not added yet. "
                            + e.getMessage());
                }
                break;
            }
            transactions.add(deposit);
            allowance = allowance.withLastCredited(dueDate);
            transactions.setAllowance(allowance);
            logger.info("Added allowance deposit for " + dueDate);
            lines.add("Allowance added: " + deposit);
            deposit.getForeignAmount().ifPresent(foreign -> lines.add("Converted at "
                    + RateCommand.formatRate(foreign.currencyCode(), foreign.rate()) + "."));
        }
        if (!lines.isEmpty()) {
            ui.showMessage(lines);
        }
    }

    /**
     * Builds the deposit for one allowance date, converting a foreign amount at the current rate.
     *
     * @throws FinNusException If no rate is set for the currency, or the SGD value is out of range.
     */
    private static Transaction createDeposit(Allowance allowance, TransactionList transactions,
            LocalDate date) throws FinNusException {
        if (allowance.currencyCode() == null) {
            return new Transaction(TransactionType.DEPOSIT, Money.of(allowance.amount()),
                    Allowance.DEPOSIT_DESCRIPTION, null, date);
        }
        ForeignAmount foreign = transactions.getRates().convert(allowance.amount(), allowance.currencyCode());
        return new Transaction(TransactionType.DEPOSIT, foreign, Allowance.DEPOSIT_DESCRIPTION, null, date);
    }
}
//@@author
