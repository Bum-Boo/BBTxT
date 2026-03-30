package app.search;

import app.model.SearchMatch;
import app.model.SkippedPath;

import java.nio.file.Path;
import java.util.List;

record FileSearchResult(
        Path path,
        boolean searched,
        List<SearchMatch> matches,
        SkippedPath skippedPath
) {

    static FileSearchResult searched(Path path, List<SearchMatch> matches) {
        return new FileSearchResult(path, true, List.copyOf(matches), null);
    }

    static FileSearchResult skipped(Path path, SkippedPath skippedPath) {
        return new FileSearchResult(path, false, List.of(), skippedPath);
    }

    static FileSearchResult notSearched(Path path) {
        return new FileSearchResult(path, false, List.of(), null);
    }

    static FileSearchResult cancelled(Path path, List<SearchMatch> matches) {
        return new FileSearchResult(path, true, List.copyOf(matches), null);
    }
}
