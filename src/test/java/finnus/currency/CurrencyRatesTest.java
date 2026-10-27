package finnus.currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import finnus.exception.FinNusException;
import finnus.money.Money;

class CurrencyRatesTest {
    @Test
    public void hasRate_newRates_isFalse() {
        assertFalse(new CurrencyRates().hasRate("JPY"));
    }

    @Test
    public void setRate_newCurrency_canBeRead() {
        CurrencyRates rates = new CurrencyRates();
        rates.setRate("JPY", new BigDecimal("110"));

        assertTrue(rates.hasRate("JPY"));
        assertEquals(new BigDecimal("110"), rates.getRate("JPY"));
    }

    @Test
    public void setRate_sameCurrencyTwice_replacesOldRate() {
        CurrencyRates rates = new CurrencyRates();
        rates.setRate("JPY", new BigDecimal("110"));
        rates.setRate("JPY", new BigDecimal("105.5"));

        assertEquals(new BigDecimal("105.5"), rates.getRate("JPY"));
        assertEquals(1, rates.getAll().size());
    }

    @Test
    public void getAll_severalCurrencies_sortedByCode() {
        CurrencyRates rates = new CurrencyRates();
        rates.setRate("USD", new BigDecimal("0.74"));
        rates.setRate("JPY", new BigDecimal("110"));
        rates.setRate("MYR", new BigDecimal("3.45"));

        assertEquals(List.of("JPY", "MYR", "USD"), List.copyOf(rates.getAll().keySet()));
    }

    @Test
    public void getAll_modified_throwsException() {
        CurrencyRates rates = new CurrencyRates();
        assertThrows(UnsupportedOperationException.class,
                () -> rates.getAll().put("JPY", BigDecimal.ONE));
    }

    //@@author chancheese
    @Test
    public void convert_rateSet_returnsAmountWithRateUsed() throws FinNusException {
        CurrencyRates rates = new CurrencyRates();
        rates.setRate("JPY", new BigDecimal("110"));

        ForeignAmount converted = rates.convert(new BigDecimal("1100"), "JPY");

        assertEquals(new ForeignAmount("JPY", new BigDecimal("1100"), new BigDecimal("110")), converted);
        assertEquals(Money.of("10"), converted.toSgd());
    }

    @Test
    public void convert_noRateSet_throwsExceptionNamingCurrency() {
        FinNusException e = assertThrows(FinNusException.class,
                () -> new CurrencyRates().convert(new BigDecimal("10"), "USD"));
        assertTrue(e.getMessage().startsWith("No exchange rate set for USD."));
    }

    @Test
    public void convert_amountBelowOneCent_throwsException() {
        CurrencyRates rates = new CurrencyRates();
        rates.setRate("IDR", new BigDecimal("12000"));
        assertThrows(FinNusException.class, () -> rates.convert(new BigDecimal("1"), "IDR"));
    }

    @Test
    public void convert_sgdValueAboveMaximum_throwsException() throws FinNusException {
        CurrencyRates rates = new CurrencyRates();
        rates.setRate("JPY", new BigDecimal("100"));

        assertEquals(Money.MAX_AMOUNT, rates.convert(new BigDecimal("100000000"), "JPY").toSgd());
        assertThrows(FinNusException.class, () -> rates.convert(new BigDecimal("100000001"), "JPY"));
    }
}
