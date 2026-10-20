package finnus.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises real filesystem writes and deterministic failures in an isolated directory.
 */
class FileStorageTest {
    @TempDir
    Path directory;

    @Test
    public void save_missingDirectory_createsFileAndRotatesBackup() throws IOException {
        Path path = directory.resolve("nested/finnus.txt");
        FileStorage storage = new FileStorage(path);
        storage.save(LedgerSnapshot.empty());
        String previous = Files.readString(path);
        storage.save(withOpening("10.00"));
        assertEquals(previous, Files.readString(path.resolveSibling("finnus.txt.bak")));
        assertTrue(Files.readString(path).contains("OPENING|10.00"));
        assertNoTemporaryFiles(path.getParent());
    }

    @Test
    public void save_replacementFailure_preservesPreviousFile() throws IOException {
        Path path = directory.resolve("finnus.txt");
        boolean[] fail = {false};
        FileStorage storage = new FileStorage(path, (source, target) -> {
            if (fail[0] && target.equals(path)) {
                throw new IOException("Injected replacement failure");
            }
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        });
        storage.save(withOpening("10"));
        String original = Files.readString(path);
        fail[0] = true;
        assertThrows(IOException.class, () -> storage.save(withOpening("20")));
        assertEquals(original, Files.readString(path));
        assertEquals(original, Files.readString(directory.resolve("finnus.txt.bak")));
        assertNoTemporaryFiles(directory);
        fail[0] = false;
        storage.save(withOpening("20"));
        assertTrue(Files.readString(path).contains("OPENING|20"));
    }

    @Test
    public void save_backupFailure_preservesMainAndOldBackup() throws IOException {
        Path path = directory.resolve("finnus.txt");
        boolean[] fail = {false};
        FileStorage storage = new FileStorage(path, (source, target) -> {
            if (fail[0] && target.toString().endsWith(".bak")) {
                throw new IOException("Injected backup failure");
            }
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        });
        storage.save(withOpening("1"));
        storage.save(withOpening("2"));
        String main = Files.readString(path);
        String backup = Files.readString(directory.resolve("finnus.txt.bak"));
        fail[0] = true;
        assertThrows(IOException.class, () -> storage.save(withOpening("3")));
        assertEquals(main, Files.readString(path));
        assertEquals(backup, Files.readString(directory.resolve("finnus.txt.bak")));
        assertNoTemporaryFiles(directory);
    }

    @Test
    public void save_atomicMoveUnavailable_doesNotCreateMainFile() {
        Path path = directory.resolve("finnus.txt");
        FileStorage storage = new FileStorage(path, (source, target) -> {
            throw new AtomicMoveNotSupportedException(source.toString(), target.toString(), "Injected failure");
        });
        assertThrows(IOException.class, () -> storage.save(LedgerSnapshot.empty()));
        assertFalse(Files.exists(path));
    }

    @Test
    public void save_parentIsFile_reportsWriteFailure() throws IOException {
        Path parent = Files.writeString(directory.resolve("not-a-folder"), "keep");
        assertThrows(IOException.class,
                () -> new FileStorage(parent.resolve("finnus.txt")).save(LedgerSnapshot.empty()));
        assertEquals("keep", Files.readString(parent));
    }

    @Test
    public void resolvePath_packagedAndDevelopment_usesExpectedFolders() {
        Path jar = directory.resolve("app folder/finnus.jar");
        Path working = directory.resolve("unrelated");
        assertEquals(jar.getParent().resolve("finnus.txt"), FileStorage.resolvePath(jar, working));
        assertEquals(working.resolve("finnus.txt"), FileStorage.resolvePath(directory.resolve("classes"), working));
    }

    private LedgerSnapshot withOpening(String amount) {
        return new LedgerSnapshot(new BigDecimal(amount), List.of(), Map.of());
    }

    private void assertNoTemporaryFiles(Path folder) throws IOException {
        try (var files = Files.list(folder)) {
            assertFalse(files.anyMatch(file -> file.getFileName().toString().endsWith(".tmp")));
        }
    }
}
