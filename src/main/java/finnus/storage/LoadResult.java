package finnus.storage;

import java.util.List;
import java.util.Objects;

/**
 * Carries startup state, user-safe warnings and permission to attempt subsequent saves.
 */
public record LoadResult(LedgerSnapshot snapshot, List<String> warnings, boolean savingAllowed) {
    /**
     * Prevents load diagnostics from changing after they are returned to the application.
     */
    public LoadResult {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(warnings);
        warnings = List.copyOf(warnings);
    }
}
