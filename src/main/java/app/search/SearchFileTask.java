package app.search;

import app.io.TextFileInspector;
import app.io.TextProbe;
import app.model.SearchMatch;
import app.model.SearchMode;
import app.model.SearchOptions;
import app.model.SkipReason;
import app.model.SkippedPath;
import app.model.SkippedPathKind;
import app.util.PreviewBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SearchFileTask implements Callable<FileSearchResult> {

    private final SearchFileCandidate candidate;
    private final SearchOptions options;
    private final TextFileInspector inspector;
    private final LiteralSearchPlan literalPlan;
    private final Pattern regexPattern;
    private final BooleanSupplier cancellationRequested;

    SearchFileTask(
            SearchFileCandidate candidate,
            SearchOptions options,
            TextFileInspector inspector,
            LiteralSearchPlan literalPlan,
            Pattern regexPattern,
            BooleanSupplier cancellationRequested
    ) {
        this.candidate = candidate;
        this.options = options;
        this.inspector = inspector;
        this.literalPlan = literalPlan;
        this.regexPattern = regexPattern;
        this.cancellationRequested = cancellationRequested;
    }

    @Override
    public FileSearchResult call() {
        if (cancellationRequested.getAsBoolean()) {
            return FileSearchResult.notSearched(candidate.path());
        }

        try {
            TextProbe probe = inspector.inspect(candidate.path(), candidate.extension());
            if (probe.binary()) {
                return FileSearchResult.skipped(candidate.path(), new SkippedPath(
                        candidate.path(),
                        SkippedPathKind.FILE,
                        SkipReason.BINARY_FILE,
                        probe.detail()
                ));
            }

            List<SearchMatch> matches = options.mode() == SearchMode.REGEX
                    ? searchRegex(probe)
                    : searchLiteral(probe);
            return cancellationRequested.getAsBoolean()
                    ? FileSearchResult.cancelled(candidate.path(), matches)
                    : FileSearchResult.searched(candidate.path(), matches);
        } catch (AccessDeniedException exception) {
            return skip(SkipReason.PERMISSION_DENIED, exception);
        } catch (CharacterCodingException exception) {
            return skip(SkipReason.DECODE_FAILURE, exception);
        } catch (IOException exception) {
            return skip(classifyIoReason(exception), exception);
        }
    }

    private List<SearchMatch> searchLiteral(TextProbe probe) throws IOException {
        IOException lastFailure = null;

        for (Charset charset : probe.candidateCharsets()) {
            try (BufferedReader reader = inspector.openStrictReader(candidate.path(), charset, probe.bomLength())) {
                List<SearchMatch> matches = new ArrayList<>();
                literalPlan.search(candidate.path(), reader, charset.displayName(), matches, cancellationRequested);
                return matches;
            } catch (CharacterCodingException exception) {
                lastFailure = exception;
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new IOException("No charset candidates available.");
    }

    private List<SearchMatch> searchRegex(TextProbe probe) throws IOException {
        IOException lastFailure = null;

        for (Charset charset : probe.candidateCharsets()) {
            try {
                String content = inspector.decodeStrict(candidate.path(), charset, probe.bomLength());
                return searchRegexContent(content, charset.displayName());
            } catch (CharacterCodingException exception) {
                lastFailure = exception;
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new IOException("No charset candidates available.");
    }

    private List<SearchMatch> searchRegexContent(String content, String charsetName) {
        List<SearchMatch> matches = new ArrayList<>();
        int[] lineStarts = buildLineStarts(content);
        Matcher matcher = regexPattern.matcher(content);

        while (matcher.find()) {
            if (cancellationRequested.getAsBoolean()) {
                return matches;
            }
            if (matcher.start() == matcher.end()) {
                continue;
            }

            int lineNumber = lineNumberForOffset(lineStarts, matcher.start());
            int lineStart = lineStarts[lineNumber - 1];
            matches.add(new SearchMatch(
                    candidate.path(),
                    lineNumber,
                    matcher.start() - lineStart + 1,
                    PreviewBuilder.fromText(content, matcher.start(), matcher.end()),
                    charsetName
            ));
        }

        return matches;
    }

    private FileSearchResult skip(SkipReason reason, Exception exception) {
        return FileSearchResult.skipped(candidate.path(), new SkippedPath(
                candidate.path(),
                SkippedPathKind.FILE,
                reason,
                exception.getMessage() == null ? "" : exception.getMessage()
        ));
    }

    private SkipReason classifyIoReason(IOException exception) {
        if (exception instanceof AccessDeniedException) {
            return SkipReason.PERMISSION_DENIED;
        }
        return SkipReason.UNEXPECTED_IO_ERROR;
    }

    private int[] buildLineStarts(String content) {
        int[] starts = new int[Math.max(8, content.length() / 32 + 1)];
        int size = 1;
        starts[0] = 0;

        for (int index = 0; index < content.length(); index++) {
            if (content.charAt(index) == '\n') {
                if (size == starts.length) {
                    int[] expanded = new int[starts.length * 2];
                    System.arraycopy(starts, 0, expanded, 0, starts.length);
                    starts = expanded;
                }
                starts[size++] = index + 1;
            }
        }

        if (size == starts.length) {
            return starts;
        }

        int[] trimmed = new int[size];
        System.arraycopy(starts, 0, trimmed, 0, size);
        return trimmed;
    }

    private int lineNumberForOffset(int[] lineStarts, int offset) {
        int low = 0;
        int high = lineStarts.length - 1;

        while (low <= high) {
            int middle = (low + high) >>> 1;
            int value = lineStarts[middle];
            if (value <= offset) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }

        return Math.max(1, high + 1);
    }
}
