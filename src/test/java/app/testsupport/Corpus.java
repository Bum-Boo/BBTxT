package app.testsupport;

import app.model.SearchEvent;
import app.model.SearchMatch;
import app.model.SearchMode;
import app.model.SearchOptions;
import app.model.SearchSummary;
import app.model.SkipReason;
import app.model.SkippedPath;
import app.search.SearchEngine;
import app.util.SearchDefaults;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

/**
 * Builds synthetic on-disk corpora in a temporary directory and runs the real search engine against them.
 */
public final class Corpus implements AutoCloseable {

    public static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    public static final byte[] UTF16LE_BOM = {(byte) 0xFF, (byte) 0xFE};
    public static final byte[] UTF16BE_BOM = {(byte) 0xFE, (byte) 0xFF};

    private final Path root;
    private final List<Path> lockedPaths = new ArrayList<>();

    public Corpus(String prefix) throws IOException {
        this.root = Files.createTempDirectory(prefix);
    }

    public Path root() {
        return root;
    }

    public Path dir(String relative) throws IOException {
        return Files.createDirectories(root.resolve(relative));
    }

    public Path write(String relative, byte[]... parts) throws IOException {
        Path target = root.resolve(relative);
        Files.createDirectories(target.getParent());
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        byte[] bytes = new byte[length];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, bytes, offset, part.length);
            offset += part.length;
        }
        return Files.write(target, bytes);
    }

    public Path write(String relative, String text, Charset charset) throws IOException {
        return write(relative, text.getBytes(charset));
    }

    /** Creates a sparse file: the given header followed by a hole up to {@code totalBytes}. */
    public Path sparse(String relative, byte[] header, long totalBytes) throws IOException {
        Path target = write(relative, header);
        try (RandomAccessFile file = new RandomAccessFile(target.toFile(), "rw")) {
            file.setLength(totalBytes);
        }
        return target;
    }

    /** Creates a symbolic link or skips the calling scenario when the platform does not allow it. */
    public Path symlink(String relative, Path target) throws IOException {
        Path link = root.resolve(relative);
        Files.createDirectories(link.getParent());
        try {
            return Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | FileSystemException exception) {
            ScenarioRunner.skip("symbolic links are not available here: " + exception.getMessage());
            return link;
        }
    }

    /**
     * Removes all permissions from a path, or skips the calling scenario when the platform or the current
     * user (for example, root) ignores the change.
     */
    public void makeUnreadable(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("---------"));
            lockedPaths.add(path);
        } catch (UnsupportedOperationException exception) {
            ScenarioRunner.skip("POSIX permissions are not supported here.");
        }
        if (Files.isReadable(path)) {
            restorePermissions(path);
            ScenarioRunner.skip("the current user can read files without permissions (running as root?).");
        }
    }

    public static void restorePermissions(Path path) {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"));
        } catch (UnsupportedOperationException | IOException ignored) {
        }
    }

    public Result search(String query) throws IOException {
        return search(options(query, SearchMode.LITERAL, false, false, false));
    }

    public Result regex(String query) throws IOException {
        return search(options(query, SearchMode.REGEX, false, false, false));
    }

    public SearchOptions options(String query, SearchMode mode, boolean caseSensitive, boolean wholeWord, boolean includeHidden) {
        return options(root, query, mode, caseSensitive, wholeWord, includeHidden, SearchDefaults.defaultExtensions());
    }

    public static SearchOptions options(
            Path root,
            String query,
            SearchMode mode,
            boolean caseSensitive,
            boolean wholeWord,
            boolean includeHidden,
            Set<String> extensions
    ) {
        return new SearchOptions(root, query, mode, caseSensitive, wholeWord, includeHidden, extensions);
    }

    public static Result search(SearchOptions options) throws IOException {
        return search(options, () -> false);
    }

    public static Result search(SearchOptions options, BooleanSupplier cancellationRequested) throws IOException {
        List<SearchEvent> events = new ArrayList<>();
        SearchSummary summary = new SearchEngine().search(options, events::addAll, cancellationRequested);
        return new Result(summary, events);
    }

    @Override
    public void close() throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        for (Path path : lockedPaths) {
            restorePermissions(path);
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    public record Result(SearchSummary summary, List<SearchEvent> events) {

        public List<SearchMatch> matches() {
            return events.stream()
                    .filter(SearchEvent.MatchFound.class::isInstance)
                    .map(event -> ((SearchEvent.MatchFound) event).match())
                    .toList();
        }

        public List<SkippedPath> skips() {
            return events.stream()
                    .filter(SearchEvent.SkipFound.class::isInstance)
                    .map(event -> ((SearchEvent.SkipFound) event).skippedPath())
                    .toList();
        }

        public List<SearchMatch> matchesIn(String fileName) {
            return matches().stream()
                    .filter(match -> match.path().getFileName().toString().equals(fileName))
                    .toList();
        }

        public SkippedPath skipFor(String fileName) {
            return skips().stream()
                    .filter(skip -> skip.path().getFileName() != null
                            && skip.path().getFileName().toString().equals(fileName))
                    .findFirst()
                    .orElse(null);
        }

        public SkipReason skipReasonFor(String fileName) {
            SkippedPath skip = skipFor(fileName);
            return skip == null ? null : skip.reason();
        }

        public boolean searchedWithoutMatch(String fileName) {
            return matchesIn(fileName).isEmpty() && skipFor(fileName) == null;
        }
    }
}
