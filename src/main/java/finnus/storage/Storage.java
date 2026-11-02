//@@author naieel04
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

    //@@author lololol8484
    /**
     * Checks that the persisted ledger is unchanged since the last load or save.
     *
     * @throws IOException If the current ledger cannot be inspected.
     * @throws StorageConflictException If another session changed the ledger.
     */
    void verifyUnchanged() throws IOException;
    //@@author

    /**
     * Persists a snapshot or reports failure so the application can retain unsaved changes.
     *
     * @throws IOException If the snapshot cannot be saved safely.
     * @throws StorageConflictException If another session changed the ledger before saving.
     */
    void save(LedgerSnapshot snapshot) throws IOException;
}
//@@author
