package finnus.currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

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
}
