//@@author chancheese
package finnus.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import finnus.allowance.Allowance;
import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

/**
 * Sets, shows or stops the user's monthly allowance, e.g. {@code allowance 5 a/500}.
 *
 * <p>The summary shows the current budget cycle, the days left until the next allowance,
 * and how much was received and spent in this cycle.
 */
public class AllowanceCommand extends Command {
    /** Example shown when no allowance is set. */
    public static final String EXAMPLE = "allowance 5 a/500";

    private static final Logger logger = Logger.getLogger(AllowanceCommand.class.getName());

    /** What the command does. */
    private enum Action {
        SHOW, SET, STOP
    }

    private final Action action;
    private final int day;
    private final BigDecimal amount;
    private final String currencyCode;
    private final LocalDate today;

    private AllowanceCommand(Action action, int day, BigDecimal amount, String currencyCode, LocalDate today) {
        assert today != null : "Today's date must not be null";
        this.action = action;
        this.day = day;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.today = today;
    }

    /** Creates a command that shows the allowance summary. */
    public static AllowanceCommand show(LocalDate today) {
        return new AllowanceCommand(Action.SHOW, 0, null, null, today);
    }

    /** Creates a command that removes the allowance, keeping deposits that were already added. */
    public static AllowanceCommand stop(LocalDate today) {
        return new AllowanceCommand(Action.STOP, 0, null, null, today);
    }

    /**
     * Creates a command that sets the allowance.
     *
     * @param day The day of the month, from 1 to 31.
     * @param amount How much the allowance is, or null to only track the cycle.
     * @param currencyCode The currency of {@code amount}, or null for SGD.
     * @param today The current date.
     */
    public static AllowanceCommand set(int day, BigDecimal amount, String currencyCode, LocalDate today) {
        assert day >= Allowance.MIN_DAY && day <= Allowance.MAX_DAY : "Day must be validated by the parser";
        assert currencyCode == null || amount != null : "A currency needs an amount";
        return new AllowanceCommand(Action.SET, day, amount, currencyCode, today);
    }

    /**
     * Carries out the command.
     *
     * @throws FinNusException If a foreign amount is too small or too large once converted
     *     at the current rate.
     */
    @Override
    public void execute(TransactionList transactions, Ui ui) throws FinNusException {
        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        switch (action) {
        case SET -> setAllowance(transactions, ui);
        case STOP -> stopAllowance(transactions, ui);
        default -> showAllowance(transactions, ui);
        }
    }

    /**
     * Saves the allowance and shows the summary. Allowance dates that have already passed are
     * never added, so a deposit the user recorded by hand is not counted twice.
     *
     * @throws FinNusException If a foreign amount is out of range at the current rate.
     */
    private void setAllowance(TransactionList transactions, Ui ui) throws FinNusException {
        // Check a foreign amount now, rather than failing on the allowance day itself.
        if (currencyCode != null && transactions.getRates().hasRate(currencyCode)) {
            transactions.getRates().convert(amount, currencyCode);
        }
        Allowance allowance = Allowance.startingFrom(day, amount, currencyCode, today);
        Optional<Allowance> previous = transactions.getAllowance();
        // An allowance still waiting to be added (e.g. for a missing rate) stays due when the same
        // day is set again, so re-entering the allowance never drops it.
        if (allowance.hasAmount() && previous.isPresent() && previous.get().hasAmount()
                && previous.get().day() == day && previous.get().lastCredited().isBefore(allowance.lastCredited())) {
            allowance = allowance.withLastCredited(previous.get().lastCredited());
        }
        transactions.setAllowance(allowance);
        logger.info("Allowance set for day " + day + (allowance.hasAmount() ? " with an amount" : ""));

        List<String> lines = new ArrayList<>();
        lines.add("Allowance set.");
        lines.addAll(summarise(allowance, transactions, today));
        if (allowance.hasAmount() && allowance.nextDueDate().isAfter(today)) {
            lines.add("From " + Transaction.formatDate(allowance.nextDueDate())
                    + ", FinNUS adds it as a deposit for you, so you do not need to record it.");
        } else if (allowance.hasAmount()) {
            lines.add("Your allowance for " + Transaction.formatDate(allowance.nextDueDate())
                    + " is still waiting to be added.");
        }
        if (currencyCode != null && !transactions.getRates().hasRate(currencyCode)) {
            lines.add("No exchange rate is set for " + currencyCode + " yet. Set one before then with: "
                    + CommandWord.RATE.getWord() + " " + currencyCode + " RATE");
        }
        ui.showMessage(lines);
    }

