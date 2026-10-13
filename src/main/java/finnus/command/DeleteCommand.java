package finnus.command;

import finnus.transaction.TransactionList;
import finnus.transaction.Transaction;
import finnus.ui.Ui;
import finnus.exception.FinNusException;

/**
 * Deletes a transaction from the transaction list.
 */
public class DeleteCommand extends Command {
    private final int index;

    /**
     * Creates a DeleteCommand with the specified index of the transaction to delete.
     *
     * @param index The recent list based index of the transaction to delete.
     */
    public DeleteCommand(int index) {
        this.index = index;
    }

    /**
     * Executes the delete command by removing the transaction at the specified index
     * from the transaction list and displaying a confirmation message.
     *
     * @param transactions The transaction list to delete from.
     * @param ui The user interface to display messages.
     * @throws FinNusException If the index is invalid.
     */
    @Override
    public void execute(TransactionList transactions, Ui ui) throws FinNusException {
        if (index < 1 || index > transactions.size()) {
            throw new FinNusException("Invalid transaction index.");
        }
        // Convert recent list based index to 0-based index for internal list operations
        Transaction deletedTransaction = transactions.delete(transactions.size() - index);
        ui.showMessage("Transaction deleted: " + deletedTransaction);
    }
}
