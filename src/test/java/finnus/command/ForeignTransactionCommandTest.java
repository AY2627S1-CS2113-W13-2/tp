//@@author chancheese
package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import finnus.currency.ForeignAmount;
import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

class ForeignTransactionCommandTest {
    private final Ui ui = new Ui();

    @Test
    public void execute_withdrawalWithRate_addsSgdValueAndKeepsOriginal() throws FinNusException {
        TransactionList transactions = new TransactionList();
        transactions.getRates().setRate("JPY", new BigDecimal("110"));

        new ForeignTransactionCommand(TransactionType.WITHDRAWAL, new BigDecimal("1100"), "JPY", "Ramen")
                .execute(transactions, ui);

        Transaction added = transactions.get(0);
        assertEquals(TransactionType.WITHDRAWAL, added.getType());
        assertEquals(Money.of("10"), added.getAmount());
        assertEquals("Ramen", added.getDescription());
        assertEquals(new ForeignAmount("JPY", new BigDecimal("1100"), new BigDecimal("110")),
                added.getForeignAmount().orElseThrow());
        assertEquals(Money.of("-10"), transactions.getBalance());
    }

    @Test
    public void execute_rateChangedLater_keepsOriginalSgdValue() throws FinNusException {
        TransactionList transactions = new TransactionList();
        transactions.getRates().setRate("USD", new BigDecimal("0.5"));
        new ForeignTransactionCommand(TransactionType.DEPOSIT, new BigDecimal("50"), "USD", "")
                .execute(transactions, ui);

        transactions.getRates().setRate("USD", new BigDecimal("0.25"));

        assertEquals(Money.of("100"), transactions.getBalance());
    }

    @Test
    public void execute_noRateSet_throwsAndAddsNothing() {
        TransactionList transactions = new TransactionList();
        ForeignTransactionCommand command = new ForeignTransactionCommand(TransactionType.DEPOSIT,
                new BigDecimal("50"), "USD", "");

        assertThrows(FinNusException.class, () -> command.execute(transactions, ui));
        assertEquals(0, transactions.size());
    }
}
