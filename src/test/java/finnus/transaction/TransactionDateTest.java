//@@author nabijonzzz
package finnus.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import finnus.money.Money;

class TransactionDateTest {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 5);

    @Test
    public void toString_withDate_showsDateFirst() {
        Transaction withdrawal = new Transaction(TransactionType.WITHDRAWAL, Money.of("4.50"), "Lunch", "Food", DATE);
        Transaction deposit = new Transaction(TransactionType.DEPOSIT, Money.of("500"), "", null, DATE);

        assertEquals("[5 Oct 2026] [Withdrawal] -$4.50 (Lunch) #Food", withdrawal.toString());
        assertEquals("[5 Oct 2026] [Deposit] +$500.00", deposit.toString());
    }

    @Test
    public void toString_withoutDate_showsNoDate() {
        Transaction transaction = new Transaction(TransactionType.WITHDRAWAL, Money.of("4.50"), "Lunch", "Food");

        assertEquals(Optional.empty(), transaction.getDate());
        assertEquals("[Withdrawal] -$4.50 (Lunch) #Food", transaction.toString());
    }

    @Test
    @ResourceLock("java.util.Locale")
    public void toString_otherDefaultLocale_stillUsesEnglishMonth() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.FRANCE);
            Transaction transaction = new Transaction(TransactionType.DEPOSIT, Money.of("1"), "", null, DATE);
            assertEquals("[5 Oct 2026] [Deposit] +$1.00", transaction.toString());
        } finally {
            Locale.setDefault(original);
        }
    }
}
