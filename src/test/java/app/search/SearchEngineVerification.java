package app.search;

import app.model.SearchEvent;
import app.model.SearchMode;
import app.model.SearchOptions;
import app.model.SearchSummary;
import app.model.SkipReason;
import app.model.SkippedPath;
import app.util.SearchDefaults;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public final class SearchEngineVerification {

    private SearchEngineVerification() {
    }

    public static void main(String[] args) throws Exception {
        Path fixture = Files.createTempDirectory("dev-text-finder-verification");
        try {
            writeFixture(fixture);

            verifyLiteralSearch(fixture);
            verifyManyFilesLiteralSearch(fixture);
            verifyHiddenInclusion(fixture);
            verifyKoreanUtf8Search(fixture);
            verifyKoreanUtf8BomSearch(fixture);
            verifyKoreanNormalizationSearch(fixture);

            System.out.println("Verification passed.");
        } finally {
            deleteRecursively(fixture);
        }
    }

    private static void verifyLiteralSearch(Path fixture) throws IOException {
        SearchEngine engine = new SearchEngine();
        List<SearchEvent> events = new ArrayList<>();

        SearchOptions options = new SearchOptions(
                fixture,
                "needle",
                SearchMode.LITERAL,
                false,
                false,
                false,
                SearchDefaults.defaultExtensions()
        );

        SearchSummary summary = collectAllEvents(engine, options, events);

        long matchCount = events.stream().filter(SearchEvent.MatchFound.class::isInstance).count();
        List<SkippedPath> skippedPaths = events.stream()
                .filter(SearchEvent.SkipFound.class::isInstance)
                .map(SearchEvent.SkipFound.class::cast)
                .map(SearchEvent.SkipFound::skippedPath)
                .toList();

        assertEquals(7L, matchCount, "Expected 7 visible matches across UTF-8, UTF-16, and MS949 files.");
        assertTrue(summary.matchesFound() == 7, "Summary match count should agree with event count.");
        assertTrue(skippedPaths.stream().anyMatch(path -> path.reason() == SkipReason.HIDDEN_EXCLUDED),
                "Hidden file should be reported as skipped when hidden files are excluded.");
        assertTrue(skippedPaths.stream().anyMatch(path -> path.reason() == SkipReason.FILTERED_EXTENSION),
                "Unapproved extensions should be reported as filtered.");
        assertTrue(skippedPaths.stream().anyMatch(path -> path.reason() == SkipReason.BINARY_FILE),
                "Binary files should be reported explicitly.");
    }

    private static void verifyHiddenInclusion(Path fixture) throws IOException {
        SearchEngine engine = new SearchEngine();
        List<SearchEvent> events = new ArrayList<>();

        SearchOptions options = new SearchOptions(
                fixture,
                "needle",
                SearchMode.LITERAL,
                false,
                true,
                true,
                SearchDefaults.defaultExtensions()
        );

        SearchSummary summary = collectAllEvents(engine, options, events);
        long matchCount = events.stream().filter(SearchEvent.MatchFound.class::isInstance).count();

        assertEquals(8L, matchCount, "Including hidden files should surface the hidden fixture match.");
        assertTrue(summary.matchesFound() == 8, "Summary should include hidden-file matches when requested.");
    }

    private static void verifyManyFilesLiteralSearch(Path fixture) throws IOException {
        SearchEngine engine = new SearchEngine();
        List<SearchEvent> events = new ArrayList<>();
        Path bulkRoot = fixture.resolve("bulk");
        Files.createDirectories(bulkRoot);

        for (int index = 0; index < 160; index++) {
            Files.writeString(
                    bulkRoot.resolve("File%03d.java".formatted(index)),
                    """
                            class BulkFile {
                                String shared = "bulk-hit";
                            }
                            """.formatted(index),
                    StandardCharsets.UTF_8
            );
        }
        for (int index = 0; index < 40; index++) {
            Files.writeString(
                    bulkRoot.resolve("Ignored%03d.tmp".formatted(index)),
                    "bulk-hit%n".formatted(),
                    StandardCharsets.UTF_8
            );
        }

        SearchOptions options = new SearchOptions(
                bulkRoot,
                "bulk-hit",
                SearchMode.LITERAL,
                false,
                false,
                false,
                SearchDefaults.defaultExtensions()
        );

        SearchSummary summary = collectAllEvents(engine, options, events);
        long matchCount = events.stream().filter(SearchEvent.MatchFound.class::isInstance).count();
        long progressEvents = events.stream().filter(SearchEvent.ProgressChanged.class::isInstance).count();

        assertEquals(160L, matchCount, "Large literal search fixture should report one match per included file.");
        assertTrue(summary.filesVisited() >= 200, "Large literal search should traverse included and filtered files.");
        assertTrue(progressEvents > 0, "Large literal search should still emit progressive updates.");
        assertTrue(progressEvents < summary.filesVisited(), "Progress delivery should be batched rather than per file.");
    }

    private static void verifyKoreanUtf8Search(Path fixture) throws IOException {
        SearchEngine engine = new SearchEngine();
        List<SearchEvent> events = new ArrayList<>();

        SearchOptions options = new SearchOptions(
                fixture,
                "사과",
                SearchMode.LITERAL,
                false,
                false,
                false,
                SearchDefaults.defaultExtensions()
        );

        SearchSummary summary = collectAllEvents(engine, options, events);
        List<SearchEvent.MatchFound> matches = events.stream()
                .filter(SearchEvent.MatchFound.class::isInstance)
                .map(SearchEvent.MatchFound.class::cast)
                .toList();

        assertTrue(summary.matchesFound() >= 2, "Korean UTF-8 fixtures should be matched.");
        assertTrue(matches.stream().anyMatch(match -> match.match().path().getFileName().toString().equals("utf8-korean.txt")),
                "Plain UTF-8 Korean files should be searchable.");
        assertTrue(matches.stream().anyMatch(match -> match.match().preview().contains("사과")),
                "Korean previews should contain readable Hangul.");
        assertTrue(matches.stream().noneMatch(match -> match.match().preview().contains("\uFFFD")),
                "Korean previews should not contain replacement characters.");
    }

    private static void verifyKoreanUtf8BomSearch(Path fixture) throws IOException {
        SearchEngine engine = new SearchEngine();
        List<SearchEvent> events = new ArrayList<>();

        SearchOptions options = new SearchOptions(
                fixture,
                "사과",
                SearchMode.LITERAL,
                false,
                false,
                false,
                SearchDefaults.defaultExtensions()
        );

        collectAllEvents(engine, options, events);

        assertTrue(events.stream()
                        .filter(SearchEvent.MatchFound.class::isInstance)
                        .map(SearchEvent.MatchFound.class::cast)
                        .anyMatch(match -> match.match().path().getFileName().toString().equals("utf8-bom-korean.txt")),
                "UTF-8 BOM Korean files should be searchable.");
    }

    private static void verifyKoreanNormalizationSearch(Path fixture) throws IOException {
        SearchEngine engine = new SearchEngine();
        List<SearchEvent> events = new ArrayList<>();

        SearchOptions options = new SearchOptions(
                fixture,
                "정규화 사과",
                SearchMode.LITERAL,
                false,
                false,
                false,
                SearchDefaults.defaultExtensions()
        );

        collectAllEvents(engine, options, events);

        assertTrue(events.stream()
                        .filter(SearchEvent.MatchFound.class::isInstance)
                        .map(SearchEvent.MatchFound.class::cast)
                        .anyMatch(match -> match.match().path().getFileName().toString().equals("nfd-korean.txt")),
                "Literal search should match decomposed Hangul via normalization.");
    }

    private static void writeFixture(Path root) throws IOException {
        Files.createDirectories(root.resolve("src"));
        Files.createDirectories(root.resolve("docs"));
        Files.createDirectories(root.resolve("unicode"));
        Files.createDirectories(root.resolve("logs"));
        Files.createDirectories(root.resolve("filtered"));
        Files.createDirectories(root.resolve("binary"));

        Files.writeString(root.resolve("src/Main.java"), """
                public class Main {
                    // needle inside code
                    String value = "needle";
                    // another needle lives here
                }
                """, StandardCharsets.UTF_8);

        Files.writeString(root.resolve("docs/readme.md"), """
                Phase 1 fixture
                This markdown file also mentions needle.
                """, StandardCharsets.UTF_8);

        Files.writeString(root.resolve("logs/app.log"), """
                2026-03-30 info ready
                2026-03-30 warn needle detected
                """, StandardCharsets.UTF_8);

        writeUtf16LeBom(root.resolve("unicode/utf16-report.txt"), """
                utf16 fixture
                needle appears here too
                """);

        Files.writeString(root.resolve(".hidden-note.txt"), "hidden needle should only appear when enabled%n".formatted(), StandardCharsets.UTF_8);
        Files.writeString(root.resolve("filtered/notes.tmp"), "needle but wrong extension%n".formatted(), StandardCharsets.UTF_8);

        Files.write(root.resolve("binary/corrupt.log"), new byte[]{0x01, 0x00, 0x02, 0x03, 0x04, 0x05},
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        Files.write(
                root.resolve("unicode/ms949-log.log"),
                "MS949 경로에서도 needle 를 찾아야 합니다.%n".formatted().getBytes(Charset.forName("MS949")),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        Files.writeString(root.resolve("unicode/utf8-korean.txt"), """
                첫 줄
                사과 미리보기
                """, StandardCharsets.UTF_8);

        writeUtf8Bom(root.resolve("unicode/utf8-bom-korean.txt"), """
                BOM 첫 줄
                사과 미리보기
                """);

        Files.writeString(
                root.resolve("unicode/nfd-korean.txt"),
                Normalizer.normalize("정규화 사과", Normalizer.Form.NFD) + System.lineSeparator(),
                StandardCharsets.UTF_8
        );
    }

    private static void writeUtf16LeBom(Path path, String value) throws IOException {
        byte[] bom = {(byte) 0xFF, (byte) 0xFE};
        byte[] body = value.getBytes(StandardCharsets.UTF_16LE);
        byte[] bytes = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, bytes, 0, bom.length);
        System.arraycopy(body, 0, bytes, bom.length, body.length);
        Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void writeUtf8Bom(Path path, String value) throws IOException {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = value.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, bytes, 0, bom.length);
        System.arraycopy(body, 0, bytes, bom.length, body.length);
        Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            stream.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new IllegalStateException(message + " Expected=" + expected + ", actual=" + actual);
        }
    }

    private static SearchSummary collectAllEvents(SearchEngine engine, SearchOptions options, List<SearchEvent> events) throws IOException {
        return engine.search(options, batch -> events.addAll(batch), () -> false);
    }
}
