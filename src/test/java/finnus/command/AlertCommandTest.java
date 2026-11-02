//@@author lololol8484
package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import finnus.alert.SpendingAlert;
import finnus.money.Money;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/** Checks display and updates of the optional spending-alert settings. */
class AlertCommandTest {
    @Test
    public void execute_show_listsUnsetSettingsAndAutomaticAlerts() {
        TransactionList ledger = new TransactionList();

        String output = run(new AlertCommand(), ledger);

        assertTrue(output.contains("Balance threshold: not set"));
        assertTrue(output.contains("Allowance threshold: not set"));
        assertTrue(output.contains("full allowance, opening balance and $0.00 remain active"));
    }

    @Test
    public void execute_setBalance_preservesAllowancePercentage() {
        TransactionList ledger = new TransactionList();
        ledger.setSpendingAlert(new SpendingAlert(null, new BigDecimal("90")));

        String output = run(AlertCommand.setBalance(Money.of("75")), ledger);

        assertEquals(Money.of("75"), ledger.getSpendingAlert().balanceThreshold().orElseThrow());
        assertEquals(new BigDecimal("90"), ledger.getSpendingAlert().allowancePercentage().orElseThrow());
        assertTrue(output.contains("Balance alert threshold set to $75.00."));
    }

    @Test
    public void execute_clearBalance_preservesAllowancePercentage() {
        TransactionList ledger = new TransactionList();
        ledger.setSpendingAlert(new SpendingAlert(Money.of("75"), new BigDecimal("90")));

        String output = run(AlertCommand.clearBalance(), ledger);

        assertTrue(ledger.getSpendingAlert().balanceThreshold().isEmpty());
        assertEquals(new BigDecimal("90"), ledger.getSpendingAlert().allowancePercentage().orElseThrow());
        assertTrue(output.contains("Balance alert threshold cleared."));
    }

    @Test
    public void execute_setAllowancePercentage_withoutAllowance() {
        TransactionList ledger = new TransactionList();
        ledger.setSpendingAlert(new SpendingAlert(Money.of("75"), null));

        String output = run(AlertCommand.setAllowancePercentage(new BigDecimal("90")), ledger);

        assertTrue(ledger.getAllowance().isEmpty());
        assertEquals(Money.of("75"), ledger.getSpendingAlert().balanceThreshold().orElseThrow());
        assertEquals(new BigDecimal("90"), ledger.getSpendingAlert().allowancePercentage().orElseThrow());
        assertTrue(output.contains("Allowance alert percentage set to 90%."));
    }

    @Test
    public void execute_clearAllowancePercentage_preservesBalanceThreshold() {
        TransactionList ledger = new TransactionList();
        ledger.setSpendingAlert(new SpendingAlert(Money.of("75"), new BigDecimal("90")));

        String output = run(AlertCommand.clearAllowancePercentage(), ledger);

        assertEquals(Money.of("75"), ledger.getSpendingAlert().balanceThreshold().orElseThrow());
        assertTrue(ledger.getSpendingAlert().allowancePercentage().isEmpty());
        assertTrue(output.contains("Allowance alert percentage cleared."));
    }

    private static String run(AlertCommand command, TransactionList ledger) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        try {
            command.execute(ledger, new Ui());
        } finally {
            System.setOut(original);
        }
        return output.toString();
    }
}
//@@author
