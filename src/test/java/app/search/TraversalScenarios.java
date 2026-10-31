package app.search;

import app.model.SearchMatch;
import app.model.SearchMode;
import app.model.SkippedPath;
import app.model.SkippedPathKind;
import app.testsupport.Corpus;
import app.testsupport.ScenarioRunner;
import app.util.ExtensionFilterParser;
import app.util.SearchDefaults;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static app.search.EncodingScenarios.reasonName;
import static app.testsupport.ScenarioRunner.assertEquals;
import static app.testsupport.ScenarioRunner.assertTrue;

/** Directory traversal: symlinks, permission errors, hidden/extension ignore rules, cancellation. */
final class TraversalScenarios {

    private TraversalScenarios() {
    }

    static void register(ScenarioRunner runner) {
        runner.run("symlinks: a symlinked root folder is searched and paths stay under the chosen root", () -> {
            try (Corpus corpus = new Corpus("link-root")) {
                corpus.write("real/a.txt", "apple\n", StandardCharsets.UTF_8);
                corpus.write("real/sub/b.txt", "apple\n", StandardCharsets.UTF_8);
                corpus.write("real/.hidden/c.txt", "apple\n", StandardCharsets.UTF_8);
                Path link = corpus.symlink("link", corpus.root().resolve("real"));

                Corpus.Result result = Corpus.search(Corpus.options(link, "apple", SearchMode.LITERAL,
                        false, false, false, SearchDefaults.defaultExtensions()));

                assertEquals(2, result.matches().size(), "Both visible files under the symlinked root match.");
                for (SearchMatch match : result.matches()) {
                    assertTrue(match.path().startsWith(link), "Reported path keeps the chosen root: " + match.path());
                }
                SkippedPath hidden = result.skipFor(".hidden");
                assertTrue(hidden != null && hidden.path().startsWith(link), "Hidden directory reported under the root.");
            }
        });

        runner.run("symlinks: links inside the tree are reported, never followed, and loops terminate", () -> {
            try (Corpus corpus = new Corpus("link-inner")) {
                Path outside = corpus.write("outside/secret.txt", "apple outside\n", StandardCharsets.UTF_8);
                corpus.write("tree/real.txt", "apple\n", StandardCharsets.UTF_8);
                corpus.symlink("tree/file-link.txt", outside);
                corpus.symlink("tree/dir-link", outside.getParent());
                corpus.symlink("tree/loop", corpus.root().resolve("tree"));
                corpus.symlink("tree/broken.txt", corpus.root().resolve("tree/missing.txt"));
                corpus.symlink("tree/skipped.tmp", outside);

                Corpus.Result result = Corpus.search(Corpus.options(corpus.root().resolve("tree"), "apple",
                        SearchMode.LITERAL, false, false, false, SearchDefaults.defaultExtensions()));

                assertEquals(1, result.matches().size(), "Only the real file is searched.");
                assertEquals("SYMBOLIC_LINK", reasonName(result, "file-link.txt"), "File symlink is reported.");
                assertEquals("SYMBOLIC_LINK", reasonName(result, "dir-link"), "Directory symlink is reported.");
                assertEquals(SkippedPathKind.DIRECTORY, result.skipFor("dir-link").kind(), "Directory link kind.");
                assertEquals("SYMBOLIC_LINK", reasonName(result, "loop"), "Loop link is reported.");
                assertEquals("SYMBOLIC_LINK", reasonName(result, "broken.txt"), "Broken link is reported.");
                assertEquals("FILTERED_EXTENSION", reasonName(result, "skipped.tmp"), "Extension filter applies first.");
            }
        });

        runner.run("special files: a FIFO is reported and never blocks the search", () -> {
            try (Corpus corpus = new Corpus("fifo")) {
                corpus.write("a.txt", "apple\n", StandardCharsets.UTF_8);
                Path fifo = corpus.root().resolve("pipe.txt");
                if (!makeFifo(fifo)) {
                    ScenarioRunner.skip("mkfifo is not available here.");
                }

                Corpus.Result result = corpus.search("apple");

                assertEquals(1, result.matches().size(), "Regular file matches.");
                assertEquals("NOT_REGULAR_FILE", reasonName(result, "pipe.txt"), "FIFO is reported.");
            }
        });

        runner.run("permissions: unreadable files and folders are reported and the search continues", () -> {
            try (Corpus corpus = new Corpus("perm")) {
                corpus.write("ok.txt", "apple\n", StandardCharsets.UTF_8);
                Path lockedFile = corpus.write("locked.txt", "apple\n", StandardCharsets.UTF_8);
                corpus.write("locked-dir/inner.txt", "apple\n", StandardCharsets.UTF_8);
                corpus.makeUnreadable(lockedFile);
                corpus.makeUnreadable(corpus.root().resolve("locked-dir"));

                Corpus.Result result = corpus.search("apple");

                assertEquals(1, result.matches().size(), "Readable file still matches.");
                assertEquals("UNREADABLE_FILE", reasonName(result, "locked.txt"), "Unreadable file reason.");
                assertEquals("PERMISSION_DENIED", reasonName(result, "locked-dir"), "Unreadable folder reason.");
                assertEquals(SkippedPathKind.DIRECTORY, result.skipFor("locked-dir").kind(), "Folder kind.");
            }
        });

        runner.run("permissions: an unreadable or missing root is reported instead of throwing", () -> {
            try (Corpus corpus = new Corpus("perm-root")) {
                Path missing = corpus.root().resolve("does-not-exist");
                Corpus.Result missingResult = Corpus.search(Corpus.options(missing, "apple", SearchMode.LITERAL,
                        false, false, false, SearchDefaults.defaultExtensions()));
                assertEquals(1, missingResult.skips().size(), "Missing root produces one skip entry.");

                Path locked = corpus.dir("locked-root");
                corpus.write("locked-root/a.txt", "apple\n", StandardCharsets.UTF_8);
                corpus.makeUnreadable(locked);
                Corpus.Result lockedResult = Corpus.search(Corpus.options(locked, "apple", SearchMode.LITERAL,
                        false, false, false, SearchDefaults.defaultExtensions()));
                assertEquals("PERMISSION_DENIED", reasonName(lockedResult, "locked-root"), "Locked root reason.");
            }
        });

        runner.run("ignore rules: hidden files and folders are skipped as a unit unless included", () -> {
            try (Corpus corpus = new Corpus("hidden")) {
                corpus.write("visible.txt", "apple\n", StandardCharsets.UTF_8);
                corpus.write(".env.txt", "apple\n", StandardCharsets.UTF_8);
                for (int index = 0; index < 50; index++) {
                    corpus.write(".git/objects/" + index + ".txt", "apple\n", StandardCharsets.UTF_8);
                }

                Corpus.Result excluded = corpus.search("apple");
                Corpus.Result included = Corpus.search(corpus.options("apple", SearchMode.LITERAL, false, false, true));

                assertEquals(1, excluded.matches().size(), "Only the visible file matches.");
                assertEquals(2, excluded.skips().size(), "One skip for .env.txt and one for the whole .git tree.");
                assertEquals(2, excluded.summary().filesVisited(), ".git contents are not visited.");
                assertEquals(52, included.matches().size(), "Including hidden searches everything.");
            }
        });

        runner.run("ignore rules: a hidden root folder is still searched", () -> {
            try (Corpus corpus = new Corpus("hidden-root")) {
                corpus.write(".config/app.properties", "apple=1\n", StandardCharsets.UTF_8);

                Corpus.Result result = Corpus.search(Corpus.options(corpus.root().resolve(".config"), "apple",
                        SearchMode.LITERAL, false, false, false, SearchDefaults.defaultExtensions()));

                assertEquals(1, result.matches().size(), "Explicitly chosen hidden root is searched.");
            }
        });

        runner.run("ignore rules: extension matching is case-insensitive and uses the last suffix", () -> {
            try (Corpus corpus = new Corpus("ext")) {
                corpus.write("UPPER.TXT", "apple\n", StandardCharsets.UTF_8);
                corpus.write("Makefile", "apple\n", StandardCharsets.UTF_8);
                // Windows silently drops a trailing dot from file names.
                boolean windows = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("windows");
                String trailingDot = windows ? "Makefile2" : "trailing.";
                corpus.write(trailingDot, "apple\n", StandardCharsets.UTF_8);
                corpus.write("archive.tar.gz", "apple\n", StandardCharsets.UTF_8);
                corpus.write("config.yaml.bak", "apple\n", StandardCharsets.UTF_8);
                corpus.write("notes.tmp", "apple\n", StandardCharsets.UTF_8);

                Corpus.Result defaults = corpus.search("apple");
                Corpus.Result custom = Corpus.search(Corpus.options(corpus.root(), "apple", SearchMode.LITERAL,
                        false, false, false, ExtensionFilterParser.parse(".TMP; bak")));

                assertEquals(1, defaults.matchesIn("UPPER.TXT").size(), "Upper-case extension matches.");
                for (String name : new String[]{"Makefile", trailingDot, "archive.tar.gz", "config.yaml.bak", "notes.tmp"}) {
                    assertEquals("FILTERED_EXTENSION", reasonName(defaults, name), name);
                }
                assertEquals(2, custom.matches().size(), "Custom extension set picks .tmp and .bak.");
            }
        });

        runner.run("ignore rules: extension parser normalises separators, dots and case", () -> {
            assertEquals(Set.of("txt", "md", "log"), ExtensionFilterParser.parse(".TXT; md ,\n\tlog"), "Parsed set.");
            assertEquals(SearchDefaults.defaultExtensions(), ExtensionFilterParser.parse("   "), "Blank uses defaults.");
            try {
                ExtensionFilterParser.parse(" , ; .");
                throw new AssertionError("Only separators should be rejected.");
            } catch (IllegalArgumentException expected) {
                // expected
            }
            assertEquals(".log, .md, .txt", ExtensionFilterParser.format(Set.of("txt", "log", "md")), "Formatting.");
        });

        runner.run("scale: 3000 files in nested folders give exact, repeatable counts", () -> {
            try (Corpus corpus = new Corpus("scale")) {
                for (int index = 0; index < 3000; index++) {
                    String folder = "d" + (index % 10) + "/e" + (index % 7);
                    String body = index % 3 == 0 ? "apple " + index + "\napple again\n" : "nothing " + index + "\n";
                    corpus.write(folder + "/f" + index + (index % 5 == 0 ? ".tmp" : ".txt"), body, StandardCharsets.UTF_8);
                }

                for (int run = 0; run < 3; run++) {
                    Corpus.Result result = corpus.search("apple");
                    long expectedMatches = 2L * java.util.stream.IntStream.range(0, 3000)
                            .filter(index -> index % 3 == 0 && index % 5 != 0).count();
                    assertEquals(3000, result.summary().filesVisited(), "Visited files.");
                    assertEquals(2400, result.summary().filesSearched(), "Searched files.");
                    assertEquals(600, result.summary().skippedEntries(), "Filtered files.");
                    assertEquals((int) expectedMatches, result.summary().matchesFound(), "Summary matches.");
                    assertEquals((int) expectedMatches, result.matches().size(), "Match events.");
                }
            }
        });

        runner.run("cancellation: a cancelled search stops promptly and says so", () -> {
            try (Corpus corpus = new Corpus("cancel")) {
                for (int index = 0; index < 2000; index++) {
                    corpus.write("f" + index + ".txt", "apple\n", StandardCharsets.UTF_8);
                }
                AtomicBoolean cancel = new AtomicBoolean(false);
                long startedAt = System.nanoTime();
                java.util.List<app.model.SearchEvent> events = new java.util.ArrayList<>();
                app.model.SearchSummary summary = new SearchEngine().search(
                        corpus.options("apple", SearchMode.LITERAL, false, false, false),
                        batch -> {
                            events.addAll(batch);
                            cancel.set(true);
                        },
                        cancel::get);

                assertTrue(summary.cancelled(), "Summary reports cancellation.");
                assertTrue(summary.filesVisited() < 2000, "Traversal stopped early: " + summary.filesVisited());
                assertTrue(System.nanoTime() - startedAt < TimeUnit.SECONDS.toNanos(10), "Stopped promptly.");
            }
        });
    }

    private static boolean makeFifo(Path path) {
        try {
            Process process = new ProcessBuilder("mkfifo", path.toString()).redirectErrorStream(true).start();
            return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0 && Files.exists(path);
        } catch (Exception exception) {
            return false;
        }
    }
}
