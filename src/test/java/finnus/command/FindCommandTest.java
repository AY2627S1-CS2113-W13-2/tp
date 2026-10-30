//@@author nabijonzzz
package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Ui;

class FindCommandTest {
    private final TransactionList transactions = new TransactionList();

    /** Oldest first, so the recent-list indices are: 4 Allowance, 3 Lunch, 2 Bus, 1 Lunch with friends. */
    @BeforeEach
    public void setUp() {
        transactions.add(new Transaction(TransactionType.DEPOSIT, Money.of("500"), "Allowance"));
        transactions.add(new Transaction(TransactionType.WITHDRAWAL, Money.of("4.50"), "Lunch", "Food"));
        transactions.add(new Transaction(TransactionType.WITHDRAWAL, Money.of("1.20"), "Bus", "Transport"));
        transactions.add(new Transaction(TransactionType.WITHDRAWAL, Money.of("12"), "Lunch with friends", "Food"));
    }

    @Test
    public void execute_partialKeywordAnyCase_findsMatchesMostRecentFirst() {
        String output = run("LUN");

        assertTrue(output.contains("Found 2 transaction(s) matching \"LUN\":"));
        int newer = output.indexOf("1. [Withdrawal] -$12.00 (Lunch with friends) #Food");
        int older = output.indexOf("3. [Withdrawal] -$4.50 (Lunch) #Food");
        assertTrue(newer >= 0 && older > newer, "Matches should be listed most recent first");
        assertFalse(output.contains("Bus"));
        assertFalse(output.contains("Allowance"));
    }

    @Test
    public void execute_match_showsIndexThatDeleteUses() throws FinNusException {
        assertTrue(run("bus").contains("2. [Withdrawal] -$1.20 (Bus) #Transport"));

        new DeleteCommand(2).execute(transactions, new Ui());

        assertTrue(run("bus").contains("No transactions match \"bus\"."));
        assertEquals(3, transactions.size());
    }

    @Test
    public void execute_phrase_matchesWholePhraseOnly() {
        String output = run("with friends");

        assertTrue(output.contains("Found 1 transaction(s)"));
        assertTrue(output.contains("Lunch with friends"));
    }

    @Test
    public void execute_noMatch_showsMessageAndKeepsTransactions() {
        assertTrue(run("rent").contains("No transactions match \"rent\"."));
        assertEquals(4, transactions.size());
    }

    @Test
    public void execute_emptyList_showsNoMatches() {
        String output = runOn(new TransactionList(), "lunch");

        assertTrue(output.contains("No transactions match \"lunch\"."));
    }

    private String run(String keyword) {
        return runOn(transactions, keyword);
    }

    /** Runs a find command on the given list and returns what it printed. */
    private String runOn(TransactionList list, String keyword) {
        return runCommand(list, new FindCommand(keyword));
    }

    //@@author chancheese
    /** Runs a command on the given list and returns what it printed. */
    private static String runCommand(TransactionList list, FindCommand command) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        try {
            command.execute(list, new Ui());
        } catch (FinNusException e) {
            throw new AssertionError("Unexpected error: " + e.getMessage(), e);
        } finally {
            System.setOut(original);
        }
        return output.toString();
    }
    //@@author nabijonzzz

    @Test
    public void containsIgnoreCase_textWhoseLowercaseChangesLength_stillMatches() {
        // "İ" lowercases to "i" plus a combining dot, which broke toLowerCase().contains().
        assertTrue(FindCommand.containsIgnoreCase("\u0130stanbul trip", "istanbul"));
        assertTrue(FindCommand.containsIgnoreCase("Trip to \u0130stanbul", "ISTANBUL"));
    }

    @Test
    public void containsIgnoreCase_greekFinalSigma_matches() {
        // Capital sigma has two lowercase forms: "σ" and the final form "ς".
        assertTrue(FindCommand.containsIgnoreCase("\u039a\u0391\u03a6\u0395\u03a3", "\u03ba\u03b1\u03c6\u03b5\u03c2"));
        assertTrue(FindCommand.containsIgnoreCase("\u039a\u0391\u03a6\u0395\u03a3", "\u03ba\u03b1\u03c6\u03b5\u03c3"));
    }

    @Test
    public void containsIgnoreCase_keywordLongerThanTextOrAbsent_returnsFalse() {
        assertFalse(FindCommand.containsIgnoreCase("Bus", "Bus fare"));
        assertFalse(FindCommand.containsIgnoreCase("Lunch", "dinner"));
        assertTrue(FindCommand.containsIgnoreCase("Lunch", "LUNCH"));
    }
}
