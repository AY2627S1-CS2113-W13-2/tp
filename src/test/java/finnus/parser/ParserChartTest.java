//@@author nkyjStar
package finnus.parser;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.command.ChartFlowCommand;
import finnus.command.ChartSpendingCommand;
import finnus.exception.FinNusException;

/** Checks chart command syntax with a fixed current date. */
class ParserChartTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 30);

    @Test
    void parse_validChartInputs_returnsMatchingChartCommands() throws FinNusException {
        assertInstanceOf(ChartFlowCommand.class,
                Parser.parse("chart flow from/2026-01-01 to/2026-10-30 int/YEAR p/2", TODAY));
        assertInstanceOf(ChartFlowCommand.class,
                Parser.parse("chart flow int/week to/2026-10-30 from/2026-10-01", TODAY));
        assertInstanceOf(ChartSpendingCommand.class, Parser.parse("chart spending", TODAY));
        assertInstanceOf(ChartSpendingCommand.class,
                Parser.parse("chart spending on/2026-10-21 int/MONTH", TODAY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"chart", "chart other", "chart flow", "chart flow from/2026-10-01 to/2026-10-30",
        "chart flow from/2026-10-31 to/2026-10-31 int/DAY",
        "chart flow from/2026-10-21 to/2026-10-20 int/DAY",
        "chart flow from/2026-02-30 to/2026-10-30 int/DAY",
        "chart flow from/2026-10-01 to/2026-10-30 int/HOUR",
        "chart flow from/2026-10-01 to/2026-10-30 int/DAY p/0",
        "chart flow from/2026-10-01 to/2026-10-30 int/DAY p/2147483648",
        "chart flow from/2026-10-01 from/2026-10-02 to/2026-10-30 int/DAY",
        "chart flow from/2026-10-01 to/2026-10-30 int/DAY extra",
        "chart spending int/WEEK", "chart spending on/2026-10-21",
        "chart spending int/WEEK on/2026-10-21 p/2",
        "chart spending int/WEEK on/2026-10-21 on/2026-10-22"})
    void parse_invalidChartInput_throwsFinNusException(String input) {
        assertThrows(FinNusException.class, () -> Parser.parse(input, TODAY));
    }

    @Test
    void parse_invalidFlowAndSpendingInputs_logsWarnings() {
        Logger logger = Logger.getLogger(Parser.class.getName());
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
            assertThrows(FinNusException.class, () -> Parser.parse("chart flow int/DAY", TODAY));
            assertThrows(FinNusException.class, () -> Parser.parse("chart spending int/WEEK", TODAY));
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(originalLevel);
        }
        assertTrue(records.stream().anyMatch(record -> record.getLevel() == Level.WARNING
                && record.getMessage().contains("Rejected flow chart command")));
        assertTrue(records.stream().anyMatch(record -> record.getLevel() == Level.WARNING
                && record.getMessage().contains("Rejected spending chart command")));
    }
}
//@@author
