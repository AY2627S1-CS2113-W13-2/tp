package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

/**
 * Checks the help output and ensures help preserves application state.
 */
class HelpCommandTest {
    @Test
    public void execute_help_printsCompleteUsageMessage() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try (PrintStream capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOutput);
            new HelpCommand().execute(new TransactionList(), new Ui());
        } finally {
            // Restore console output even if execution fails so other tests are unaffected.
            System.setOut(originalOutput);
        }

        // Keep expected text independent of HelpCommand so output changes are detected.
        String expectedOutput = String.join(System.lineSeparator(),
                "____________________________________________________________",
                "Available commands:",
                "+----------+-----------------+--------------------------------+",
                "| Command  | Description     | Syntax                         |",
                "+----------+-----------------+--------------------------------+",
                "| deposit  | Record income   | deposit AMOUNT [d/DESCRIPTION] |",
                "| withdraw | Record spending | withdraw AMOUNT d/DESCRIPTION  |",
                "| recent   | Recent activity | recent [NUMBER]                |",
                "| delete   | Delete a record | delete INDEX                   |",
                "| opening  | Opening balance | opening AMOUNT                 |",
                "| balance  | Current balance | balance                        |",
                "| rate     | Manage FX rates | rate [CURRENCY RATE]           |",
                "| help     | List commands   | help                           |",
                "| exit     | Close FinNUS    | exit                           |",
                "+----------+-----------------+--------------------------------+",
                "",
                "[] means optional.",
                "____________________________________________________________",
                "");
        assertEquals(expectedOutput, output.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void execute_help_preservesExistingTransactions() {
        TransactionList transactions = new TransactionList();
        transactions.add(new Transaction(TransactionType.DEPOSIT, new BigDecimal("100"), "Allowance"));
        transactions.add(new Transaction(TransactionType.WITHDRAWAL, new BigDecimal("4.50"), "Lunch"));
        List<Transaction> expectedTransactions = transactions.getAll();

        new HelpCommand().execute(transactions, new Ui());

        // Transactions are immutable, so list equality checks their count, order, and contents.
        assertEquals(expectedTransactions, transactions.getAll());
    }

    @Test
    public void isExit_help_returnsFalse() {
        assertFalse(new HelpCommand().isExit());
    }
}
