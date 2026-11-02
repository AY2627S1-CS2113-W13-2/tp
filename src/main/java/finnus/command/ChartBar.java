//@@author nkyjStar
package finnus.command;

import java.math.RoundingMode;

import finnus.money.Money;

/** Renders bars on a common scale and places the exact displayed SGD total beside each bar. */
final class ChartBar {
    private static final int WIDTH = 16;

    private ChartBar() {
    }

    /**
     * Scales a positive amount against {@code maximum}; zero has an empty bar.
     *
     * @param amount The SGD total represented by the bar.
     * @param maximum The largest total on the chart; positive when {@code amount} is positive.
     * @return A fixed-width bar followed by the exact formatted SGD total.
     */
    static String render(Money amount, Money maximum) {
        int length = 0;
        if (amount.isPositive()) {
            length = Math.max(1, amount.toBigDecimal().multiply(java.math.BigDecimal.valueOf(WIDTH))
                    .divide(maximum.toBigDecimal(), 0, RoundingMode.DOWN).intValue());
        }
        return String.format("%-" + WIDTH + "s %s", "#".repeat(length), amount.format());
    }
}
//@@author
