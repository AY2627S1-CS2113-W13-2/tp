package finnus.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import finnus.money.Money;

/**
 * Checks recovery decisions and preservation of damaged files before later saves.
 */
class StorageRecoveryTest {
    @TempDir
    Path directory;

    @Test
    public void load_missingFile_startsEmptyWithoutWriting() {
        Path path = directory.resolve("finnus.txt");
        LoadResult loaded = new FileStorage(path).load();
        assertEquals(LedgerSnapshot.empty(), loaded.snapshot());
        assertTrue(loaded.warnings().isEmpty());
        assertFalse(Files.exists(path));
    }

    @Test
    public void load_partialFile_preservesOriginalBeforeSaving() throws IOException {
        Path path = directory.resolve("finnus.txt");
        String damaged = "FINNUS|1\nOPENING|10\nBAD|row\nTRANSACTION|DEPOSIT|1|\n";
        Files.writeString(path, damaged);
        FileStorage storage = new FileStorage(path);
        LoadResult loaded = storage.load();
        assertEquals(1, loaded.snapshot().transactions().size());
        assertFalse(loaded.warnings().isEmpty());
        assertEquals(damaged, Files.readString(path));
        assertTrue(recoveryFiles().isEmpty());
        storage.save(loaded.snapshot());
        assertEquals(damaged, Files.readString(recoveryFiles().getFirst()));
        assertEquals(LedgerCodec.Status.CLEAN, new LedgerCodec().decode(Files.readString(path)).status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "broken", "FINNUS|1\nBAD|row\n"})
    public void load_whollyCorruptFile_startsFreshWithoutWriting(String damaged) throws IOException {
        Path path = Files.writeString(directory.resolve("finnus.txt"), damaged);
        LoadResult loaded = new FileStorage(path).load();
        assertEquals(LedgerSnapshot.empty(), loaded.snapshot());
        assertTrue(loaded.savingAllowed());
        assertTrue(loaded.warnings().stream().anyMatch(message -> message.contains("exit without changing")));
        assertEquals(damaged, Files.readString(path));
    }

    @Test
    public void load_corruptMain_usesValidBackupWithoutReplacingIt() throws IOException {
        Path path = Files.writeString(directory.resolve("finnus.txt"), "damaged");
        String backup = "FINNUS|1\nOPENING|12.30\n";
        Files.writeString(directory.resolve("finnus.txt.bak"), backup);
        FileStorage storage = new FileStorage(path);
        LoadResult loaded = storage.load();
        assertEquals(Money.of("12.30"), loaded.snapshot().openingBalance());
        assertEquals("damaged", Files.readString(path));
        storage.save(loaded.snapshot());
        assertEquals(backup, Files.readString(directory.resolve("finnus.txt.bak")));
        assertEquals("damaged", Files.readString(recoveryFiles().getFirst()));
    }

    @Test
    public void load_missingMain_usesValidBackup() throws IOException {
        Files.writeString(directory.resolve("finnus.txt.bak"), "FINNUS|1\nOPENING|4\n");
        Path path = directory.resolve("finnus.txt");
        assertEquals(Money.of("4"), new FileStorage(path).load().snapshot().openingBalance());
        assertFalse(Files.exists(path));
    }

    @Test
    public void load_invalidUtf8_preservesOriginalBytes() throws IOException {
        Path path = directory.resolve("finnus.txt");
        byte[] invalid = {(byte) 0xff, (byte) 0xfe};
        Files.write(path, invalid);
        FileStorage storage = new FileStorage(path);
        assertEquals(LedgerSnapshot.empty(), storage.load().snapshot());
        storage.save(LedgerSnapshot.empty());
        assertArrayEquals(invalid, Files.readAllBytes(recoveryFiles().getFirst()));
    }

    @Test
    public void load_unsupportedVersion_disablesSaving() throws IOException {
        Path path = Files.writeString(directory.resolve("finnus.txt"), "FINNUS|10\nOPENING|10\n");
        FileStorage storage = new FileStorage(path);
        assertFalse(storage.load().savingAllowed());
        assertThrows(IOException.class, () -> storage.save(LedgerSnapshot.empty()));
        assertTrue(Files.readString(path).startsWith("FINNUS|10"));
    }

    @Test
    public void load_onlyNewerBackup_disablesSaving() throws IOException {
        Path path = directory.resolve("finnus.txt");
        String newer = "FINNUS|10\nOPENING|10\n";
        Files.writeString(directory.resolve("finnus.txt.bak"), newer);
        FileStorage storage = new FileStorage(path);
        assertFalse(storage.load().savingAllowed());
        assertThrows(IOException.class, () -> storage.save(LedgerSnapshot.empty()));
        assertFalse(Files.exists(path));
        assertEquals(newer, Files.readString(directory.resolve("finnus.txt.bak")));
    }

    @Test
    public void load_unreadablePath_disablesSavingInsteadOfQuarantining() throws IOException {
        Path path = Files.createDirectory(directory.resolve("finnus.txt"));
        FileStorage storage = new FileStorage(path);
        assertFalse(storage.load().savingAllowed());
        assertThrows(IOException.class, () -> storage.save(LedgerSnapshot.empty()));
        assertTrue(Files.isDirectory(path));
        assertTrue(recoveryFiles().isEmpty());
    }

    @Test
    public void save_preservationFailure_keepsDamagedFile() throws IOException {
        Path path = Files.writeString(directory.resolve("finnus.txt"), "damaged");
        FileStorage storage = new FileStorage(path, (source, target) -> {
            throw new IOException("Injected preservation failure");
        });
        storage.load();
        assertThrows(IOException.class, () -> storage.save(LedgerSnapshot.empty()));
        assertEquals("damaged", Files.readString(path));
        assertTrue(recoveryFiles().isEmpty());
    }

    @Test
    public void save_corruptBackup_preservesItBeforeRotation() throws IOException {
        Path path = Files.writeString(directory.resolve("finnus.txt"), "FINNUS|1\nOPENING|2\n");
        Files.writeString(directory.resolve("finnus.txt.bak"), "broken backup");
        FileStorage storage = new FileStorage(path);
        storage.load();
        storage.save(LedgerSnapshot.empty());
        assertEquals("broken backup", Files.readString(recoveryFiles().getFirst()));
        assertTrue(Files.readString(directory.resolve("finnus.txt.bak")).contains("OPENING|2"));
    }

    private List<Path> recoveryFiles() throws IOException {
        try (var files = Files.list(directory)) {
            return files.filter(file -> file.getFileName().toString().contains(".corrupt-")).toList();
        }
    }
}
