package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import finnus.exception.FinNusException;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

class RateCommandTest {
    private final Ui ui = new Ui();

    @Test
    public void execute_setRate_storesRate() {
        TransactionList transactions = new TransactionList();
        new RateCommand("JPY", new BigDecimal("110")).execute(transactions, ui);

        assertEquals(new BigDecimal("110"), transactions.getRates().getRate("JPY"));
    }

    @Test
    public void execute_setRate_doesNotAddTransaction() {
        TransactionList transactions = new TransactionList();
        new RateCommand("JPY", new BigDecimal("110")).execute(transactions, ui);

        assertEquals(0, transactions.size());
    }

    @Test
    public void execute_listWithNoRates_showsHowToSetOne() {
        String output = runAndCaptureOutput(new RateCommand(), new TransactionList());
        assertTrue(output.contains("No exchange rates set yet"));
    }

    @Test
    public void execute_listWithRates_showsEachRate() {
        TransactionList transactions = new TransactionList();
        transactions.getRates().setRate("JPY", new BigDecimal("110.00"));
        transactions.getRates().setRate("MYR", new BigDecimal("3.45"));

        String output = runAndCaptureOutput(new RateCommand(), transactions);

        // Trailing zeros are dropped, so 110.00 is shown as 110.
        assertTrue(output.contains("1 SGD = 110 JPY"));
        assertTrue(output.contains("1 SGD = 3.45 MYR"));
    }

    /**
     * Runs the command and returns what it printed, restoring System.out afterwards.
     */
    private String runAndCaptureOutput(Command command, TransactionList transactions) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;
        try (PrintStream capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOutput);
            command.execute(transactions, ui);
        } catch (FinNusException e) {
            throw new AssertionError("Command should not fail", e);
        } finally {
            System.setOut(originalOutput);
        }
        return output.toString(StandardCharsets.UTF_8);
    }
}
