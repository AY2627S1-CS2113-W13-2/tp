package finnus.command;

import java.math.BigDecimal;

import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Represents the command to get the user's balance
 */
public class BalanceCommand extends Command {
    /**
     * Prints the user's balance
     * 
     * @param transactions The list of transactions to get the balance from
     * @param ui The Ui instance
     */
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        BigDecimal balance = transactions.getBalance();
        ui.showMessage("Your current balance is: $" + balance); 
    }
}
