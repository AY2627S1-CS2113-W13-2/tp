package finnus.storage;

import java.io.IOException;

//@@author lololol8484
/**
 * Indicates that the ledger changed after this session loaded it.
 *
 * <p>The caller must stop the session rather than overwrite the newer ledger.</p>
 */
public class StorageConflictException extends IOException {
    /**
     * Creates a conflict with a user-facing explanation.
     *
     * @param message Explanation shown when the session is stopped.
     */
    public StorageConflictException(String message) {
        super(message);
        assert message != null && !message.isBlank() : "Conflict message must not be blank";
    }
}
//@@author
