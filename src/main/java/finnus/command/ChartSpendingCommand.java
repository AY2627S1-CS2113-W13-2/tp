//@@author nkyjStar
package finnus.command;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import finnus.category.CategoryList;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

/** Displays withdrawal totals by category, either overall or in one calendar period. */
public class ChartSpendingCommand extends Command {
    private static final Logger logger = Logger.getLogger(ChartSpendingCommand.class.getName());

    private final ChartPeriod period;
    private final LocalDate date;

    /**
     * Creates an overall chart when both arguments are null, or a dated chart otherwise.
     *
     * @param period The calendar interval to show, or null for overall spending.
     * @param date A date within that interval, or null for overall spending.
     */
    public ChartSpendingCommand(ChartPeriod period, LocalDate date) {
        assert (period == null) == (date == null) : "Period and date must be supplied together";
        this.period = period;
        this.date = date;
    }

    /**
     * Aggregates recorded SGD withdrawal values and shows only categories with spending.
     *
     * @param transactions The ledger to read without modifying it.
     * @param ui The UI used to display the category totals.
     */
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        assertExecutionContext(transactions, ui);
        logger.fine(period == null ? "Preparing overall spending chart"
                : "Preparing spending chart for " + period + " containing " + date);
        Map<String, Money> totals = new HashMap<>();
        LocalDate start = period == null ? null : period.startOf(date);
        LocalDate end = period == null ? null : period.nextStart(start);
        for (Transaction transaction : transactions.getAll()) {
            if (transaction.getType() != TransactionType.WITHDRAWAL) {
                continue;
            }
            if (start != null && (transaction.getDate().isEmpty()
                    || transaction.getDate().orElseThrow().isBefore(start)
                    || !transaction.getDate().orElseThrow().isBefore(end))) {
                continue;
            }
            String category = transaction.getCategory().orElse(CategoryList.DEFAULT_CATEGORY);
            totals.merge(category, transaction.getAmount(), Money::add);
        }

        String heading = period == null ? "Overall spending by category:"
                : "Spending " + period.name() + ": " + start + ".." + end.minusDays(1) + ":";
        if (totals.isEmpty()) {
            ui.showMessage(heading, period == null ? "No spending recorded." : "No spending in this period.");
            logger.fine("Displayed spending chart with no withdrawals in the selected interval");
            return;
        }

        Money maximum = Money.ZERO;
        for (Money total : totals.values()) {
            if (total.compareTo(maximum) > 0) {
                maximum = total;
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add(heading);
        for (String category : transactions.getCategories().getAll()) {
            Money total = totals.get(category);
            if (total != null) {
                lines.add(String.format("%-20s | %s", category, ChartBar.render(total, maximum)));
            }
        }
        ui.showMessage(lines);
        logger.fine("Displayed spending chart with " + totals.size() + " category total(s)");
    }
}
//@@author
