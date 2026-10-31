package app.search;

import app.model.SearchMatch;
import app.model.SearchMode;
import app.testsupport.Corpus;
import app.testsupport.ScenarioRunner;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import static app.search.EncodingScenarios.reasonName;
import static app.search.EncodingScenarios.single;
import static app.testsupport.ScenarioRunner.assertEquals;
import static app.testsupport.ScenarioRunner.assertTrue;

/** Binary detection, long lines, huge files, line endings, case folding, whole-word and regex behaviour. */
final class ContentScenarios {

    private static final long THREE_GIB = 3L * 1024 * 1024 * 1024;

    private ContentScenarios() {
    }

    static void register(ScenarioRunner runner) {
        runner.run("binary: NUL bytes or a PNG header in the sniffing window are reported as binary", () -> {
            try (Corpus corpus = new Corpus("bin-nul")) {
                corpus.write("nul.txt", "apple".getBytes(StandardCharsets.UTF_8), new byte[]{0, 1, 2}, "\napple\n".getBytes(StandardCharsets.UTF_8));
                corpus.write("image.log", new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'});
                corpus.write("plain.txt", "apple\n", StandardCharsets.UTF_8);

                Corpus.Result result = corpus.search("apple");

                assertEquals("BINARY_FILE", reasonName(result, "nul.txt"), "NUL byte in header.");
                assertEquals("BINARY_FILE", reasonName(result, "image.log"), "PNG renamed to .log.");
                assertEquals(1, result.matches().size(), "Only the plain text file matches.");
            }
        });

        runner.run("binary: control-character heuristic applies only to untrusted extensions", () -> {
            try (Corpus corpus = new Corpus("bin-ctrl")) {
                byte[] controls = new byte[512];
                Arrays.fill(controls, (byte) 0x01);
                corpus.write("blob.dat", "apple ".getBytes(StandardCharsets.UTF_8), controls);
                corpus.write("ansi.log", "\u001B[31mERROR\u001B[0m apple\n".repeat(50), StandardCharsets.UTF_8);
                corpus.write("ansi.dat", "\u001B[31mERROR\u001B[0m apple\n", StandardCharsets.UTF_8);

                Corpus.Result result = Corpus.search(Corpus.options(corpus.root(), "apple", SearchMode.LITERAL,
                        false, false, false, Set.of("dat", "log")));

                assertEquals("BINARY_FILE", reasonName(result, "blob.dat"), "Control-heavy untrusted file.");
                assertEquals(50, result.matchesIn("ansi.log").size(), "ANSI-coloured log stays searchable.");
                assertEquals(1, result.matchesIn("ansi.dat").size(), "Few control chars in untrusted extension.");
            }
        });

        runner.run("binary: NUL bytes after the 8 KiB sniffing window do not hide earlier matches", () -> {
            try (Corpus corpus = new Corpus("bin-late")) {
                byte[] text = ("apple line\n" + "x".repeat(9000) + "\n").getBytes(StandardCharsets.UTF_8);
                corpus.write("late.log", text, new byte[]{0, 0, 0, '\n'});

                Corpus.Result result = corpus.search("apple");

                assertEquals(1, result.matchesIn("late.log").size(), "Header-only sniffing; file is searched.");
            }
        });

        runner.run("long lines: 5 MB single line reports exact column and a bounded preview", () -> {
            try (Corpus corpus = new Corpus("long-line")) {
                int prefix = 5 * 1024 * 1024;
                corpus.write("minified.js", "a".repeat(prefix) + "apple" + "b".repeat(1000), StandardCharsets.UTF_8);

                for (SearchMode mode : SearchMode.values()) {
                    Corpus.Result result = Corpus.search(corpus.options("apple", mode, false, false, false));
                    SearchMatch match = single(result, "minified.js");
                    assertEquals(1, match.lineNumber(), mode + " line.");
                    assertEquals(prefix + 1, match.columnNumber(), mode + " column.");
                    assertTrue(match.preview().length() <= 160, mode + " preview is bounded: " + match.preview().length());
                    assertTrue(match.preview().startsWith("... ") && match.preview().endsWith(" ..."), mode + " ellipses.");
                }
            }
        });

        runner.run("long lines: thousands of matches on one line are all reported", () -> {
            try (Corpus corpus = new Corpus("long-many")) {
                corpus.write("dump.json", "{\"k\":\"apple\"},".repeat(20_000), StandardCharsets.UTF_8);

                Corpus.Result result = corpus.search("apple");

                assertEquals(20_000, result.summary().matchesFound(), "Summary count.");
                assertEquals(20_000, result.matches().size(), "Event count.");
                assertEquals(20_000 * 14 - 14 + 7, result.matches().get(19_999).columnNumber(), "Last column.");
            }
        });

        runner.run("case folding: length-changing lowercase (Turkish dotted I) does not crash or shift columns", () -> {
            try (Corpus corpus = new Corpus("fold")) {
                corpus.write("long.txt", "İ".repeat(100) + " 한글 tail\n", StandardCharsets.UTF_8);
                corpus.write("short.txt", "İİ 한글 tail\n", StandardCharsets.UTF_8);
                corpus.write("other.txt", "한글\n", StandardCharsets.UTF_8);

                Corpus.Result result = corpus.search("한글");

                assertEquals(102, single(result, "long.txt").columnNumber(), "Column after 100 dotted capital I.");
                assertEquals(4, single(result, "short.txt").columnNumber(), "Column after two dotted capital I.");
                assertEquals(1, result.matchesIn("other.txt").size(), "Unrelated files still searched.");
            }
        });

        runner.run("case folding: non-ASCII case-insensitive and case-sensitive behaviour", () -> {
            try (Corpus corpus = new Corpus("fold-unicode")) {
                corpus.write("greek.txt", "ΣΊΣΥΦΟΣ\n", StandardCharsets.UTF_8);
                corpus.write("cyrillic.txt", "Привет МИР\n", StandardCharsets.UTF_8);
                corpus.write("ascii.txt", "Apple APPLE apple\n", StandardCharsets.UTF_8);

                assertEquals(1, corpus.search("σίσυφος").matchesIn("greek.txt").size(), "Greek with final sigma.");
                assertEquals(1, corpus.search("мир").matchesIn("cyrillic.txt").size(), "Cyrillic.");
                assertEquals(1, corpus.regex("мир").matchesIn("cyrillic.txt").size(), "Cyrillic regex (UNICODE_CASE).");
                assertEquals(3, corpus.search("apple").matchesIn("ascii.txt").size(), "ASCII case-insensitive.");
                Corpus.Result sensitive = Corpus.search(corpus.options("Apple", SearchMode.LITERAL, true, false, false));
                assertEquals(1, sensitive.matchesIn("ascii.txt").size(), "Case-sensitive.");
            }
        });

        runner.run("whole word: underscores and digits are word characters, Hangul too", () -> {
            try (Corpus corpus = new Corpus("whole-word")) {
                corpus.write("a.txt", "pineapple apple_pie apple2 (apple). APPLE\n", StandardCharsets.UTF_8);
                corpus.write("k.txt", "사과를 먹었다. 사과 하나\n", StandardCharsets.UTF_8);

                Corpus.Result ascii = Corpus.search(corpus.options("apple", SearchMode.LITERAL, false, true, false));
                Corpus.Result korean = Corpus.search(corpus.options("사과", SearchMode.LITERAL, false, true, false));

                List<Integer> columns = ascii.matchesIn("a.txt").stream().map(SearchMatch::columnNumber).toList();
                assertEquals(List.of(29, 37), columns, "Only '(apple)' and 'APPLE' are whole words.");
                assertEquals(1, korean.matchesIn("k.txt").size(), "Hangul particles count as word characters.");
                assertEquals(10, korean.matchesIn("k.txt").get(0).columnNumber(), "Standalone Hangul word.");
            }
        });

        runner.run("regex: multi-line matches, zero-length matches and invalid patterns", () -> {
            try (Corpus corpus = new Corpus("regex")) {
                corpus.write("multi.txt", "alpha\nbegin\nend\nomega\n", StandardCharsets.UTF_8);
                corpus.write("xs.txt", "a xx b x\n", StandardCharsets.UTF_8);

                SearchMatch multi = single(corpus.regex("begin\\nend"), "multi.txt");
                assertEquals(2, multi.lineNumber(), "Multi-line match starts on line 2.");
                assertTrue(multi.preview().contains("begin end"), "Preview joins lines: " + multi.preview());
                assertEquals(1, corpus.regex("^omega$").matchesIn("multi.txt").size(), "MULTILINE anchors.");
                assertEquals(2, corpus.regex("x*").matchesIn("xs.txt").size(), "Zero-length matches are ignored.");
                try {
                    corpus.regex("([unclosed");
                    throw new AssertionError("Invalid regex should be rejected.");
                } catch (PatternSyntaxException expected) {
                    // The UI validates the pattern up front; the engine still refuses it.
                }
            }
        });
    }

    private static void assertLineColumn(SearchMatch match, int line, int column, String label) {
        assertEquals(line, match.lineNumber(), label + " line.");
        assertEquals(column, match.columnNumber(), label + " column.");
    }
}
