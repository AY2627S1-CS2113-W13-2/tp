package finnus.command;

import java.math.BigDecimal;

import finnus.transaction.TransactionList;
import finnus.ui.Ui;

public class BalanceCommand extends Command {
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        BigDecimal balance = transactions.getBalance();
        ui.showMessage("Your current balance is: $" + balance); 
    }
}
