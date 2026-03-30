package app.search;

import app.model.SearchEvent;
import app.model.SearchMatch;
import app.model.SearchProgress;
import app.model.SkippedPath;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class SearchEventBatcher {

    private static final int MAX_EVENT_BATCH_SIZE = 96;
    private static final int PROGRESS_FILE_DELTA = 12;
    private static final int PROGRESS_SEARCHED_DELTA = 6;
    private static final int PROGRESS_SKIP_DELTA = 6;
    private static final int PROGRESS_MATCH_DELTA = 24;
    private static final long PROGRESS_INTERVAL_NANOS = 90_000_000L;

    private final SearchListener listener;
    private final List<SearchEvent> pendingEvents = new ArrayList<>(MAX_EVENT_BATCH_SIZE);

    private SearchEvent.ProgressChanged pendingProgress;
    private int lastProgressFilesVisited;
    private int lastProgressFilesSearched;
    private int lastProgressSkippedEntries;
    private int lastProgressMatchesFound;
    private long lastProgressAt;

    SearchEventBatcher(SearchListener listener) {
        this.listener = listener;
    }

    void addMatches(List<SearchMatch> matches) {
        for (SearchMatch match : matches) {
            pendingEvents.add(new SearchEvent.MatchFound(match));
            flushIfNeeded(false);
        }
    }

    void addSkip(SkippedPath skippedPath) {
        pendingEvents.add(new SearchEvent.SkipFound(skippedPath));
        flushIfNeeded(false);
    }

    void updateProgress(
            int filesVisited,
            int filesSearched,
            int skippedEntries,
            int matchesFound,
            Path currentPath,
            boolean force
    ) {
        pendingProgress = new SearchEvent.ProgressChanged(new SearchProgress(
                filesVisited,
                filesSearched,
                skippedEntries,
                matchesFound,
                currentPath
        ));

        if (force || shouldFlushProgress(filesVisited, filesSearched, skippedEntries, matchesFound)) {
            flush();
        }
    }

    void flush() {
        if (pendingProgress != null) {
            pendingEvents.add(pendingProgress);
            SearchProgress progress = pendingProgress.progress();
            lastProgressFilesVisited = progress.filesVisited();
            lastProgressFilesSearched = progress.filesSearched();
            lastProgressSkippedEntries = progress.skippedEntries();
            lastProgressMatchesFound = progress.matchesFound();
            lastProgressAt = System.nanoTime();
            pendingProgress = null;
        }

        if (pendingEvents.isEmpty()) {
            return;
        }

        listener.onEvents(List.copyOf(pendingEvents));
        pendingEvents.clear();
    }

    private void flushIfNeeded(boolean force) {
        if (force || pendingEvents.size() >= MAX_EVENT_BATCH_SIZE) {
            flush();
        }
    }

    private boolean shouldFlushProgress(int filesVisited, int filesSearched, int skippedEntries, int matchesFound) {
        long now = System.nanoTime();
        if (now - lastProgressAt >= PROGRESS_INTERVAL_NANOS) {
            return true;
        }
        if (filesVisited - lastProgressFilesVisited >= PROGRESS_FILE_DELTA) {
            return true;
        }
        if (filesSearched - lastProgressFilesSearched >= PROGRESS_SEARCHED_DELTA) {
            return true;
        }
        if (skippedEntries - lastProgressSkippedEntries >= PROGRESS_SKIP_DELTA) {
            return true;
        }
        return matchesFound - lastProgressMatchesFound >= PROGRESS_MATCH_DELTA;
    }
}
