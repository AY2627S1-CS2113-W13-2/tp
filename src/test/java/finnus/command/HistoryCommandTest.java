package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

/** Checks history ordering, page navigation, anchored resizing, and boundaries. */
class HistoryCommandTest {
    private final RecordingUi ui = new RecordingUi();

    @Test
    public void execute_andNavigate_showGlobalIndicesInNewestFirstOrder() throws FinNusException {
        TransactionList transactions = transactions(7);
        HistoryCommand history = new HistoryCommand(3, 2);

        history.execute(transactions, ui);
        assertEquals("Transaction history: 4-6 of 7 (3 per page):", ui.lines.getFirst());
        assertEquals("4. " + transactions.get(3), ui.lines.get(1));
        assertEquals("6. " + transactions.get(1), ui.lines.get(3));

        history.handleInput("next", transactions, ui);
        assertEquals("Transaction history: 7-7 of 7 (3 per page):", ui.lines.getFirst());
        assertEquals("7. " + transactions.get(0), ui.lines.get(1));

        history.handleInput("prev", transactions, ui);
        assertEquals("Transaction history: 4-6 of 7 (3 per page):", ui.lines.getFirst());
        assertEquals(7, transactions.size());
    }

    @Test
    public void resize_keepsFirstVisibleTransaction_thenPageJumpUsesStandardBoundary() throws FinNusException {
        TransactionList transactions = transactions(10);
        HistoryCommand history = new HistoryCommand(5, 2);
        history.execute(transactions, ui);

        history.handleInput("n/3", transactions, ui);
        assertEquals("Transaction history: 6-8 of 10 (3 per page):", ui.lines.getFirst());
        assertEquals("6. " + transactions.get(4), ui.lines.get(1));

        history.handleInput("p/2", transactions, ui);
        assertEquals("Transaction history: 4-6 of 10 (3 per page):", ui.lines.getFirst());
    }

    @Test
    public void invalidNavigation_preservesCurrentPage() throws FinNusException {
        TransactionList transactions = transactions(4);
        HistoryCommand history = new HistoryCommand(3, 1);
        history.execute(transactions, ui);

        assertThrows(FinNusException.class, () -> history.handleInput("prev", transactions, ui));
        assertThrows(FinNusException.class, () -> history.handleInput("p/3", transactions, ui));
        assertThrows(FinNusException.class, () -> history.handleInput("n/0", transactions, ui));
        assertThrows(FinNusException.class, () -> history.handleInput("n/2147483648", transactions, ui));
        assertThrows(FinNusException.class, () -> history.handleInput("deposit 5", transactions, ui));
        history.handleInput("next", transactions, ui);
        assertEquals("Transaction history: 4-4 of 4 (3 per page):", ui.lines.getFirst());
        assertThrows(FinNusException.class, () -> history.handleInput("next", transactions, ui));
        assertEquals("Transaction history: 4-4 of 4 (3 per page):", ui.lines.getFirst());
    }

    @Test
    public void emptyHistory_canBeViewedAndStopped() throws FinNusException {
        TransactionList transactions = new TransactionList();
        HistoryCommand history = new HistoryCommand(5, 1);

        history.execute(transactions, ui);
        assertTrue(ui.lines.contains("Your list of transactions is currently empty."));
        assertThrows(FinNusException.class, () -> history.handleInput("next", transactions, ui));
        assertFalse(history.handleInput("n/2", transactions, ui));
        assertTrue(history.handleInput("stop", transactions, ui));
        assertEquals("Stopped viewing transaction history.", ui.lines.getFirst());
        assertThrows(FinNusException.class, () -> new HistoryCommand(5, 2).execute(transactions, ui));
    }

    /** Creates transactions in insertion order with distinct descriptions for order checks. */
    private static TransactionList transactions(int count) {
        TransactionList result = new TransactionList();
        for (int number = 1; number <= count; number++) {
            result.add(new Transaction(TransactionType.DEPOSIT, Money.of("1"), "Entry " + number));
        }
        return result;
    }

    /** Captures each displayed message as separate lines. */
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
