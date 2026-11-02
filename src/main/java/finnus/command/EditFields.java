package finnus.command;

import java.time.LocalDate;

import finnus.money.Money;

/**
 * Changes requested by an edit. Null means retain the old value; an empty description
 * explicitly clears a deposit's description. Target-dependent checks happen at execution.
 *
 * @param amount Replacement amount in SGD, or null to retain it.
 * @param description Replacement description, or null to retain it.
 * @param categoryName Replacement category name, or null to retain it.
 * @param date Replacement date, or null to retain it.
 */
public record EditFields(Money amount, String description, String categoryName, LocalDate date) {
    /** Returns whether at least one field was supplied. */
    public boolean hasChanges() {
        return amount != null || description != null || categoryName != null || date != null;
    }
}
