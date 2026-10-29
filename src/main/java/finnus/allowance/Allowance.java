//@@author chancheese
package finnus.allowance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

import finnus.currency.CurrencyRates;
import finnus.money.Money;

/**
 * The user's monthly allowance: the day it arrives and, optionally, how much it is.
 * The day splits time into budget cycles, e.g. day 5 gives cycles from the 5th of
 * one month to the 4th of the next.
 *
 * <p>If a month is shorter than the day, the allowance arrives on that month's last day,
 * e.g. day 31 is 28 Feb in a non-leap year.
 *
 * <p>This is an immutable record; {@link #withLastCredited} returns an updated copy.
 *
 * @param day The day of the month the allowance arrives, from 1 to 31.
 * @param amount How much the allowance is, or null to only track the cycle.
 * @param currencyCode The currency of {@code amount}, or null for SGD.
 * @param lastCredited The latest allowance date that has been added as a deposit, or that
 *     was skipped because it had already passed when the allowance was set.
 */
public record Allowance(int day, BigDecimal amount, String currencyCode, LocalDate lastCredited) {
    /** The smallest valid allowance day. */
    public static final int MIN_DAY = 1;
    /** The largest valid allowance day; shorter months use their last day instead. */
    public static final int MAX_DAY = 31;
    /** The description given to deposits that are added automatically. */
    public static final String DEPOSIT_DESCRIPTION = "Allowance";

    /**
     * Checks the values; invalid values here are programming errors, since the parser
     * and storage validate them first.
     */
    public Allowance {
        assert day >= MIN_DAY && day <= MAX_DAY : "Allowance day must be from 1 to 31";
        assert amount == null || amount.signum() > 0 : "Allowance amount must be positive";
        assert currencyCode == null || amount != null : "A currency needs an amount";
        assert currencyCode == null || !currencyCode.equals(CurrencyRates.BASE_CURRENCY)
                : "SGD is stored as a null currency";
        assert lastCredited != null : "Last credited date must not be null";
    }

    /**
     * Creates a new allowance starting from today. Allowance dates that have already passed,
     * including today, are treated as credited, so setting an allowance never adds a deposit
     * the user may have recorded by hand.
     *
     * @param day The day of the month, from 1 to 31.
     * @param amount How much the allowance is, or null to only track the cycle.
     * @param currencyCode The currency of {@code amount}, or null for SGD.
     * @param today The current date.
     */
    public static Allowance startingFrom(int day, BigDecimal amount, String currencyCode, LocalDate today) {
        return new Allowance(day, amount, currencyCode, cycleStart(day, today));
    }

    /**
     * Returns the date the allowance arrives in the given month, using the month's last day
     * if the month is shorter than {@code day}.
     */
    static LocalDate dateIn(YearMonth month, int day) {
        return month.atDay(Math.min(day, month.lengthOfMonth()));
    }

    /**
     * Returns the latest allowance date on or before {@code today}.
     */
    private static LocalDate cycleStart(int day, LocalDate today) {
        YearMonth thisMonth = YearMonth.from(today);
        LocalDate thisMonthsDate = dateIn(thisMonth, day);
        // On the allowance day itself, the new cycle starts today.
        if (!today.isBefore(thisMonthsDate)) {
            return thisMonthsDate;
        }
        return dateIn(thisMonth.minusMonths(1), day);
    }

    /** Returns the first day of the cycle that contains {@code today}. */
    public LocalDate cycleStart(LocalDate today) {
        return cycleStart(day, today);
    }

    /** Returns the date of the next allowance after {@code today}; never today itself. */
    public LocalDate nextDate(LocalDate today) {
        return dateIn(YearMonth.from(cycleStart(today)).plusMonths(1), day);
    }

    /** Returns the last day of the cycle that contains {@code today}. */
    public LocalDate cycleEnd(LocalDate today) {
        return nextDate(today).minusDays(1);
    }

    /** Returns how many days are left until the next allowance; always at least 1. */
    public long daysUntilNext(LocalDate today) {
        return ChronoUnit.DAYS.between(today, nextDate(today));
    }

    /** Returns the first allowance date that has not been credited yet. */
    public LocalDate nextDueDate() {
        return dateIn(YearMonth.from(lastCredited).plusMonths(1), day);
    }

    /** Returns whether FinNUS should add the allowance as a deposit automatically. */
    public boolean hasAmount() {
        return amount != null;
    }

    /** Returns a copy of this allowance with a new last credited date. */
    public Allowance withLastCredited(LocalDate date) {
        return new Allowance(day, amount, currencyCode, date);
    }

    /**
     * Returns the amount as the user typed it, e.g. {@code $500.00} or {@code JPY 60000}.
     * Call {@link #hasAmount} first.
     */
    public String formatAmount() {
        assert hasAmount() : "Allowance has no amount";
        if (currencyCode == null) {
            return Money.of(amount).format();
        }
        return currencyCode + " " + amount.toPlainString();
    }
}
//@@author
