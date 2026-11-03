package finnus.command;

import java.math.BigDecimal;
import java.time.LocalDate;

//@@author naieel04
/**
 * Changes requested by an edit. Null means retain the old value; an empty description
 * explicitly clears a deposit's description. Target-dependent checks happen at execution.
 *
 * @param amount Replacement amount in the transaction's currency, or null to retain it.
 * @param description Replacement description, or null to retain it.
 * @param categoryName Replacement category name, or null to retain it.
 * @param date Replacement date, or null to retain it.
 * @param rate Replacement historical FX rate, or null to retain it.
 */
public record EditFields(BigDecimal amount, String description, String categoryName, LocalDate date, BigDecimal rate) {
    /** Returns whether at least one field was supplied. */
    public boolean hasChanges() {
        return amount != null || description != null || categoryName != null || date != null || rate != null;
        //@@author
    }
}
