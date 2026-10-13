package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import finnus.transaction.TransactionList;
import finnus.ui.Ui;

class OpeningBalanceCommandTest {
    private final Ui ui = new Ui();

    @Test
    public void execute_newList_setsOpeningBalance() {
        TransactionList transactions = new TransactionList();
        new OpeningBalanceCommand(new BigDecimal("1200.50")).execute(transactions, ui);

        assertEquals(new BigDecimal("1200.50"), transactions.getOpeningBalance());
        assertEquals(new BigDecimal("1200.50"), transactions.getBalance());
    }

    @Test
    public void execute_runTwice_overwritesOldValue() {
        TransactionList transactions = new TransactionList();
        new OpeningBalanceCommand(new BigDecimal("100")).execute(transactions, ui);
        new OpeningBalanceCommand(new BigDecimal("250")).execute(transactions, ui);

        assertEquals(new BigDecimal("250"), transactions.getOpeningBalance());
    }

    @Test
    public void execute_withExistingTransactions_shiftsBalanceButAddsNoTransaction() {
        TransactionList transactions = new TransactionList();
        new DepositCommand(new BigDecimal("50")).execute(transactions, ui);
        new WithdrawCommand(new BigDecimal("20"), "Books").execute(transactions, ui);

        new OpeningBalanceCommand(new BigDecimal("100")).execute(transactions, ui);

        // 100 + 50 - 20, and the opening balance is not counted as a transaction.
        assertEquals(new BigDecimal("130"), transactions.getBalance());
        assertEquals(2, transactions.size());
    }

    @Test
    public void execute_zero_resetsOpeningBalance() {
        TransactionList transactions = new TransactionList();
        new OpeningBalanceCommand(new BigDecimal("100")).execute(transactions, ui);
        new OpeningBalanceCommand(BigDecimal.ZERO).execute(transactions, ui);

        assertEquals(0, transactions.getOpeningBalance().signum());
    }
}
