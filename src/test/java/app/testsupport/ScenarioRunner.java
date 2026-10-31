package app.testsupport;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal dependency-free scenario runner used by the verification harnesses.
 * Each scenario runs in isolation; a failure is reported and the remaining scenarios still run.
 */
public final class ScenarioRunner {

    @FunctionalInterface
    public interface Scenario {
        void run() throws Exception;
    }

    /** Thrown by a scenario that cannot run on the current platform (for example, no symlink support). */
    public static final class SkipScenario extends RuntimeException {
        public SkipScenario(String reason) {
            super(reason);
        }
    }

    private final String suiteName;
    private final List<String> failures = new ArrayList<>();
    private int passed;
    private int skipped;

    public ScenarioRunner(String suiteName) {
        this.suiteName = suiteName;
    }

    public void run(String name, Scenario scenario) {
        long startedAt = System.nanoTime();
        try {
            scenario.run();
            passed++;
            System.out.printf("PASS  %s (%d ms)%n", name, (System.nanoTime() - startedAt) / 1_000_000L);
        } catch (SkipScenario skip) {
            skipped++;
            System.out.printf("SKIP  %s: %s%n", name, skip.getMessage());
        } catch (Throwable throwable) {
            failures.add(name);
            System.out.printf("FAIL  %s: %s%n", name, throwable);
            throwable.printStackTrace(System.out);
        }
    }

    /** Prints the summary and exits non-zero when any scenario failed. */
    public void finish() {
        System.out.printf("%s: %d passed, %d failed, %d skipped%n", suiteName, passed, failures.size(), skipped);
        if (!failures.isEmpty()) {
            System.out.println("Failed scenarios: " + String.join(", ", failures));
            System.exit(1);
        }
    }

    public static void skip(String reason) {
        throw new SkipScenario(reason);
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " Expected=" + expected + ", actual=" + actual);
        }
    }
}
