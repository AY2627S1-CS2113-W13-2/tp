//@@author nabijonzzz
package finnus.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import finnus.exception.FinNusException;

class CategoryListTest {
    private final CategoryList categories = new CategoryList();

    @Test
    public void getAll_newList_containsOnlyPredefinedCategories() {
        assertEquals(CategoryList.PREDEFINED, categories.getAll());
        assertTrue(categories.getCustom().isEmpty());
    }

    @Test
    public void find_differentCase_returnsOriginalSpelling() {
        assertEquals(Optional.of("Food"), categories.find("FOOD"));
        assertEquals(Optional.of("Food"), categories.find("food"));
        assertEquals(Optional.empty(), categories.find("Fod"));
        assertEquals(Optional.empty(), categories.find(null));
    }

    @Test
    public void add_customCategory_isFoundIgnoringCaseAndListedAfterPredefined() {
        categories.add("Gym");

        assertEquals(Optional.of("Gym"), categories.find("gYM"));
        assertEquals(List.of("Gym"), categories.getCustom());
        assertEquals("Gym", categories.getAll().getLast());
    }

    @Test
    public void resolve_noCategory_returnsDefault() throws FinNusException {
        assertEquals(CategoryList.DEFAULT_CATEGORY, categories.resolve(null));
    }

    @Test
    public void resolve_knownCategory_returnsOriginalSpelling() throws FinNusException {
        assertEquals("Transport", categories.resolve("transport"));
    }

    @Test
    public void resolve_unknownCategory_throwsExceptionListingValidOnes() {
        FinNusException e = assertThrows(FinNusException.class, () -> categories.resolve("fod"));
        assertTrue(e.getMessage().contains("Unknown category 'fod'"));
        assertTrue(e.getMessage().contains("Food, Transport"));
        assertTrue(e.getMessage().contains("category NAME"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Gym", "bubble-tea", "K-pop2", "a", "abcdefghijklmnopqrst"})
    public void isValidName_validNames_returnsTrue(String name) {
        assertTrue(CategoryList.isValidName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "two words", "a/b", "a|b", "café", "abcdefghijklmnopqrstu", "#food"})
    public void isValidName_invalidNames_returnsFalse(String name) {
        assertFalse(CategoryList.isValidName(name));
    }
}
