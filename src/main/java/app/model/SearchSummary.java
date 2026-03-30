package app.model;

public record SearchSummary(
        boolean cancelled,
        int filesVisited,
        int filesSearched,
        int skippedEntries,
        int matchesFound,
        long elapsedMillis,
        String message
) {
}
