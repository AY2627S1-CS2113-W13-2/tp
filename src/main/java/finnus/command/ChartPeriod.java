//@@author nkyjStar
package finnus.command;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

import finnus.exception.FinNusException;

/** Calendar periods shared by the cash-flow and spending charts. */
public enum ChartPeriod {
    DAY,
    WEEK,
    MONTH,
    YEAR;

    /** Returns the first day of the period containing {@code date}. Weeks start on Monday. */
    public LocalDate startOf(LocalDate date) {
        return switch (this) {
        case DAY -> date;
        case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        case MONTH -> date.withDayOfMonth(1);
        case YEAR -> date.withDayOfYear(1);
        };
    }

    /** Returns the first day after the period beginning at {@code start}. */
    public LocalDate nextStart(LocalDate start) {
        return switch (this) {
        case DAY -> start.plusDays(1);
        case WEEK -> start.plusWeeks(1);
        case MONTH -> start.plusMonths(1);
        case YEAR -> start.plusYears(1);
        };
    }

    /** Parses a chart period without depending on the machine's default locale. */
    public static ChartPeriod parse(String text) throws FinNusException {
        try {
            return valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new FinNusException("Invalid chart period '" + text + "'. Use DAY, WEEK, MONTH, or YEAR.");
        }
    }
}
//@@author
