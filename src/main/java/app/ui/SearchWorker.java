package app.ui;

import app.model.SearchEvent;
import app.model.SearchOptions;
import app.model.SearchSummary;
import app.search.SearchEngine;

import javax.swing.SwingWorker;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class SearchWorker extends SwingWorker<SearchSummary, SearchEvent> {

    private final SearchEngine searchEngine;
    private final SearchOptions options;
    private final Consumer<List<SearchEvent>> eventConsumer;
    private final Consumer<SearchSummary> summaryConsumer;
    private final Consumer<Throwable> errorConsumer;
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    public SearchWorker(
            SearchEngine searchEngine,
            SearchOptions options,
            Consumer<List<SearchEvent>> eventConsumer,
            Consumer<SearchSummary> summaryConsumer,
            Consumer<Throwable> errorConsumer
    ) {
        this.searchEngine = searchEngine;
        this.options = options;
        this.eventConsumer = eventConsumer;
        this.summaryConsumer = summaryConsumer;
        this.errorConsumer = errorConsumer;
    }

    public void requestCancel() {
        cancelRequested.set(true);
    }

    @Override
    protected SearchSummary doInBackground() throws Exception {
        return searchEngine.search(options, this::publishEvents, cancelRequested::get);
    }

    @Override
    protected void process(List<SearchEvent> chunks) {
        eventConsumer.accept(chunks);
    }

    @Override
    protected void done() {
        try {
            summaryConsumer.accept(get());
        } catch (Exception exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            errorConsumer.accept(cause);
        }
    }

    private void publishEvents(List<SearchEvent> events) {
        if (!events.isEmpty()) {
            publish(events.toArray(SearchEvent[]::new));
        }
    }
}
