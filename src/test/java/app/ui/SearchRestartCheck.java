package app.ui;

import app.testsupport.Corpus;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Restarts a running search from the query field (Enter) and checks that the cancelled search can no longer
 * touch the result tables or the button state of the new search.
 */
final class SearchRestartCheck {

    private static final int FILES = 3000;

    private SearchRestartCheck() {
    }

    static void run() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Search restart check skipped because the environment is headless.");
            return;
        }

        try (Corpus corpus = new Corpus("ui-restart")) {
            for (int index = 0; index < FILES; index++) {
                corpus.write("d" + (index % 20) + "/f" + index + ".txt",
                        "alpha old query line " + index + "\n".repeat(3) + (index % 2 == 0 ? "beta new query\n" : ""),
                        StandardCharsets.UTF_8);
            }

            AtomicReference<MainFrame> frameRef = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                MainFrame frame = new MainFrame();
                frameRef.set(frame);
                JTextField rootField = field(frame, "rootField", JTextField.class);
                SearchQueryField queryField = field(frame, "queryField", SearchQueryField.class);
                rootField.setText(corpus.root().toString());
                queryField.setText("alpha");
                queryField.postActionEvent();
            });
            // Let the first search publish results, then restart it with Enter.
            Thread.sleep(150);
            List<Boolean> stopEnabledWhileRunning = new ArrayList<>();
            SwingUtilities.invokeAndWait(() -> {
                SearchQueryField queryField = field(frameRef.get(), "queryField", SearchQueryField.class);
                queryField.setText("beta");
                queryField.postActionEvent();
            });

            long deadline = System.currentTimeMillis() + 60_000;
            while (System.currentTimeMillis() < deadline) {
                AtomicReference<Boolean> finished = new AtomicReference<>();
                SwingUtilities.invokeAndWait(() -> {
                    MainFrame frame = frameRef.get();
                    boolean running = field(frame, "currentWorker", Object.class) != null;
                    stopEnabledWhileRunning.add(!running || field(frame, "stopButton", JButton.class).isEnabled());
                    finished.set(!running && field(frame, "searchButton", JButton.class).isEnabled());
                });
                if (Boolean.TRUE.equals(finished.get())) {
                    break;
                }
                Thread.sleep(20);
            }
            // Give any stray callbacks of the cancelled search time to arrive.
            Thread.sleep(500);

            AtomicReference<String> failure = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                MainFrame frame = frameRef.get();
                MatchTableModel model = field(frame, "matchTableModel", MatchTableModel.class);
                int stale = 0;
                for (int row = 0; row < model.getRowCount(); row++) {
                    if (!String.valueOf(model.getValueAt(row, 4)).contains("beta")) {
                        stale++;
                    }
                }
                if (stale > 0 || model.getRowCount() != FILES / 2) {
                    failure.set("Expected " + (FILES / 2) + " 'beta' rows, got " + model.getRowCount()
                            + " rows of which " + stale + " are stale rows from the cancelled search.");
                } else if (stopEnabledWhileRunning.contains(false)) {
                    failure.set("The cancelled search re-enabled Search and disabled Stop while the new search ran.");
                }
                frame.dispose();
            });
            if (failure.get() != null) {
                throw new IllegalStateException(failure.get());
            }
        }
        System.out.println("Search restart check passed.");
    }

    private static <T> T field(MainFrame frame, String name, Class<T> type) {
        try {
            Field field = MainFrame.class.getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(frame));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
