package app.search;

import app.model.SkipReason;
import app.model.SkippedPathKind;
import app.testsupport.ScenarioRunner;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static app.testsupport.ScenarioRunner.assertTrue;

/**
 * Synthetic-corpus verification suite: encodings, binary detection, long lines, huge files,
 * symlinks, permissions and ignore rules. Run with {@code ./gradlew runCorpusVerification}.
 */
public final class CorpusVerificationSuite {

    private static final String[] LOCALE_FILES = {
            "/i18n/messages.properties",
            "/i18n/messages_ko.properties",
            "/i18n/messages_ja.properties",
            "/i18n/messages_zh.properties"
    };

    private CorpusVerificationSuite() {
    }

    public static void main(String[] args) {
        ScenarioRunner runner = new ScenarioRunner("Corpus verification");
        EncodingScenarios.register(runner);
        ContentScenarios.register(runner);
        TraversalScenarios.register(runner);
        runner.run("i18n: every skip reason and kind is translated in all bundled languages", () -> {
            List<String> keys = new ArrayList<>();
            for (SkipReason reason : SkipReason.values()) {
                keys.add(reason.messageKey());
            }
            for (SkippedPathKind kind : SkippedPathKind.values()) {
                keys.add(kind.messageKey());
            }
            for (String file : LOCALE_FILES) {
                Properties properties = new Properties();
                try (InputStream input = CorpusVerificationSuite.class.getResourceAsStream(file)) {
                    assertTrue(input != null, "Missing bundle " + file);
                    properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
                }
                for (String key : keys) {
                    String value = properties.getProperty(key);
                    assertTrue(value != null && !value.isBlank(), file + " is missing " + key);
                }
            }
        });
        runner.finish();
    }
}
