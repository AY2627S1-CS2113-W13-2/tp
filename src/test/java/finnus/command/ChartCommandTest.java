//@@author nkyjStar
package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

/** Checks chart period boundaries, totals, pagination, and category filtering. */
class ChartCommandTest {
    private final RecordingUi ui = new RecordingUi();

    @Test
    void execute_dailyFlowRange_displaysFourPeriodsPerPageAndExactTotals() throws FinNusException {
        TransactionList transactions = new TransactionList();
        transactions.add(new Transaction(TransactionType.DEPOSIT, Money.of("10.50"), "Income", null,
                LocalDate.of(2026, 10, 1)));
        transactions.add(new Transaction(TransactionType.WITHDRAWAL, Money.of("2.25"), "Food", "Food",
                LocalDate.of(2026, 10, 5)));
        transactions.add(new Transaction(TransactionType.DEPOSIT, Money.of("99"), "Outside", null,
                LocalDate.of(2026, 9, 30)));

        new ChartFlowCommand(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6), ChartPeriod.DAY, 1)
                .execute(transactions, ui);
        assertTrue(ui.lines.getFirst().contains("page 1/2"));
        assertEquals(4, ui.lines.stream().filter(line -> line.matches("2026-10-\\d{2} to .*"))
                .count());
        assertTrue(ui.lines.stream().anyMatch(line -> line.contains("$10.50")));
        assertTrue(ui.lines.stream().noneMatch(line -> line.contains("$99.00")));
        assertTrue(ui.lines.stream().allMatch(line -> line.length() <= 60));

        new ChartFlowCommand(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6), ChartPeriod.DAY, 2)
                .execute(transactions, ui);
        assertTrue(ui.lines.getFirst().contains("page 2/2"));
        assertTrue(ui.lines.contains("2026-10-05 to 2026-10-05"));
        assertTrue(ui.lines.stream().anyMatch(line -> line.contains("$2.25")));
        assertEquals(2, ui.lines.stream().filter(line -> line.matches("2026-10-\\d{2} to .*"))
                .count());
        assertThrows(FinNusException.class, () -> new ChartFlowCommand(LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 6), ChartPeriod.DAY, 3).execute(transactions, ui));
    }

    @Test
    void execute_partialWeekRange_excludesTransactionsOutsideRequestedDates() throws FinNusException {
        TransactionList transactions = new TransactionList();
        transactions.add(new Transaction(TransactionType.DEPOSIT, Money.of("10"), "Before", null,
                LocalDate.of(2026, 10, 19)));
        transactions.add(new Transaction(TransactionType.DEPOSIT, Money.of("5"), "Inside", null,
                LocalDate.of(2026, 10, 21)));
        new ChartFlowCommand(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22), ChartPeriod.WEEK, 1)
                .execute(transactions, ui);
        assertTrue(ui.lines.contains("2026-10-19 to 2026-10-25"));
        assertTrue(ui.lines.stream().anyMatch(line -> line.contains("$5.00")));
        assertTrue(ui.lines.stream().noneMatch(line -> line.contains("$10.00")));
    }

    @Test
    void execute_overallAndDatedSpending_displaysSelectedCategoryTotals() {
        TransactionList transactions = new TransactionList();
        transactions.add(new Transaction(TransactionType.WITHDRAWAL, Money.of("3.50"), "Old", "Food"));
        transactions.add(new Transaction(TransactionType.WITHDRAWAL, Money.of("2"), "Lunch", "Food",
                LocalDate.of(2026, 10, 20)));
        transactions.add(new Transaction(TransactionType.WITHDRAWAL, Money.of("7"), "Bus", "Transport",
                LocalDate.of(2026, 10, 21)));
        transactions.add(new Transaction(TransactionType.DEPOSIT, Money.of("100"), "Income", null,
                LocalDate.of(2026, 10, 21)));

        new ChartSpendingCommand(null, null).execute(transactions, ui);
        assertTrue(ui.lines.stream().anyMatch(line -> line.startsWith("Food") && line.contains("$5.50")));
        assertTrue(ui.lines.stream().anyMatch(line -> line.startsWith("Transport") && line.contains("$7.00")));
        assertTrue(ui.lines.stream().noneMatch(line -> line.contains("$100.00")));
        assertTrue(ui.lines.stream().allMatch(line -> line.length() <= 60));

        new ChartSpendingCommand(ChartPeriod.DAY, LocalDate.of(2026, 10, 20)).execute(transactions, ui);
        assertTrue(ui.lines.stream().anyMatch(line -> line.startsWith("Food") && line.contains("$2.00")));
        assertTrue(ui.lines.stream().noneMatch(line -> line.startsWith("Transport")));

        new ChartSpendingCommand(ChartPeriod.MONTH, LocalDate.of(2026, 10, 20)).execute(transactions, ui);
        assertTrue(ui.lines.stream().anyMatch(line -> line.startsWith("Food") && line.contains("$2.00")));
        assertTrue(ui.lines.stream().anyMatch(line -> line.startsWith("Transport") && line.contains("$7.00")));
    }

    @Test
    void execute_emptySpendingPeriod_displaysNoSpendingMessage() {
        new ChartSpendingCommand(ChartPeriod.YEAR, LocalDate.of(2026, 1, 1))
                .execute(new TransactionList(), ui);
        assertEquals("No spending in this period.", ui.lines.get(1));
    }

    @Test
    void startOf_datesWithinPeriods_returnsCalendarStartDates() {
        LocalDate date = LocalDate.of(2026, 10, 21);
        assertEquals(LocalDate.of(2026, 10, 21), ChartPeriod.DAY.startOf(date));
        assertEquals(LocalDate.of(2026, 10, 19), ChartPeriod.WEEK.startOf(date));
        assertEquals(LocalDate.of(2026, 10, 1), ChartPeriod.MONTH.startOf(date));
        assertEquals(LocalDate.of(2026, 1, 1), ChartPeriod.YEAR.startOf(date));
    }

    @Test
    void nextStart_yearStart_returnsFollowingYearStart() {
        LocalDate date = LocalDate.of(2026, 10, 21);
        assertEquals(LocalDate.of(2027, 1, 1), ChartPeriod.YEAR.nextStart(ChartPeriod.YEAR.startOf(date)));
    }

    @Test
    void execute_outOfRangeFlowPage_throwsFinNusExceptionAndLogsWarning() {
        Logger logger = Logger.getLogger(ChartFlowCommand.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Level originalLevel = logger.getLevel();
        logger.setLevel(Level.WARNING);
        logger.addHandler(handler);
        try {
            assertThrows(FinNusException.class, () -> new ChartFlowCommand(LocalDate.of(2026, 10, 20),
                    LocalDate.of(2026, 10, 20), ChartPeriod.DAY, 2)
                    .execute(new TransactionList(), ui));
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(originalLevel);
        }
        assertTrue(records.stream().anyMatch(record -> record.getLevel() == Level.WARNING
                && record.getMessage().contains("page 2 is beyond 1")));
    }

    /** Captures chart output without printing it during tests. */
    private static class RecordingUi extends Ui {
        private List<String> lines = List.of();

        @Override
        public void showMessage(String... messageLines) {
            lines = List.of(messageLines);
        }

        @Override
        public void showMessage(List<String> messageLines) {
            lines = new ArrayList<>(messageLines);
        }
    }
}
//@@author
