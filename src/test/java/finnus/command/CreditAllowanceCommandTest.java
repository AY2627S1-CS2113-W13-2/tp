//@@author chancheese
package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import finnus.allowance.Allowance;
import finnus.command.AllowanceCommandTest.RecordingUi;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;

class CreditAllowanceCommandTest {
    private final TransactionList transactions = new TransactionList();
    private final RecordingUi ui = new RecordingUi();

    @Test
    public void execute_allowanceDayArrived_addsDatedDeposit() {
        transactions.setAllowance(new Allowance(5, new BigDecimal("500"), null, LocalDate.of(2026, 10, 5)));

        new CreditAllowanceCommand(LocalDate.of(2026, 11, 5), true).execute(transactions, ui);

        assertEquals(1, transactions.size());
        Transaction deposit = transactions.get(0);
        assertEquals(TransactionType.DEPOSIT, deposit.getType());
        assertEquals(Money.of("500"), deposit.getAmount());
        assertEquals("Allowance", deposit.getDescription());
        assertEquals(Optional.of(LocalDate.of(2026, 11, 5)), deposit.getDate());
        assertEquals(LocalDate.of(2026, 11, 5), transactions.getAllowance().orElseThrow().lastCredited());
        assertEquals("Allowance added: [5 Nov 2026] [Deposit] +$500.00 (Allowance)", ui.output());
    }

    @Test
    public void execute_beforeAllowanceDay_addsNothing() {
        transactions.setAllowance(new Allowance(5, new BigDecimal("500"), null, LocalDate.of(2026, 10, 5)));

        new CreditAllowanceCommand(LocalDate.of(2026, 11, 4), true).execute(transactions, ui);

        assertEquals(0, transactions.size());
        assertEquals("", ui.output());
    }

    @Test
    public void execute_appClosedForMonths_addsEachMissedMonthOnItsOwnDate() {
        transactions.setAllowance(new Allowance(31, new BigDecimal("500"), null, LocalDate.of(2026, 12, 31)));

        new CreditAllowanceCommand(LocalDate.of(2027, 3, 31), true).execute(transactions, ui);

        assertEquals(3, transactions.size());
        assertEquals(Optional.of(LocalDate.of(2027, 1, 31)), transactions.get(0).getDate());
        assertEquals(Optional.of(LocalDate.of(2027, 2, 28)), transactions.get(1).getDate());
        assertEquals(Optional.of(LocalDate.of(2027, 3, 31)), transactions.get(2).getDate());
    }

    @Test
    public void execute_runTwice_doesNotAddTheSameAllowanceAgain() {
        transactions.setAllowance(new Allowance(5, new BigDecimal("500"), null, LocalDate.of(2026, 10, 5)));
        LocalDate today = LocalDate.of(2026, 11, 5);

        new CreditAllowanceCommand(today, true).execute(transactions, ui);
        // The user deletes the automatic deposit; it must not come back.
        transactions.delete(0);
        new CreditAllowanceCommand(today, true).execute(transactions, ui);

        assertEquals(0, transactions.size());
    }

    @Test
    public void execute_dayOnly_addsNothing() {
        transactions.setAllowance(new Allowance(5, null, null, LocalDate.of(2026, 10, 5)));

        new CreditAllowanceCommand(LocalDate.of(2026, 12, 5), true).execute(transactions, ui);

        assertEquals(0, transactions.size());
    }

    @Test
    public void execute_foreignWithRate_convertsAtCurrentRate() {
        transactions.getRates().setRate("JPY", new BigDecimal("120"));
        transactions.setAllowance(new Allowance(5, new BigDecimal("60000"), "JPY", LocalDate.of(2026, 10, 5)));

        new CreditAllowanceCommand(LocalDate.of(2026, 11, 5), true).execute(transactions, ui);

        assertEquals(Money.of("500.00"), transactions.get(0).getAmount());
        assertTrue(ui.output().contains("[Deposit] +$500.00 (Allowance) [JPY 60000]"));
        assertTrue(ui.output().contains("Converted at 1 SGD = 120 JPY."));
    }

    @Test
    public void execute_foreignWithoutRate_waitsUntilRateIsSet() {
        transactions.setAllowance(new Allowance(5, new BigDecimal("60000"), "JPY", LocalDate.of(2026, 10, 5)));
        LocalDate today = LocalDate.of(2026, 11, 6);

        new CreditAllowanceCommand(today, true).execute(transactions, ui);

        assertEquals(0, transactions.size());
        assertEquals(LocalDate.of(2026, 10, 5), transactions.getAllowance().orElseThrow().lastCredited());
        assertTrue(ui.output().startsWith("Your allowance for 5 Nov 2026 was not added yet. "
                + "No exchange rate set for JPY."));

        transactions.getRates().setRate("JPY", new BigDecimal("120"));
        new CreditAllowanceCommand(today, false).execute(transactions, ui);

        assertEquals(1, transactions.size());
        assertEquals(Optional.of(LocalDate.of(2026, 11, 5)), transactions.get(0).getDate());
    }

    @Test
    public void execute_problemWithoutWarnings_staysQuiet() {
        transactions.setAllowance(new Allowance(5, new BigDecimal("60000"), "JPY", LocalDate.of(2026, 10, 5)));

        new CreditAllowanceCommand(LocalDate.of(2026, 11, 6), false).execute(transactions, ui);

        assertEquals("", ui.output());
    }
}
//@@author
