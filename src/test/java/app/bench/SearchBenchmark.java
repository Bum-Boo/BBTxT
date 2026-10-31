package app.bench;

import app.model.SearchMode;
import app.model.SearchOptions;
import app.model.SearchSummary;
import app.search.SearchEngine;
import app.util.SearchDefaults;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Reproducible engine benchmark on a seeded synthetic corpus.
 *
 * <pre>
 * ./gradlew runBenchmark                                  # default corpus in build/bench-corpus
 * ./gradlew runBenchmark -Pbench.files=50000 -Pbench.runs=7
 * </pre>
 *
 * The corpus is regenerated only when its marker file does not match the requested parameters. The numbers cover
 * the search engine alone (traversal, decoding, matching, event batching), not Swing table rendering.
 */
public final class SearchBenchmark {

    private static final long SEED = 20261031L;
    private static final String[] WORDS = {
            "request", "response", "handler", "config", "value", "service", "client", "server", "cache", "token",
            "session", "error", "warning", "info", "debug", "retry", "timeout", "buffer", "stream", "parser",
            "사용자", "요청", "응답", "설정", "오류", "검색", "파일", "로그", "日本語", "中文"
    };

    private SearchBenchmark() {
    }

    public static void main(String[] args) throws IOException {
        Path corpus = Path.of(args.length > 0 ? args[0] : "build/bench-corpus");
        int files = args.length > 1 ? Integer.parseInt(args[1]) : 20_000;
        int runs = args.length > 2 ? Integer.parseInt(args[2]) : 5;

        long bytes = ensureCorpus(corpus, files);
        System.out.printf("Corpus: %s, %d files, %.1f MiB, %d CPUs, max heap %d MiB, Java %s%n",
                corpus.toAbsolutePath(), files, bytes / 1048576.0, Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().maxMemory() / 1048576, System.getProperty("java.version"));

        measure(corpus, runs, bytes, "literal, ASCII, case-insensitive", "timeout", SearchMode.LITERAL, false, false);
        measure(corpus, runs, bytes, "literal, ASCII, case-sensitive", "timeout", SearchMode.LITERAL, true, false);
        measure(corpus, runs, bytes, "literal, ASCII, whole-word", "token", SearchMode.LITERAL, false, true);
        measure(corpus, runs, bytes, "literal, Korean", "검색", SearchMode.LITERAL, false, false);
        measure(corpus, runs, bytes, "literal, rare (no matches)", "zz-no-such-term-zz", SearchMode.LITERAL, false, false);
        measure(corpus, runs, bytes, "regex, case-insensitive", "time(out|r)\\s+\\d+", SearchMode.REGEX, false, false);
    }

    private static void measure(
            Path corpus,
            int runs,
            long bytes,
            String label,
            String query,
            SearchMode mode,
            boolean caseSensitive,
            boolean wholeWord
    ) throws IOException {
        SearchOptions options = new SearchOptions(corpus, query, mode, caseSensitive, wholeWord, false,
                SearchDefaults.defaultExtensions());
        SearchEngine engine = new SearchEngine();
        long[] millis = new long[runs];
        SearchSummary last = null;
        // One warm-up run so that the page cache and JIT are in the same state for every configuration.
        engine.search(options, events -> { }, () -> false);
        for (int run = 0; run < runs; run++) {
            long startedAt = System.nanoTime();
            last = engine.search(options, events -> { }, () -> false);
            millis[run] = (System.nanoTime() - startedAt) / 1_000_000L;
        }
        Arrays.sort(millis);
        long median = millis[runs / 2];
        System.out.printf("%-36s median %5d ms (min %5d, max %5d) | %6.0f files/s | %6.1f MiB/s | %d searched, %d matches, %d skipped%n",
                label, median, millis[0], millis[runs - 1],
                last.filesVisited() * 1000.0 / Math.max(1, median),
                bytes / 1048576.0 * 1000.0 / Math.max(1, median),
                last.filesSearched(), last.matchesFound(), last.skippedEntries());
    }

