package finnus.storage;

import java.io.IOException;

/**
 * Persists complete ledger snapshots without depending on console or command classes.
 */
public interface Storage {
    /**
     * Returns recovered state and diagnostics without rewriting files during startup.
     */
    LoadResult load();

    /**
     * Persists a snapshot or reports failure so the application can retain unsaved changes.
     *
     * @throws IOException If the snapshot cannot be saved safely.
     */
    void save(LedgerSnapshot snapshot) throws IOException;
}
