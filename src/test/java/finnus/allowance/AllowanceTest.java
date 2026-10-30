//@@author chancheese
package finnus.allowance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Checks the cycle dates, including short months, leap years and the change of year.
 */
class AllowanceTest {
    private static Allowance onDay(int day) {
        return new Allowance(day, null, null, LocalDate.of(2026, 1, 1));
    }

    @ParameterizedTest(name = "day {0} on {1}: cycle {2} to {3}, next {4}")
    @CsvSource({
        // Before the allowance day: the cycle started last month.
        "5, 2026-10-29, 2026-10-05, 2026-11-04, 2026-11-05",
        "5, 2026-10-04, 2026-09-05, 2026-10-04, 2026-10-05",
        // On the allowance day, a new cycle starts today.
        "5, 2026-10-05, 2026-10-05, 2026-11-04, 2026-11-05",
        // Day 31 uses the last day of shorter months.
        "31, 2027-02-28, 2027-02-28, 2027-03-30, 2027-03-31",
        "31, 2027-02-27, 2027-01-31, 2027-02-27, 2027-02-28",
        "31, 2028-02-28, 2028-01-31, 2028-02-28, 2028-02-29",
        "31, 2026-04-30, 2026-04-30, 2026-05-30, 2026-05-31",
        // The cycle can cross into a new year.
        "15, 2027-01-03, 2026-12-15, 2027-01-14, 2027-01-15",
        "1, 2026-12-31, 2026-12-01, 2026-12-31, 2027-01-01"
    })
    public void cycleDates_variousDays_matchCalendar(int day, LocalDate today, LocalDate start,
            LocalDate end, LocalDate next) {
        Allowance allowance = onDay(day);

        assertEquals(start, allowance.cycleStart(today));
        assertEquals(end, allowance.cycleEnd(today));
        assertEquals(next, allowance.nextDate(today));
    }

    @Test
    public void daysUntilNext_onAllowanceDay_isWholeMonthNotZero() {
        assertEquals(31, onDay(5).daysUntilNext(LocalDate.of(2026, 10, 5)));
        assertEquals(1, onDay(5).daysUntilNext(LocalDate.of(2026, 10, 4)));
    }

    @Test
    public void startingFrom_dayAlreadyPassed_isNotDueUntilNextMonth() {
        Allowance allowance = Allowance.startingFrom(5, new BigDecimal("500"), null, LocalDate.of(2026, 10, 29));

        assertEquals(LocalDate.of(2026, 10, 5), allowance.lastCredited());
        assertEquals(LocalDate.of(2026, 11, 5), allowance.nextDueDate());
    }

    @Test
    public void startingFrom_onAllowanceDay_treatsTodayAsAlreadyReceived() {
        Allowance allowance = Allowance.startingFrom(5, new BigDecimal("500"), null, LocalDate.of(2026, 10, 5));

        assertEquals(LocalDate.of(2026, 11, 5), allowance.nextDueDate());
    }

    @Test
    public void nextDueDate_afterShortMonth_returnsToFullDay() {
        Allowance allowance = new Allowance(31, new BigDecimal("500"), null, LocalDate.of(2027, 2, 28));

        assertEquals(LocalDate.of(2027, 3, 31), allowance.nextDueDate());
    }

    @Test
    public void formatAmount_sgdAndForeign_showsAmountAsTyped() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        assertEquals("$500.00", new Allowance(5, new BigDecimal("500"), null, date).formatAmount());
        assertEquals("JPY 60000", new Allowance(5, new BigDecimal("60000"), "JPY", date).formatAmount());
    }
}
//@@author
