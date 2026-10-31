package app.search;

import app.model.SearchMatch;
import app.model.SearchMode;
import app.testsupport.Corpus;
import app.testsupport.ScenarioRunner;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

import static app.testsupport.ScenarioRunner.assertEquals;
import static app.testsupport.ScenarioRunner.assertTrue;

/** Korean/CJK text, mixed encodings and byte-order marks. */
final class EncodingScenarios {

    private static final Charset MS949 = Charset.forName("MS949");

    private EncodingScenarios() {
    }

    static void register(ScenarioRunner runner) {
        runner.run("encoding: UTF-8 Korean in .txt and .java reports line, column and preview", () -> {
            try (Corpus corpus = new Corpus("enc-utf8")) {
                corpus.write("notes.txt", "첫 줄\n두 번째 줄에 사과 있음\n", StandardCharsets.UTF_8);
                corpus.write("src/Fruit.java", "class Fruit { String name = \"사과\"; }\n", StandardCharsets.UTF_8);

                Corpus.Result result = corpus.search("사과");

                SearchMatch txt = single(result, "notes.txt");
                assertEquals(2, txt.lineNumber(), "Line number for Korean text.");
                assertEquals(9, txt.columnNumber(), "Column counts Java chars from 1.");
                assertEquals("두 번째 줄에 사과 있음", txt.preview(), "Preview keeps readable Hangul.");
                assertEquals("UTF-8", txt.charsetName(), "Charset label.");
                assertEquals(1, result.matchesIn("Fruit.java").size(), "Korean inside Java string literal.");
            }
        });

        runner.run("encoding: UTF-8 BOM is skipped and does not shift columns", () -> {
            try (Corpus corpus = new Corpus("enc-bom8")) {
                corpus.write("bom.txt", Corpus.UTF8_BOM, "사과 맛\n".getBytes(StandardCharsets.UTF_8));
                corpus.write("bom.java", Corpus.UTF8_BOM, "// 사과\n".getBytes(StandardCharsets.UTF_8));

                Corpus.Result result = corpus.search("사과");

                SearchMatch txt = single(result, "bom.txt");
                assertEquals(1, txt.columnNumber(), "BOM must not count as a column.");
                assertTrue(!txt.preview().contains("﻿"), "Preview must not contain the BOM.");
                assertEquals(1, result.matchesIn("bom.java").size(), "UTF-8 BOM in a UTF-8-primary extension.");
            }
        });

        runner.run("encoding: UTF-16LE and UTF-16BE with BOM, Korean and ASCII", () -> {
            try (Corpus corpus = new Corpus("enc-bom16")) {
                corpus.write("le.txt", Corpus.UTF16LE_BOM, "제목\r\n사과 apple\r\n".getBytes(StandardCharsets.UTF_16LE));
                corpus.write("be.log", Corpus.UTF16BE_BOM, "제목\n사과 apple\n".getBytes(StandardCharsets.UTF_16BE));
                corpus.write("le.java", Corpus.UTF16LE_BOM, "// 사과\n".getBytes(StandardCharsets.UTF_16LE));

                Corpus.Result korean = corpus.search("사과");
                Corpus.Result ascii = corpus.search("APPLE");

                SearchMatch le = single(korean, "le.txt");
                assertEquals(2, le.lineNumber(), "UTF-16LE line number.");
                assertEquals(1, le.columnNumber(), "UTF-16LE BOM must not count as a column.");
                assertEquals("UTF-16LE", le.charsetName(), "UTF-16LE charset label.");
                assertEquals("UTF-16BE", single(korean, "be.log").charsetName(), "UTF-16BE charset label.");
                assertEquals(1, korean.matchesIn("le.java").size(), "UTF-16LE BOM in a UTF-8-primary extension.");
                assertEquals(2, ascii.matches().size(), "Case-insensitive ASCII inside UTF-16 files.");
            }
        });

        runner.run("encoding: UTF-16 without BOM is detected for ASCII-heavy text", () -> {
            try (Corpus corpus = new Corpus("enc-nobom16")) {
                corpus.write("le.txt", "hello apple\nsecond line\n", StandardCharsets.UTF_16LE);
                corpus.write("be.txt", "hello apple\nsecond line\n", StandardCharsets.UTF_16BE);

                Corpus.Result result = corpus.search("apple");

                assertEquals("UTF-16LE", single(result, "le.txt").charsetName(), "BOM-less UTF-16LE heuristic.");
                assertEquals("UTF-16BE", single(result, "be.txt").charsetName(), "BOM-less UTF-16BE heuristic.");
            }
        });

        runner.run("encoding: BOM-only and empty files are searched without errors", () -> {
            try (Corpus corpus = new Corpus("enc-empty")) {
                corpus.write("empty.txt", new byte[0]);
                corpus.write("bom-only.txt", Corpus.UTF8_BOM);
                corpus.write("bom16-only.txt", Corpus.UTF16LE_BOM);

                Corpus.Result result = corpus.search("apple");

                assertEquals(3, result.summary().filesSearched(), "All three files are searched.");
                assertEquals(0, result.skips().size(), "No skips for empty files.");
            }
        });

        runner.run("encoding: MS949 Korean in legacy extensions (.txt/.log/.properties)", () -> {
            try (Corpus corpus = new Corpus("enc-ms949")) {
                String text = "설정 파일\n사과=apple\n";
                corpus.write("a.txt", text, MS949);
                corpus.write("b.log", text, MS949);
                corpus.write("c.properties", text, MS949);
                corpus.write("d.csv", text, Charset.forName("EUC-KR"));

                Corpus.Result result = corpus.search("사과");

                assertEquals(4, result.matches().size(), "Every MS949/EUC-KR legacy file matches.");
                for (SearchMatch match : result.matches()) {
                    assertEquals(2, match.lineNumber(), "Line number in " + match.path().getFileName());
                    assertTrue(match.charsetName().contains("949"), "MS949 label in " + match.path().getFileName());
                    assertTrue(match.preview().startsWith("사과"), "Readable Hangul preview.");
                }
            }
        });

        runner.run("encoding: other legacy encodings are either matched or explicitly skipped", () -> {
            try (Corpus corpus = new Corpus("enc-other")) {
                corpus.write("sjis.txt", "日本語のテキスト apple\n", Charset.forName("Shift_JIS"));
                corpus.write("gbk.txt", "中文文本 apple\n", Charset.forName("GBK"));
                corpus.write("big5.txt", "繁體中文 apple\n", Charset.forName("Big5"));
                corpus.write("cp1252.txt", "naïve “quotes” apple\n", Charset.forName("windows-1252"));
                corpus.write("utf16-korean-nobom.txt", "한글 사과 apple\n", StandardCharsets.UTF_16LE);

                for (SearchMode mode : SearchMode.values()) {
                    Corpus.Result result = Corpus.search(corpus.options("apple", mode, false, false, false));
                    for (String name : new String[]{"sjis.txt", "gbk.txt", "big5.txt", "cp1252.txt", "utf16-korean-nobom.txt"}) {
                        assertTrue(!result.searchedWithoutMatch(name),
                                mode + ": " + name + " must produce a match or an explicit skip.");
                    }
                }
            }
        });

    }

    static SearchMatch single(Corpus.Result result, String fileName) {
        assertEquals(1, result.matchesIn(fileName).size(), "Exactly one match expected in " + fileName
                + " (skip=" + result.skipFor(fileName) + ")");
        return result.matchesIn(fileName).get(0);
    }

    static String reasonName(Corpus.Result result, String fileName) {
        return result.skipReasonFor(fileName) == null ? "searched" : result.skipReasonFor(fileName).name();
    }
}
