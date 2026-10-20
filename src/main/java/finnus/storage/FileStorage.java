package finnus.storage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

/**
 * Saves ledger snapshots beside the JAR using atomic replacement and a previous-state backup.
 */
public class FileStorage {
    private static final Logger LOGGER = Logger.getLogger(FileStorage.class.getName());
    private static final String FILE_NAME = "finnus.txt";

    private final Path path;
    private final LedgerCodec codec = new LedgerCodec();
    private final AtomicReplacement replacement;
    private boolean knownClean;

    /**
     * Creates storage for the supplied file; useful for isolated tests and alternate launchers.
     */
    public FileStorage(Path path) {
        this(path, (source, target) -> Files.move(source, target,
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING));
    }

    FileStorage(Path path, AtomicReplacement replacement) {
        this.path = path.toAbsolutePath().normalize();
        this.replacement = replacement;
    }

    /**
     * Returns the save location beside a packaged JAR, or in the working directory during development.
     */
    public static Path defaultPath() {
        try {
            Path location = Path.of(FileStorage.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return resolvePath(location, Path.of(""));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot determine the application location", e);
        }
    }

    static Path resolvePath(Path codeLocation, Path workingDirectory) {
        Path location = codeLocation.toAbsolutePath().normalize();
        Path folder = location.getFileName().toString().endsWith(".jar")
                ? location.getParent() : workingDirectory.toAbsolutePath().normalize();
        return folder.resolve(FILE_NAME);
    }

    /**
     * Returns the absolute save path for startup diagnostics.
     */
    public Path getPath() {
        return path;
    }

    /**
     * Writes a complete snapshot without truncating the main file or its backup.
     *
     * @throws IOException If writing, backup rotation or atomic replacement fails.
     */
    public void save(LedgerSnapshot snapshot) throws IOException {
        if (Files.exists(path) && !knownClean) {
            throw new IOException("Existing data must be loaded before it can be replaced");
        }
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), ".finnus-", ".tmp");
        try {
            Files.writeString(temporary, codec.encode(snapshot), StandardCharsets.UTF_8);
            if (knownClean && Files.exists(path)) {
                rotateBackup();
            }
            replacement.replace(temporary, path);
            knownClean = true;
            LOGGER.fine("Saved ledger snapshot");
        } finally {
            removeTemporary(temporary);
        }
    }

    private void rotateBackup() throws IOException {
        Path temporary = Files.createTempFile(path.getParent(), ".finnus-", ".tmp");
        try {
            Files.copy(path, temporary, StandardCopyOption.REPLACE_EXISTING);
            replacement.replace(temporary, backupPath());
        } finally {
            removeTemporary(temporary);
        }
    }

    private Path backupPath() {
        return path.resolveSibling(path.getFileName() + ".bak");
    }

    private void removeTemporary(Path temporary) {
        try {
            Files.deleteIfExists(temporary);
        } catch (IOException e) {
            LOGGER.fine("Could not remove a temporary storage file");
        }
    }

    /**
     * Provides a narrow failure-injection seam without depending on platform permission behavior.
     */
    @FunctionalInterface
    interface AtomicReplacement {
        void replace(Path source, Path target) throws IOException;
    }
}