    /** Removes the allowance; deposits that were already added stay in the list. */
    private void stopAllowance(TransactionList transactions, Ui ui) {
        if (transactions.getAllowance().isEmpty()) {
            ui.showMessage("No allowance is set.");
            return;
        }
        transactions.setAllowance(null);
        logger.info("Allowance stopped");
        ui.showMessage("Allowance stopped. Deposits already added are kept.");
    }

    /** Shows the summary, or explains how to set an allowance if there is none. */
    private void showAllowance(TransactionList transactions, Ui ui) {
        Optional<Allowance> allowance = transactions.getAllowance();
        if (allowance.isEmpty()) {
            ui.showMessage("No allowance set yet. Set one with, e.g.: " + EXAMPLE);
            return;
        }
        ui.showMessage(summarise(allowance.get(), transactions, today));
    }

    /**
     * Returns the lines of the allowance summary, e.g.
     * <pre>
     * Allowance: $500.00 on day 5 (added automatically)
     * This cycle: 5 Oct 2026 - 4 Nov 2026
     * Next allowance in 7 days (5 Nov 2026).
     * Received this cycle: $500.00 in 1 deposit(s).
     * Spent this cycle: $18.30 in 3 withdrawal(s).
     * </pre>
     */
    static List<String> summarise(Allowance allowance, TransactionList transactions, LocalDate today) {
        LocalDate start = allowance.cycleStart(today);
        LocalDate next = allowance.nextDate(today);
        List<String> lines = new ArrayList<>();
        lines.add(describeSetting(allowance));
        lines.add("This cycle: " + Transaction.formatDate(start) + " - "
                + Transaction.formatDate(allowance.cycleEnd(today)));
        lines.add(countdown(allowance, today));

        CycleTotal received = new CycleTotal();
        CycleTotal spent = new CycleTotal();
        int undated = 0;
        for (Transaction transaction : transactions.getAll()) {
            Optional<LocalDate> date = transaction.getDate();
            if (date.isEmpty()) {
                undated++;
                continue;
            }
            // A cycle runs from its start date up to, but not including, the next allowance.
            if (date.get().isBefore(start) || !date.get().isBefore(next)) {
                continue;
            }
            CycleTotal total = transaction.getType() == TransactionType.DEPOSIT ? received : spent;
            total.add(transaction.getAmount());
        }
        lines.add("Received this cycle: " + received.amount.format() + " in " + received.count + " deposit(s).");
        lines.add("Spent this cycle: " + spent.amount.format() + " in " + spent.count + " withdrawal(s).");
        if (undated > 0) {
            lines.add(undated + " older transaction(s) have no date, so they are not counted.");
        }
        return lines;
    }

    /**
     * Returns the countdown line, e.g. {@code Next allowance in 7 days (5 Nov 2026).}
     * It is also shown by {@code balance}.
     */
    public static String countdown(Allowance allowance, LocalDate today) {
        long days = allowance.daysUntilNext(today);
        return "Next allowance in " + days + (days == 1 ? " day" : " days") + " ("
                + Transaction.formatDate(allowance.nextDate(today)) + ").";
    }

    /** Returns the first summary line, e.g. {@code Allowance: $500.00 on day 5 (added automatically)}. */
    private static String describeSetting(Allowance allowance) {
        String day = String.valueOf(allowance.day());
        // Days 29 to 31 do not exist in every month, so say what happens then.
        if (allowance.day() > 28) {
            day += " or the last day of shorter months";
        }
        if (!allowance.hasAmount()) {
            return "Allowance day: " + day;
        }
        return "Allowance: " + allowance.formatAmount() + " on day " + day + " (added automatically)";
    }

    /** Adds up the SGD value and number of transactions of one type within a cycle. */
    private static class CycleTotal {
        private Money amount = Money.ZERO;
        private int count;

        private void add(Money value) {
            amount = amount.add(value);
            count++;
        }
    }
}
//@@author
