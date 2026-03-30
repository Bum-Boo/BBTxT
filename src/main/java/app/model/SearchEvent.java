package app.model;

public sealed interface SearchEvent permits SearchEvent.MatchFound, SearchEvent.SkipFound, SearchEvent.ProgressChanged {

    record MatchFound(SearchMatch match) implements SearchEvent {
    }

    record SkipFound(SkippedPath skippedPath) implements SearchEvent {
    }

    record ProgressChanged(SearchProgress progress) implements SearchEvent {
    }
}
