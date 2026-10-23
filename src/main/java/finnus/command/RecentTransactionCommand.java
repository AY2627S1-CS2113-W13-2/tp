package finnus.command;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/**
 * Displays a list of the most recent transactions.
 */
public class RecentTransactionCommand extends Command {
    private static final Logger logger = Logger.getLogger(RecentTransactionCommand.class.getName());

    private final int length;

    /**
     * Creates a command that displays up to five recent transactions.
     */
    public RecentTransactionCommand() {
        logger.fine("Using default recent transaction length");

        this.length = 5;
    }

    /**
     * Creates a command that displays up to the specified number of transactions.
     *
     * @param length The specified length of the recent transactions list
     */
    public RecentTransactionCommand(int length) {
        assert length > 0 : "Recent transaction length must be positive";
        logger.fine("Using requested recent transaction length");
        this.length = length;
    }

    /**
     * Formats one transaction as a numbered line for the recent-transactions output.
     *
     * @param index The 1-based index of the transaction
     * @param transaction The transaction to print
     */
    private String getMessageToAppend(int index, Transaction transaction) {
        String messageToAppend = "";
        messageToAppend += Integer.toString(index) + ". " + transaction.toString();
        return messageToAppend;
    }

    /**
     * Formats recent transactions as numbered output lines.
     *
     * @param recentTransactions The list of recent transactions to show
     */
    private List<String> getMessageToShow(List<Transaction> recentTransactions) {
        List<String> messageList = new ArrayList<>();

        for (int i = 0; i < recentTransactions.size(); i++) {
            Transaction t = recentTransactions.get(i);

            String messageToAppend = getMessageToAppend(i + 1, t);
            messageList.add(messageToAppend);
        }
        return messageList;
    }

    /**
     * Shows the requested recent transactions, or an empty-list message when none exist.
     */
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        assert transactions != null : "Transaction list must not be null";
        assert ui != null : "UI must not be null";
        logger.fine("Getting recent transactions");

        // Get a list of the most recent transactions with a specified length
        List<Transaction> recentTransactionsList = transactions.getRecentTransactions(length);

        // Inform user if transaction list is currently empty
        if (recentTransactionsList.isEmpty()) {
            logger.fine("No transactions available to display");

            ui.showMessage("Your list of transactions is currently empty.");
            return;
        }

        logger.fine("Formatting recent transactions for display");

        List<String> messageList = getMessageToShow(recentTransactionsList);
        messageList.add(0, "Here is a list of the recent "
                + Integer.toString(messageList.size()) + " transaction(s):");
        ui.showMessage(messageList);

        logger.fine("Displayed recent transactions");
    }
}
