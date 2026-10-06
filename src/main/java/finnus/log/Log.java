package finnus.log;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a log of the transactions made
 */
public class Log {

    private ArrayList<Transaction> transactions;

    /**
     * Returns a sublist of transactions, with length equal to given length
     */
    public List<Transaction> getRecentTransactions(int length) {
        // Safeguard against lists of size smaller than length
        int fromIndex = Math.max(0, transactions.size() - 5);

        return transactions.subList(fromIndex, transactions.size());
    }

    /**
     * Prints the 5 most recent transactions
     */
    public void printRecentTransactions() {
        List<Transaction> recentTransactions = getRecentTransactions(5);

        // Print each each transaction
        for (int i = 0; i < recentTransactions.size(); i++) {
            Transaction t = recentTransactions.get(i);

            System.out.println(i + ": ");
            System.out.println("Transaction type: " + t.getTransactionType());
            System.out.println("Amount: " + t.getAmount());
            System.out.println("Description: " + t.getDescription());
            System.out.println();
        }
    }

    /**
     * Prints a given number of recent transactions
     */
    public void printRecentTransactions(int length) {
        List<Transaction> recentTransactions = getRecentTransactions(length);

        // Print each transaction
        for (int i = 0; i < recentTransactions.size(); i++) {
            Transaction t = recentTransactions.get(i);

            System.out.println(i + ": ");
            System.out.println("Transaction type: " + t.getTransactionType());
            System.out.println("Amount: " + t.getAmount());
            System.out.println("Description: " + t.getDescription());
            System.out.println();
        }
    }
}
