package app.search;

import app.io.TextFileInspector;
import app.model.SearchOptions;
import app.model.SearchSummary;
import app.model.SkipReason;
import app.model.SkippedPath;
import app.model.SkippedPathKind;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class SearchEngine {

    private static final int MAX_WORKERS = 6;
    private static final int IN_FLIGHT_MULTIPLIER = 4;

    private final TextFileInspector inspector = new TextFileInspector();
    private final SearchFileEnumerator enumerator = new SearchFileEnumerator();

    public SearchSummary search(SearchOptions options, SearchListener listener, BooleanSupplier cancellationRequested)
            throws IOException, PatternSyntaxException {
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(listener, "listener");
        Objects.requireNonNull(cancellationRequested, "cancellationRequested");

        Pattern regexPattern = compilePattern(options);
        LiteralSearchPlan literalPlan = options.regexMode() ? null : new LiteralSearchPlan(options);
        SearchCounters counters = new SearchCounters();
        SearchEventBatcher batcher = new SearchEventBatcher(listener);
        long startedAt = System.nanoTime();

        int workerCount = determineWorkerCount();
        ExecutorService executor = Executors.newFixedThreadPool(workerCount, workerThreadFactory());
        CompletionService<FileSearchResult> completionService = new ExecutorCompletionService<>(executor);
        SearchContext context = new SearchContext(
                options,
                cancellationRequested,
                completionService,
                new Semaphore(workerCount * IN_FLIGHT_MULTIPLIER),
                counters,
                batcher
        );

        try {
            enumerateAndSubmit(context, literalPlan, regexPattern);
            while (context.pendingTasks > 0) {
                drainCompletedResults(context, true);
            }
        } catch (IOException exception) {
            recordSkip(context, options.rootDirectory(), SkippedPathKind.DIRECTORY, classifyIoReason(exception), exception.getMessage());
            emitProgress(context, options.rootDirectory(), true);
            batcher.flush();
            throw exception;
        } finally {
            executor.shutdown();
            awaitExecutorShutdown(executor);
        }

        emitProgress(context, context.currentPath == null ? options.rootDirectory() : context.currentPath, true);
        batcher.flush();

        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        return new SearchSummary(
                cancellationRequested.getAsBoolean(),
                counters.filesVisited,
                counters.filesSearched,
                counters.skippedEntries,
                counters.matchesFound,
                elapsedMillis,
                cancellationRequested.getAsBoolean() ? "Search cancelled." : "Search completed."
        );
    }

    private void enumerateAndSubmit(SearchContext context, LiteralSearchPlan literalPlan, Pattern regexPattern) throws IOException {
        enumerator.enumerate(context.options, context.cancellationRequested, new SearchFileEnumerator.Listener() {
            @Override
            public void onVisitedFile(Path file) throws IOException {
                context.counters.filesVisited++;
                emitProgress(context, file, false);
                drainCompletedResults(context, false);
            }

            @Override
            public void onCandidate(SearchFileCandidate candidate) throws IOException {
                submitCandidate(context, candidate, literalPlan, regexPattern);
                drainCompletedResults(context, false);
            }

            @Override
            public void onSkip(SkippedPath skippedPath) throws IOException {
                context.counters.skippedEntries++;
                context.batcher.addSkip(skippedPath);
                emitProgress(context, skippedPath.path(), false);
                drainCompletedResults(context, false);
            }
        });
    }

    private void submitCandidate(
            SearchContext context,
            SearchFileCandidate candidate,
            LiteralSearchPlan literalPlan,
            Pattern regexPattern
    ) throws IOException {
        waitForCapacity(context);
        if (context.cancellationRequested.getAsBoolean()) {
            return;
        }

        context.completionService.submit(new SearchFileTask(
                candidate,
                context.options,
                inspector,
                literalPlan,
                regexPattern,
                context.cancellationRequested
        ));
        context.pendingTasks++;
    }

    private void waitForCapacity(SearchContext context) throws IOException {
        while (!context.cancellationRequested.getAsBoolean()) {
            if (context.capacity.tryAcquire()) {
                return;
            }
            drainCompletedResults(context, true);
        }
    }

    private void drainCompletedResults(SearchContext context, boolean waitForOne) throws IOException {
        Future<FileSearchResult> future = waitForOne
                ? takeCompletedResult(context)
                : context.completionService.poll();

        while (future != null) {
            FileSearchResult result = resolveResult(future);
            context.pendingTasks--;
            context.capacity.release();
            applyResult(context, result);
            future = context.completionService.poll();
        }
    }

    private Future<FileSearchResult> takeCompletedResult(SearchContext context) throws IOException {
        try {
            return context.completionService.take();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Search interrupted while waiting for worker results.", exception);
        }
    }

    private FileSearchResult resolveResult(Future<FileSearchResult> future) throws IOException {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Search interrupted while collecting worker results.", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IOException("Search worker failed unexpectedly.", cause);
        }
    }

    private void applyResult(SearchContext context, FileSearchResult result) {
        if (result.searched()) {
            context.counters.filesSearched++;
        }
        if (!result.matches().isEmpty()) {
            context.counters.matchesFound += result.matches().size();
            context.batcher.addMatches(result.matches());
        }
        if (result.skippedPath() != null) {
            context.counters.skippedEntries++;
            context.batcher.addSkip(result.skippedPath());
        }

        emitProgress(context, result.path(), false);
    }

    private void emitProgress(SearchContext context, Path currentPath, boolean force) {
        context.currentPath = currentPath;
        context.batcher.updateProgress(
                context.counters.filesVisited,
                context.counters.filesSearched,
                context.counters.skippedEntries,
                context.counters.matchesFound,
                currentPath,
                force
        );
    }

    private void recordSkip(
            SearchContext context,
            Path path,
            SkippedPathKind kind,
            SkipReason reason,
            String detail
    ) {
        context.counters.skippedEntries++;
        context.batcher.addSkip(new SkippedPath(path, kind, reason, detail == null ? "" : detail));
    }

    private Pattern compilePattern(SearchOptions options) {
        if (!options.regexMode()) {
            return null;
        }

        int flags = Pattern.MULTILINE;
        if (!options.caseSensitive()) {
            flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        return Pattern.compile(options.query(), flags);
    }

    private int determineWorkerCount() {
        return Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), MAX_WORKERS));
    }

    private ThreadFactory workerThreadFactory() {
        AtomicInteger threadCounter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, "search-worker-" + threadCounter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }

    private void awaitExecutorShutdown(ExecutorService executor) {
        try {
            executor.awaitTermination(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private SkipReason classifyIoReason(IOException exception) {
        if (exception instanceof AccessDeniedException) {
            return SkipReason.PERMISSION_DENIED;
        }
        return SkipReason.UNEXPECTED_IO_ERROR;
    }

    private static final class SearchCounters {
        private int filesVisited;
        private int filesSearched;
        private int skippedEntries;
        private int matchesFound;
    }

    private static final class SearchContext {
        private final SearchOptions options;
        private final BooleanSupplier cancellationRequested;
        private final CompletionService<FileSearchResult> completionService;
        private final Semaphore capacity;
        private final SearchCounters counters;
        private final SearchEventBatcher batcher;

        private int pendingTasks;
        private Path currentPath;

        private SearchContext(
                SearchOptions options,
                BooleanSupplier cancellationRequested,
                CompletionService<FileSearchResult> completionService,
                Semaphore capacity,
                SearchCounters counters,
                SearchEventBatcher batcher
        ) {
            this.options = options;
            this.cancellationRequested = cancellationRequested;
            this.completionService = completionService;
            this.capacity = capacity;
            this.counters = counters;
            this.batcher = batcher;
            this.currentPath = options.rootDirectory();
        }
    }
}
