//@@author chancheese
package finnus.currency;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import finnus.money.Money;

class ForeignAmountTest {
    @Test
    public void toSgd_exactDivision_returnsSgdValue() {
        ForeignAmount amount = new ForeignAmount("JPY", new BigDecimal("1100"), new BigDecimal("110"));
        assertEquals(Money.of("10.00"), amount.toSgd());
    }

    @Test
    public void toSgd_inexactDivision_roundsToNearestCent() {
        // 1000 / 110 = 9.0909..., which rounds down to 9.09
        assertEquals(Money.of("9.09"),
                new ForeignAmount("JPY", new BigDecimal("1000"), new BigDecimal("110")).toSgd());
        // 0.005 rounds up: 1 / 200 = 0.005 -> 0.01
        assertEquals(Money.of("0.01"),
                new ForeignAmount("JPY", new BigDecimal("1"), new BigDecimal("200")).toSgd());
    }

    @Test
    public void toSgd_tinyAmount_roundsToZero() {
        assertEquals(Money.ZERO, new ForeignAmount("IDR", new BigDecimal("1"), new BigDecimal("12000")).toSgd());
    }

    @Test
    public void toString_showsCodeAndAmountAsTyped() {
        assertEquals("JPY 1100.50",
                new ForeignAmount("JPY", new BigDecimal("1100.50"), new BigDecimal("110")).toString());
    }
}
