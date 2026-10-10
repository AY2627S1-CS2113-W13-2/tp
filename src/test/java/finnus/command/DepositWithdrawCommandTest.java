package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

class DepositWithdrawCommandTest {
    private final Ui ui = new Ui();

    @Test
    public void execute_deposit_addsDepositToList() {
        TransactionList transactions = new TransactionList();
        new DepositCommand(new BigDecimal("500"), "Monthly Allowance").execute(transactions, ui);

        assertEquals(1, transactions.size());
        Transaction added = transactions.get(0);
        assertEquals(TransactionType.DEPOSIT, added.getType());
        assertEquals(new BigDecimal("500"), added.getAmount());
        assertEquals("Monthly Allowance", added.getDescription());
    }

    @Test
    public void execute_withdraw_addsWithdrawalToList() {
        TransactionList transactions = new TransactionList();
        new WithdrawCommand(new BigDecimal("4.50"), "Lunch").execute(transactions, ui);

        assertEquals(1, transactions.size());
        Transaction added = transactions.get(0);
        assertEquals(TransactionType.WITHDRAWAL, added.getType());
        assertEquals(new BigDecimal("-4.50"), added.getSignedAmount());
    }

    @Test
    public void execute_depositThenWithdraw_keepsOrder() {
        TransactionList transactions = new TransactionList();
        new DepositCommand(new BigDecimal("100")).execute(transactions, ui);
        new WithdrawCommand(new BigDecimal("20"), "Books").execute(transactions, ui);

        assertEquals(2, transactions.size());
        assertEquals(TransactionType.DEPOSIT, transactions.get(0).getType());
        assertEquals(TransactionType.WITHDRAWAL, transactions.get(1).getType());
    }

    @Test
    public void execute_depositWithoutDescription_storesEmptyDescription() {
        TransactionList transactions = new TransactionList();
        new DepositCommand(new BigDecimal("50")).execute(transactions, ui);

        assertEquals("", transactions.get(0).getDescription());
    }
}
