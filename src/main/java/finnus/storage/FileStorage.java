package finnus.storage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Saves ledger snapshots beside the JAR using atomic replacement and a previous-state backup.
 */
public class FileStorage implements Storage {
    private static final Logger LOGGER = Logger.getLogger(FileStorage.class.getName());
    private static final String FILE_NAME = "finnus.txt";

    private final Path path;
    private final LedgerCodec codec = new LedgerCodec();
    private final AtomicReplacement replacement;
    private boolean knownClean;
    private boolean initialized;
    private boolean savingAllowed = true;
    private boolean preserveOriginal;
    private final Set<Path> preservedSources = new HashSet<>();

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
     * Loads valid rows, using a fully valid backup only when the main file cannot be recovered.
     * Damaged files remain untouched until the user makes a change and a save is attempted.
     */
    @Override
    public LoadResult load() {
        initialized = true;
        knownClean = false;
        preserveOriginal = false;
        savingAllowed = true;
        preservedSources.clear();
        try {
            LedgerCodec.DecodeResult decoded = read(path);
            if (decoded == null) {
                return recoverBackup(false, new ArrayList<>());
            }
            return handleMainFile(decoded);
        } catch (CharacterCodingException e) {
            preserveOriginal = true;
            return recoverBackup(true, new ArrayList<>(List.of("Save file contains invalid UTF-8.")));
        } catch (IOException e) {
            return readFailure(path);
        }
    }

    private LoadResult handleMainFile(LedgerCodec.DecodeResult decoded) {
        List<String> warnings = new ArrayList<>(decoded.warnings());
        switch (decoded.status()) {
        case CLEAN:
            knownClean = true;
            return result(decoded.snapshot(), warnings);
        case PARTIAL:
            preserveOriginal = true;
            warnings.add("Recovered valid rows from " + path + ". Balances may be incomplete. "
                    + "The original file will be preserved before any save.");
            return result(decoded.snapshot(), warnings);
        case UNSUPPORTED:
            savingAllowed = false;
            warnings.add("Unsupported save format at " + path + ". Saving is disabled for this session.");
            return result(LedgerSnapshot.empty(), warnings);
        case UNRECOVERABLE:
            preserveOriginal = true;
            return recoverBackup(true, warnings);
        default:
            throw new IllegalStateException("Unknown storage decode status");
        }
    }

    private LoadResult recoverBackup(boolean damagedMain, List<String> warnings) {
        try {
            LedgerCodec.DecodeResult backup = read(backupPath());
            if (backup != null && backup.status() == LedgerCodec.Status.CLEAN) {
                warnings.add("Recovered financial state from backup: " + backupPath()
                        + ". Files will remain unchanged until a state-changing command is used.");
                return result(backup.snapshot(), warnings);
            }
            if (backup != null && backup.status() == LedgerCodec.Status.UNSUPPORTED) {
                savingAllowed = false;
                warnings.add("Unsupported backup format at " + backupPath()
                        + ". Starting an unsaved session. Saving is disabled to protect incompatible data.");
                return result(LedgerSnapshot.empty(), warnings);
            }
            if (backup != null) {
                warnings.add("The backup is not fully valid and was not used: " + backupPath());
            }
        } catch (CharacterCodingException e) {
            warnings.add("The backup contains invalid UTF-8 and was not used: " + backupPath());
        } catch (IOException e) {
            return readFailure(backupPath());
        }
        if (damagedMain || !warnings.isEmpty()) {
            warnings.add("The save file is corrupted and no saved state could be recovered. "
                    + "FinNUS is starting with an empty ledger. To inspect or repair the original, "
                    + "exit without changing transactions, opening balance or rates. Open "
                    + (damagedMain ? path : backupPath())
                    + ", repair it, then restart FinNUS.");
        }
        return result(LedgerSnapshot.empty(), warnings);
    }

    private LoadResult readFailure(Path source) {
        savingAllowed = false;
        return result(LedgerSnapshot.empty(), List.of("Unable to read " + source
                + ". Starting an unsaved session. Saving is disabled to protect data that could not be loaded."));
    }

    private LoadResult result(LedgerSnapshot snapshot, List<String> warnings) {
        for (String warning : warnings) {
            LOGGER.warning(warning);
        }
        return new LoadResult(snapshot, warnings, savingAllowed);
    }

    private LedgerCodec.DecodeResult read(Path source) throws IOException {
        try {
            return codec.decode(Files.readString(source, StandardCharsets.UTF_8));
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    /**
     * Writes a complete snapshot without truncating the main file or its backup.
     *
     * @throws IOException If writing, backup rotation or atomic replacement fails.
     */
    @Override
    public void save(LedgerSnapshot snapshot) throws IOException {
        if (!initialized) {
            load();
        }
        if (!savingAllowed) {
            throw new IOException("Saving is disabled because the source could not be loaded safely");
        }
        Files.createDirectories(path.getParent());
        if (preserveOriginal) {
            preserveFile(path);
        }
        Path temporary = Files.createTempFile(path.getParent(), ".finnus-", ".tmp");
        try {
            Files.writeString(temporary, codec.encode(snapshot), StandardCharsets.UTF_8);
            if (knownClean && Files.exists(path)) {
                rotateBackup();
            }
            replacement.replace(temporary, path);
            knownClean = true;
            preserveOriginal = false;
            LOGGER.fine("Saved ledger snapshot");
        } finally {
            removeTemporary(temporary);
        }
    }

    private void rotateBackup() throws IOException {
        protectDamagedBackup();
        Path temporary = Files.createTempFile(path.getParent(), ".finnus-", ".tmp");
        try {
            Files.copy(path, temporary, StandardCopyOption.REPLACE_EXISTING);
            replacement.replace(temporary, backupPath());
        } finally {
            removeTemporary(temporary);
        }
    }

    private void protectDamagedBackup() throws IOException {
        try {
            LedgerCodec.DecodeResult backup = read(backupPath());
            if (backup != null && backup.status() != LedgerCodec.Status.CLEAN) {
                preserveFile(backupPath());
            }
        } catch (CharacterCodingException e) {
            preserveFile(backupPath());
        }
    }

    /**
     * Copies damaged bytes to a unique recovery file before any replacement is permitted.
     */
    private void preserveFile(Path source) throws IOException {
        if (preservedSources.contains(source)) {
            return;
        }
        Path destination = Files.createTempFile(path.getParent(), source.getFileName() + ".corrupt-", "");
        Path temporary = null;
        try {
            temporary = Files.createTempFile(path.getParent(), ".finnus-", ".tmp");
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            replacement.replace(temporary, destination);
            preservedSources.add(source);
            LOGGER.warning("Preserved damaged storage file at " + destination);
        } catch (IOException e) {
            removeTemporary(destination);
            throw e;
        } finally {
            if (temporary != null) {
                removeTemporary(temporary);
            }
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
