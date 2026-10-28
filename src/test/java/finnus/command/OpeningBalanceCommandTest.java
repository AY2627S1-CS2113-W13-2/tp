package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

class OpeningBalanceCommandTest {
    private final Ui ui = new Ui();

    @Test
    public void execute_newList_setsOpeningBalance() {
        TransactionList transactions = new TransactionList();
        new OpeningBalanceCommand(Money.of("1200.50")).execute(transactions, ui);

        assertEquals(Money.of("1200.50"), transactions.getOpeningBalance());
        assertEquals(Money.of("1200.50"), transactions.getBalance());
    }

    @Test
    public void execute_runTwice_overwritesOldValue() {
        TransactionList transactions = new TransactionList();
        new OpeningBalanceCommand(Money.of("100")).execute(transactions, ui);
        new OpeningBalanceCommand(Money.of("250")).execute(transactions, ui);

        assertEquals(Money.of("250"), transactions.getOpeningBalance());
    }

    @Test
    public void execute_withExistingTransactions_shiftsBalanceButAddsNoTransaction() throws FinNusException {
        TransactionList transactions = new TransactionList();
        new DepositCommand(Money.of("50")).execute(transactions, ui);
        new WithdrawCommand(Money.of("20"), "Books").execute(transactions, ui);

        new OpeningBalanceCommand(Money.of("100")).execute(transactions, ui);

        // 100 + 50 - 20, and the opening balance is not counted as a transaction.
        assertEquals(Money.of("130"), transactions.getBalance());
        assertEquals(2, transactions.size());
    }

    @Test
    public void execute_zero_resetsOpeningBalance() {
        TransactionList transactions = new TransactionList();
        new OpeningBalanceCommand(Money.of("100")).execute(transactions, ui);
        new OpeningBalanceCommand(Money.ZERO).execute(transactions, ui);

        assertEquals(Money.ZERO, transactions.getOpeningBalance());
    }
}
