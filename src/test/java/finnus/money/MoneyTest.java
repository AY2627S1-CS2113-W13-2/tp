package finnus.money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class MoneyTest {
    @Test
    public void add_decimalAmounts_retainsExactValue() {
        Money total = Money.ZERO;
        for (int i = 0; i < 10; i++) {
            total = total.add(Money.of("0.10"));
        }

        assertEquals(Money.of("1.00"), total);
    }

    @Test
    public void negate_positiveAmount_returnsNegativeAmountWithoutChangingOriginal() {
        Money original = Money.of("12.30");

        assertEquals(Money.of("-12.30"), original.negate());
        assertEquals(Money.of("12.30"), original);
    }

    @Test
    public void format_positiveNegativeAndZero_usesConsistentCurrencyFormat() {
        assertEquals("$12.30", Money.of("12.3").format());
        assertEquals("-$12.30", Money.of("-12.3").format());
        assertEquals("$0.00", Money.ZERO.format());
    }

    @Test
    public void format_halfCent_usesHalfUpRoundingOnlyForDisplay() {
        Money amount = Money.of("10.005");

        assertEquals("$10.01", amount.format());
        assertEquals("10.005", amount.toPlainString());
    }

    @Test
    public void formatWithSign_positiveAndNegative_haveExplicitSigns() {
        assertEquals("+$4.50", Money.of("4.50").formatWithSign());
        assertEquals("-$4.50", Money.of("-4.50").formatWithSign());
    }

    @Test
    public void signQueries_positiveNegativeAndZero_returnExpectedResults() {
        assertTrue(Money.of("1").isPositive());
        assertTrue(Money.of("-1").isNegative());
        assertTrue(Money.ZERO.isZero());
        assertFalse(Money.ZERO.isPositive());
        assertFalse(Money.ZERO.isNegative());
    }

    @Test
    public void equals_differentDecimalScales_areEqualMoneyValues() {
        assertEquals(Money.of("10"), Money.of("10.00"));
        assertEquals(Money.of("10").hashCode(), Money.of("10.00").hashCode());
    }
}
