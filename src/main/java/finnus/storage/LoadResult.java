package finnus.storage;

import java.util.List;
import java.util.Objects;

/**
 * Carries startup state, setup status, user-safe warnings and permission to attempt subsequent saves.
 */
public record LoadResult(LedgerSnapshot snapshot, List<String> warnings, boolean savingAllowed,
                         boolean setupRequired) {
    /**
     * Keeps existing storage implementations source-compatible; they report setup as unnecessary.
     */
    public LoadResult(LedgerSnapshot snapshot, List<String> warnings, boolean savingAllowed) {
        this(snapshot, warnings, savingAllowed, false);
    }

    /**
     * Prevents load diagnostics from changing after they are returned to the application.
     */
    public LoadResult {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(warnings);
        warnings = List.copyOf(warnings);
    }
}
