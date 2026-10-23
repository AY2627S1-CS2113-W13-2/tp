package finnus.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks command lookup and the metadata required by parsing and help output.
 */
class CommandWordTest {
    @ParameterizedTest
    @EnumSource(value = CommandWord.class, names = "OPENING", mode = EnumSource.Mode.EXCLUDE)
    public void fromWord_registeredWord_returnsCommand(CommandWord command) {
        assertEquals(Optional.of(command), CommandWord.fromWord(command.getWord()));
        assertEquals(Optional.of(command), CommandWord.fromWord(command.getWord().toUpperCase(Locale.ROOT)));
    }

    @Test
    public void fromWord_openingDuringNormalUse_returnsEmpty() {
        assertTrue(CommandWord.fromWord("opening").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "unknown", "depositt", "help 123", " help", "help "})
    public void fromWord_unknownWord_returnsEmpty(String word) {
        assertTrue(CommandWord.fromWord(word).isEmpty());
    }

    @Test
    @ResourceLock("java.util.Locale")
    public void fromWord_turkishLocale_returnsCommand() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(Optional.of(CommandWord.EXIT), CommandWord.fromWord("EXIT"));
            assertEquals(Optional.of(CommandWord.WITHDRAW), CommandWord.fromWord("WITHDRAW"));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void metadata_registeredCommands_haveUniqueWordsAndCompleteMetadata() {
        Set<String> words = new HashSet<>();
        for (CommandWord command : CommandWord.values()) {
            assertTrue(command.getWord().matches("[a-z]+"), "Command words must use lowercase letters");
            assertTrue(words.add(command.getWord()), "Duplicate command word: " + command.getWord());
            assertFalse(command.getDescription().isBlank(), "Missing description for " + command);
            assertFalse(command.getUsage().isBlank(), "Missing usage for " + command);
            assertEquals(command.getUsage().trim(), command.getUsage(), "Usage must not have surrounding spaces");
        }
    }

    @Test
    public void getUsage_commandsWithAndWithoutArguments_returnsFullSyntax() {
        assertEquals("deposit AMOUNT [d/DESCRIPTION]", CommandWord.DEPOSIT.getUsage());
        assertEquals("help", CommandWord.HELP.getUsage());
    }
}
