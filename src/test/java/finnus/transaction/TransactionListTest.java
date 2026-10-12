package finnus.transaction;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TransactionListTest {

    private static TransactionList transactions;
    private static Transaction newWithdrawal;

    @BeforeAll
    public static void initAll() {
        transactions = new TransactionList();
        transactions.add(new Transaction(TransactionType.DEPOSIT, new BigDecimal(100), ""));
        transactions.add(new Transaction(TransactionType.WITHDRAWAL, new BigDecimal(30), "Food"));
        transactions.add(new Transaction(TransactionType.DEPOSIT, new BigDecimal(30), "January"));

        newWithdrawal = new Transaction(TransactionType.WITHDRAWAL, new BigDecimal(5), "Taxi");
        transactions.add(newWithdrawal);
    }

    @Test
    public void getSubList_startEndProvided_returnsImmutableSublist() {
        List<Transaction> subList = transactions.getSubList(0, transactions.size());
        assertEquals(4, subList.size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> subList.add(new Transaction(TransactionType.WITHDRAWAL, new BigDecimal(30), "Taxi"))
        );
    }

    @ParameterizedTest
    @ValueSource (ints = {4, 5})
    public void getRecentTransactions_lengthMoreThanOrEqualsSize_returnsAllTransactions(int length) {
        List<Transaction> recentTransactions = transactions.getRecentTransactions(length);
        assertEquals(4, recentTransactions.size());
    }

    @Test
    public void getRecentTransactions_lengthEqualsSize_mostRecentTransactionFirst() {
        List<Transaction> recentTransactions = transactions.getRecentTransactions(3);
        assertEquals(
                newWithdrawal,
                recentTransactions.get(0)
        );
    }

    @Test
    public void getRecentTransactions_lengthLessThanSize_returnsSmallerSublist() {
        List<Transaction> recentTransactions = transactions.getRecentTransactions(2);
        assertEquals(2, recentTransactions.size());
    }

    // The balance tests below build their own list so they do not depend on the shared fixture.

    @Test
    public void getBalance_newList_isZero() {
        assertEquals(0, new TransactionList().getBalance().signum());
    }

    @Test
    public void getBalance_openingPlusTransactions_addsUpCorrectly() {
        TransactionList list = new TransactionList();
        list.setOpeningBalance(new BigDecimal("10.10"));
        list.add(new Transaction(TransactionType.DEPOSIT, new BigDecimal("0.20"), ""));
        list.add(new Transaction(TransactionType.WITHDRAWAL, new BigDecimal("5.05"), "Snack"));

        assertEquals(new BigDecimal("5.25"), list.getBalance());
    }

    @Test
    public void getBalance_spendingMoreThanOpening_canBeNegative() {
        TransactionList list = new TransactionList();
        list.setOpeningBalance(new BigDecimal("5"));
        list.add(new Transaction(TransactionType.WITHDRAWAL, new BigDecimal("8"), "Taxi"));

        assertEquals(new BigDecimal("-3"), list.getBalance());
    }

    @Test
    public void setOpeningBalance_existingTransactions_doesNotAddTransaction() {
        TransactionList list = new TransactionList();
        list.add(new Transaction(TransactionType.DEPOSIT, new BigDecimal("50"), ""));
        list.setOpeningBalance(new BigDecimal("100"));

        assertEquals(1, list.size());
        assertEquals(new BigDecimal("100"), list.getOpeningBalance());
    }

}
