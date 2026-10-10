package finnus.command;

import java.util.List;

import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Returns a list of the most recent transactions
 */
public class RecentTransactionCommand extends Command {
    private final int length;

    /**
     * Creates a RecentTransactionCommand with a default length of 5
     */
    public RecentTransactionCommand() {
        this.length = 5;
    }

    /**
     * Creates a RecentTransactioNCommand with a specified desired length
     * 
     * @param length The specified length of the recent transactions list
     */
    public RecentTransactionCommand(int length) {
        this.length = length;
    }

    /**
     * Returns the transaction details as a String
     * 
     * @param index The 1-based index of the transaction
     * @param transaction The transcation to print
     */
    private String getMessageToAppend(int index, Transaction transaction) {
        String messageToAppend = "";
        messageToAppend += Integer.toString(index) + ". " + transaction.toString();
        return messageToAppend;
    }

    /**
     * Returns the list of recent transactions as a String
     * 
     * @param recentTransactions The list of recent transactions to show
     */
    private List<String> getMessageToShow(List<Transaction> recentTransactions) {
        List<String> messageList = List.of();
        for (int i = 0; i < recentTransactions.size(); i++) {
            Transaction t = recentTransactions.get(i);

            String messageToAppend = getMessageToAppend(i + 1, t);
            messageList.add(messageToAppend);
        }
        return messageList;
    }

    /**
     * Shows the list of recent transactions to the user
     */
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        // Get a list of the most recent transactions with a specified length
        List<Transaction> recentTransactionsList = transactions.getRecentTransactions(length);

        // Inform user if transaction list is currently empty
        if (recentTransactionsList.size() <= 0) {
            ui.showMessage("Your list of transactions is currently empty.");
            return;
        }

        List<String> messageList = getMessageToShow(recentTransactionsList);
        ui.showMessage(messageList);
    }
}
