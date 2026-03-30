package app.model;

import java.nio.file.Path;

public record SearchProgress(
        int filesVisited,
        int filesSearched,
        int skippedEntries,
        int matchesFound,
        Path currentPath
) {
}
