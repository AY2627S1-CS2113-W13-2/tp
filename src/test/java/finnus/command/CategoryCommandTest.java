//@@author nabijonzzz
package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.exception.FinNusException;
import finnus.money.Money;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

class CategoryCommandTest {
    private final TransactionList transactions = new TransactionList();

    @Test
    public void execute_add_addsCustomCategory() throws FinNusException {
        new CategoryCommand("Gym").execute(transactions, new Ui());

        assertEquals(List.of("Gym"), transactions.getCategories().getCustom());
    }

    @ParameterizedTest
    @ValueSource(strings = {"FOOD", "others", "gym"})
    public void execute_addExistingNameIgnoringCase_throwsException(String name) throws FinNusException {
        new CategoryCommand("Gym").execute(transactions, new Ui());

        FinNusException e = assertThrows(FinNusException.class,
                () -> new CategoryCommand(name).execute(transactions, new Ui()));
        assertTrue(e.getMessage().contains("already exists"));
        assertEquals(List.of("Gym"), transactions.getCategories().getCustom());
    }

    @Test
    public void execute_list_showsPredefinedAndCustomCategories() throws FinNusException {
        String emptyListOutput = captureOutput(new CategoryCommand());
        assertTrue(emptyListOutput.contains("Custom: none yet"));
        assertTrue(emptyListOutput.contains("category NAME"));

        new CategoryCommand("Gym").execute(transactions, new Ui());
        String output = captureOutput(new CategoryCommand());

        assertTrue(output.contains("Predefined: Food, Transport, Housing"));
        assertTrue(output.contains("Custom: Gym"));
    }

    @Test
    public void execute_withdrawWithCustomCategory_recordsCategory() throws FinNusException {
        new CategoryCommand("Gym").execute(transactions, new Ui());
        new WithdrawCommand(Money.of("30"), "Membership", "GYM").execute(transactions, new Ui());

        assertEquals(Optional.of("Gym"), transactions.get(0).getCategory());
    }

    /** Runs a command and returns what it printed. */
    private String captureOutput(Command command) throws FinNusException {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        try {
            command.execute(transactions, new Ui());
        } finally {
            System.setOut(original);
        }
        return output.toString();
    }
}
