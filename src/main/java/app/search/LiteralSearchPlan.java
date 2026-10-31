package app.search;

import app.model.SearchMatch;
import app.model.SearchOptions;
import app.util.PreviewBuilder;
import app.util.TextNormalization;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BooleanSupplier;

final class LiteralSearchPlan {

    private final String normalizedQuery;
    private final String foldedQuery;
    private final char[] asciiQuery;
    private final boolean caseSensitive;
    private final boolean wholeWord;
    private final boolean asciiCaseInsensitive;
    private final int queryLength;

    LiteralSearchPlan(SearchOptions options) {
        this.caseSensitive = options.caseSensitive();
        this.wholeWord = options.wholeWord();
        this.normalizedQuery = TextNormalization.normalizeLiteral(options.query());
        this.queryLength = normalizedQuery.length();
        this.asciiCaseInsensitive = !caseSensitive && TextNormalization.isAscii(normalizedQuery);
        this.foldedQuery = caseSensitive ? normalizedQuery : foldCase(normalizedQuery);
        this.asciiQuery = asciiCaseInsensitive ? foldedQuery.toCharArray() : new char[0];
    }

    int search(
            Path file,
            BufferedReader reader,
            String charsetName,
            List<SearchMatch> matches,
            BooleanSupplier cancellationRequested
    ) throws IOException {
        String line;
        int lineNumber = 0;

        while ((line = reader.readLine()) != null) {
            if (cancellationRequested.getAsBoolean()) {
                return matches.size();
            }

            lineNumber++;
            String normalizedLine = TextNormalization.normalizeLiteral(line);
            collectLineMatches(file, normalizedLine, lineNumber, charsetName, matches);
        }

        return matches.size();
    }

    private void collectLineMatches(
            Path file,
            String line,
            int lineNumber,
            String charsetName,
            List<SearchMatch> matches
    ) {
        if (line.length() < queryLength) {
            return;
        }

        String foldedLine = caseSensitive || asciiCaseInsensitive ? null : foldCase(line);
        int fromIndex = 0;

        while (fromIndex <= line.length() - queryLength) {
            int index = caseSensitive
                    ? line.indexOf(normalizedQuery, fromIndex)
                    : asciiCaseInsensitive
                    ? indexOfIgnoreCaseAscii(line, fromIndex)
                    : foldedLine.indexOf(foldedQuery, fromIndex);

            if (index < 0) {
                return;
            }

            if (!wholeWord || isWholeWord(line, index)) {
                matches.add(new SearchMatch(
                        file,
                        lineNumber,
                        index + 1,
                        PreviewBuilder.fromLine(line, index, queryLength),
                        charsetName
                ));
            }

            fromIndex = index + Math.max(queryLength, 1);
        }
    }

    /**
     * Folds case one UTF-16 unit at a time so the folded text keeps the original length. Match offsets found in
     * the folded line can then be used directly on the original line. {@link String#toLowerCase} cannot be used
     * here because it changes the length for characters such as U+0130 (Turkish dotted capital I).
     */
    private static String foldCase(String value) {
        char[] chars = value.toCharArray();
        for (int index = 0; index < chars.length; index++) {
            chars[index] = Character.toLowerCase(Character.toUpperCase(chars[index]));
        }
        return new String(chars);
    }

    private int indexOfIgnoreCaseAscii(String line, int fromIndex) {
        int limit = line.length() - queryLength;
        for (int index = fromIndex; index <= limit; index++) {
            if (matchesAscii(line, index)) {
                return index;
            }
        }
        return -1;
    }

    private boolean matchesAscii(String line, int start) {
        for (int offset = 0; offset < queryLength; offset++) {
            if (toLowerAscii(line.charAt(start + offset)) != asciiQuery[offset]) {
                return false;
            }
        }
        return true;
    }

    private char toLowerAscii(char value) {
        return value >= 'A' && value <= 'Z' ? (char) (value + ('a' - 'A')) : value;
    }

    private boolean isWholeWord(String line, int start) {
        int end = start + queryLength;
        boolean leftBoundary = start == 0 || !isWordCharacter(line.charAt(start - 1));
        boolean rightBoundary = end == line.length() || !isWordCharacter(line.charAt(end));
        return leftBoundary && rightBoundary;
    }

    private boolean isWordCharacter(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }
}