    private static long ensureCorpus(Path corpus, int files) throws IOException {
        Path marker = corpus.resolve(".bench-corpus");
        String expected = "seed=" + SEED + " files=" + files + " v1";
        if (Files.isRegularFile(marker) && Files.readString(marker).equals(expected)) {
            return sizeOf(corpus);
        }
        if (Files.exists(corpus)) {
            try (Stream<Path> paths = Files.walk(corpus)) {
                for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                    Files.delete(path);
                }
            }
        }

        Random random = new Random(SEED);
        Charset ms949 = Charset.forName("MS949");
        for (int index = 0; index < files; index++) {
            Path dir = corpus.resolve("module" + (index % 40)).resolve("pkg" + (index % 13));
            Files.createDirectories(dir);
            int kind = random.nextInt(100);
            if (kind < 45) {
                Files.writeString(dir.resolve("Source" + index + ".java"), javaSource(random), StandardCharsets.UTF_8);
            } else if (kind < 70) {
                Files.writeString(dir.resolve("app" + index + ".log"), logLines(random, 40 + random.nextInt(400)), StandardCharsets.UTF_8);
            } else if (kind < 80) {
                Files.write(dir.resolve("legacy" + index + ".txt"), logLines(random, 50).getBytes(ms949));
            } else if (kind < 88) {
                Files.writeString(dir.resolve("data" + index + ".json"), json(random), StandardCharsets.UTF_8);
            } else if (kind < 94) {
                Files.writeString(dir.resolve("notes" + index + ".md"), logLines(random, 30), StandardCharsets.UTF_8);
            } else if (kind < 97) {
                byte[] binary = new byte[4096 + random.nextInt(65536)];
                random.nextBytes(binary);
                Files.write(dir.resolve("blob" + index + ".log"), binary);
            } else {
                Files.writeString(dir.resolve("ignored" + index + ".tmp"), logLines(random, 20), StandardCharsets.UTF_8);
            }
        }
        // A handful of large files: one big log and one minified single-line bundle.
        Files.writeString(corpus.resolve("big.log"), logLines(random, 400_000), StandardCharsets.UTF_8);
        Files.writeString(corpus.resolve("bundle.min.js"), json(random).repeat(20_000).replace('\n', ' '), StandardCharsets.UTF_8);
        Files.writeString(marker, expected);
        return sizeOf(corpus);
    }

    private static String javaSource(Random random) {
        StringBuilder builder = new StringBuilder("package bench;\n\npublic class Generated {\n");
        int methods = 3 + random.nextInt(30);
        for (int method = 0; method < methods; method++) {
            builder.append("    // ").append(sentence(random)).append('\n')
                    .append("    public String ").append(word(random).replaceAll("[^a-z]", "x")).append(method)
                    .append("(String value) {\n        return \"").append(sentence(random)).append("\";\n    }\n\n");
        }
        return builder.append("}\n").toString();
    }

    private static String logLines(Random random, int lines) {
        StringBuilder builder = new StringBuilder(lines * 80);
        for (int line = 0; line < lines; line++) {
            builder.append("2026-10-31T12:").append(10 + line % 50).append(':').append(10 + random.nextInt(50))
                    .append(" [").append(random.nextBoolean() ? "INFO" : "WARN").append("] ")
                    .append(sentence(random)).append(' ').append(random.nextInt(10_000)).append('\n');
        }
        return builder.toString();
    }

    private static String json(Random random) {
        StringBuilder builder = new StringBuilder("{\n");
        for (int field = 0; field < 20; field++) {
            builder.append("  \"").append(word(random)).append(field).append("\": \"").append(sentence(random)).append("\",\n");
        }
        return builder.append("  \"end\": true\n}\n").toString();
    }

    private static String sentence(Random random) {
        List<String> words = new ArrayList<>();
        int count = 4 + random.nextInt(8);
        for (int index = 0; index < count; index++) {
            words.add(word(random));
        }
        return String.join(" ", words);
    }

    private static String word(Random random) {
        return WORDS[random.nextInt(WORDS.length)];
    }

    private static long sizeOf(Path corpus) throws IOException {
        try (Stream<Path> paths = Files.walk(corpus)) {
            return paths.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException exception) {
                    return 0L;
                }
            }).sum();
        }
    }
}
