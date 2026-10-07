package seedu.duke.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import seedu.duke.transaction.Transaction;
import seedu.duke.transaction.TransactionList;
import seedu.duke.transaction.TransactionType;
import seedu.duke.ui.Ui;

class DepositWithdrawCommandTest {
    private final Ui ui = new Ui();

    @Test
    public void execute_deposit_addsDepositToList() {
        TransactionList transactions = new TransactionList();
        new DepositCommand(500, "Monthly Allowance").execute(transactions, ui);

        assertEquals(1, transactions.size());
        Transaction added = transactions.get(0);
        assertEquals(TransactionType.DEPOSIT, added.getType());
        assertEquals(500, added.getAmount());
        assertEquals("Monthly Allowance", added.getDescription());
    }

    @Test
    public void execute_withdraw_addsWithdrawalToList() {
        TransactionList transactions = new TransactionList();
        new WithdrawCommand(4.5, "Lunch").execute(transactions, ui);

        assertEquals(1, transactions.size());
        Transaction added = transactions.get(0);
        assertEquals(TransactionType.WITHDRAWAL, added.getType());
        assertEquals(-4.5, added.getSignedAmount());
    }

    @Test
    public void execute_depositThenWithdraw_keepsOrder() {
        TransactionList transactions = new TransactionList();
        new DepositCommand(100, "").execute(transactions, ui);
        new WithdrawCommand(20, "Books").execute(transactions, ui);

        assertEquals(2, transactions.size());
        assertEquals(TransactionType.DEPOSIT, transactions.get(0).getType());
        assertEquals(TransactionType.WITHDRAWAL, transactions.get(1).getType());
    }
}
