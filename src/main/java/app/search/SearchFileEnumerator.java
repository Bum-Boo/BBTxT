package app.search;

import app.model.SearchOptions;
import app.model.SkipReason;
import app.model.SkippedPath;
import app.model.SkippedPathKind;
import app.util.PathFilters;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.function.BooleanSupplier;

final class SearchFileEnumerator {

    interface Listener {
        void onVisitedFile(Path file) throws IOException;

        void onCandidate(SearchFileCandidate candidate) throws IOException;

        void onSkip(SkippedPath skippedPath) throws IOException;
    }

    void enumerate(SearchOptions options, BooleanSupplier cancellationRequested, Listener listener) throws IOException {
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(cancellationRequested, "cancellationRequested");
        Objects.requireNonNull(listener, "listener");

        Files.walkFileTree(options.rootDirectory(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (cancellationRequested.getAsBoolean()) {
                    return FileVisitResult.TERMINATE;
                }

                if (!dir.equals(options.rootDirectory())
                        && !options.includeHidden()
                        && PathFilters.isHiddenLike(dir)) {
                    listener.onSkip(new SkippedPath(
                            dir,
                            SkippedPathKind.DIRECTORY,
                            SkipReason.HIDDEN_EXCLUDED,
                            "Hidden directory excluded from traversal."
                    ));
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (cancellationRequested.getAsBoolean()) {
                    return FileVisitResult.TERMINATE;
                }

                if (!attrs.isRegularFile()) {
                    return FileVisitResult.CONTINUE;
                }

                listener.onVisitedFile(file);

                if (!options.includeHidden() && PathFilters.isHiddenLike(file)) {
                    listener.onSkip(new SkippedPath(
                            file,
                            SkippedPathKind.FILE,
                            SkipReason.HIDDEN_EXCLUDED,
                            "Hidden file excluded by the current search options."
                    ));
                    return FileVisitResult.CONTINUE;
                }

                String extension = PathFilters.extensionOf(file);
                if (extension.isEmpty() || !options.allowedExtensions().contains(extension)) {
                    listener.onSkip(new SkippedPath(
                            file,
                            SkippedPathKind.FILE,
                            SkipReason.FILTERED_EXTENSION,
                            "Extension is outside the configured search scope."
                    ));
                    return FileVisitResult.CONTINUE;
                }

                if (!Files.isReadable(file)) {
                    listener.onSkip(new SkippedPath(
                            file,
                            SkippedPathKind.FILE,
                            SkipReason.UNREADABLE_FILE,
                            "The file exists but cannot be read."
                    ));
                    return FileVisitResult.CONTINUE;
                }

                listener.onCandidate(new SearchFileCandidate(file, extension));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException {
                listener.onSkip(new SkippedPath(
                        file,
                        Files.isDirectory(file) ? SkippedPathKind.DIRECTORY : SkippedPathKind.FILE,
                        classifyIoReason(exception),
                        exception.getMessage() == null ? "" : exception.getMessage()
                ));
                return cancellationRequested.getAsBoolean() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
            }
        });
    }

    private SkipReason classifyIoReason(IOException exception) {
        if (exception instanceof AccessDeniedException) {
            return SkipReason.PERMISSION_DENIED;
        }
        return SkipReason.UNEXPECTED_IO_ERROR;
    }
}
