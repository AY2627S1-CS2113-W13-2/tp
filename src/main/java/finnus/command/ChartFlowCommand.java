//@@author nkyjStar
package finnus.command;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

/** Displays deposits and withdrawals in chronological, four-period pages. */
public class ChartFlowCommand extends Command {
    private static final int PERIODS_PER_PAGE = 4;
    private static final Logger logger = Logger.getLogger(ChartFlowCommand.class.getName());

    private final LocalDate from;
    private final LocalDate to;
    private final ChartPeriod period;
    private final int page;

    /**
     * Creates a read-only cash-flow chart request with an inclusive date range.
     *
     * @param from The first date to include.
     * @param to The last date to include; not before {@code from}.
     * @param period The calendar interval used to group transactions.
     * @param page The one-based page number.
     */
    public ChartFlowCommand(LocalDate from, LocalDate to, ChartPeriod period, int page) {
        assert from != null : "Chart start date must not be null";
        assert to != null : "Chart end date must not be null";
        assert !from.isAfter(to) : "Chart start date must be on or before end date";
        assert period != null : "Chart period must not be null";
        assert page >= 1 : "Chart page must be positive";
        this.from = from;
        this.to = to;
        this.period = period;
        this.page = page;
    }

    /**
     * Aggregates only transactions inside the requested dates, including partial end periods.
     *
     * @param transactions The ledger to read without modifying it.
     * @param ui The UI used to display the selected page.
     * @throws FinNusException If the requested page is beyond the available pages.
     */
    @Override
    public void execute(TransactionList transactions, Ui ui) throws FinNusException {
        assertExecutionContext(transactions, ui);
        logger.fine("Preparing cash-flow chart by " + period + " from " + from + " to " + to
                + ", page " + page);
        List<LocalDate> starts = new ArrayList<>();
        for (LocalDate start = period.startOf(from); !start.isAfter(to); start = period.nextStart(start)) {
            starts.add(start);
        }
        int pageCount = (starts.size() + PERIODS_PER_PAGE - 1) / PERIODS_PER_PAGE;
        if (page > pageCount) {
            logger.warning("Cash-flow chart page " + page + " is beyond " + pageCount + " available page(s)");
            throw new FinNusException("Chart page is out of range. Choose a page from 1 to " + pageCount + ".");
        }

        int first = (page - 1) * PERIODS_PER_PAGE;
        int end = Math.min(first + PERIODS_PER_PAGE, starts.size());
        Map<LocalDate, FlowTotals> totals = new HashMap<>();
        for (Transaction transaction : transactions.getAll()) {
            if (transaction.getDate().isEmpty()) {
                continue;
            }
            LocalDate date = transaction.getDate().orElseThrow();
            if (date.isBefore(from) || date.isAfter(to)) {
                continue;
            }
            LocalDate start = period.startOf(date);
            FlowTotals total = totals.computeIfAbsent(start, ignored -> new FlowTotals());
            if (transaction.getType() == TransactionType.DEPOSIT) {
                total.deposits = total.deposits.add(transaction.getAmount());
            } else {
                total.withdrawals = total.withdrawals.add(transaction.getAmount());
            }
        }

        Money maximum = Money.ZERO;
        for (int index = first; index < end; index++) {
            FlowTotals total = totals.getOrDefault(starts.get(index), new FlowTotals());
            if (total.deposits.compareTo(maximum) > 0) {
                maximum = total.deposits;
            }
            if (total.withdrawals.compareTo(maximum) > 0) {
                maximum = total.withdrawals;
            }
        }

        List<String> lines = new ArrayList<>();
        lines.add("Cash flow " + period.name() + ": " + from + ".." + to
                + " (page " + page + "/" + pageCount + "):");
        for (int index = first; index < end; index++) {
            LocalDate start = starts.get(index);
            FlowTotals total = totals.getOrDefault(start, new FlowTotals());
            lines.add(start + " to " + period.nextStart(start).minusDays(1));
            lines.add("  Deposits    | " + ChartBar.render(total.deposits, maximum));
            lines.add("  Withdrawals | " + ChartBar.render(total.withdrawals, maximum));
        }
        lines.add("Use p/PAGE in the chart command to view another page.");
        ui.showMessage(lines);
        logger.fine("Displayed cash-flow chart page " + page + " of " + pageCount
                + " with " + (end - first) + " period(s)");
    }

    /** SGD totals for one calendar period. */
    private static class FlowTotals {
        private Money deposits = Money.ZERO;
        private Money withdrawals = Money.ZERO;
    }
}
//@@author
