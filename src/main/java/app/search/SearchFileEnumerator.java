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

        // Without FOLLOW_LINKS, Files.walkFileTree treats a symlinked start path as a plain file, so a root
        // folder that is itself a link yielded nothing. Walk the link target but keep reporting paths under the
        // root the user chose. Links inside the tree are still reported and never followed.
        Path root = options.rootDirectory();
        Path walkRoot = Files.isSymbolicLink(root) && Files.isDirectory(root) ? root.toRealPath() : root;

        Files.walkFileTree(walkRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path walkedDir, BasicFileAttributes attrs) throws IOException {
                if (cancellationRequested.getAsBoolean()) {
                    return FileVisitResult.TERMINATE;
                }

                Path dir = displayPath(walkedDir);
                if (!walkedDir.equals(walkRoot)
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
            public FileVisitResult visitFile(Path walkedFile, BasicFileAttributes attrs) throws IOException {
                if (cancellationRequested.getAsBoolean()) {
                    return FileVisitResult.TERMINATE;
                }

                Path file = displayPath(walkedFile);
                if (!attrs.isRegularFile()) {
                    reportNonRegularEntry(file, attrs);
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
            public FileVisitResult visitFileFailed(Path walkedFile, IOException exception) throws IOException {
                Path file = displayPath(walkedFile);
                listener.onSkip(new SkippedPath(
                        file,
                        Files.isDirectory(file) ? SkippedPathKind.DIRECTORY : SkippedPathKind.FILE,
                        classifyIoReason(exception),
                        exception.getMessage() == null ? "" : exception.getMessage()
                ));
                return cancellationRequested.getAsBoolean() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
            }

            private Path displayPath(Path walked) {
                return walkRoot == root ? walked : root.resolve(walkRoot.relativize(walked));
            }

            private void reportNonRegularEntry(Path entry, BasicFileAttributes attrs) throws IOException {
                // Symbolic links, pipes, sockets and devices are never opened, but they are reported so that
                // nothing in scope disappears silently. A FIFO would block a reader indefinitely.
                boolean directoryTarget = Files.isDirectory(entry);
                SkippedPathKind kind = directoryTarget ? SkippedPathKind.DIRECTORY : SkippedPathKind.FILE;

                if (!options.includeHidden() && PathFilters.isHiddenLike(entry)) {
                    listener.onSkip(new SkippedPath(entry, kind, SkipReason.HIDDEN_EXCLUDED,
                            "Hidden entry excluded by the current search options."));
                    return;
                }

                String extension = PathFilters.extensionOf(entry);
                if (!directoryTarget && (extension.isEmpty() || !options.allowedExtensions().contains(extension))) {
                    listener.onSkip(new SkippedPath(entry, kind, SkipReason.FILTERED_EXTENSION,
                            "Extension is outside the configured search scope."));
                    return;
                }

                if (attrs.isSymbolicLink()) {
                    listener.onSkip(new SkippedPath(entry, kind, SkipReason.SYMBOLIC_LINK,
                            "Symbolic links inside the search folder are not followed."));
                } else {
                    listener.onSkip(new SkippedPath(entry, kind, SkipReason.NOT_REGULAR_FILE,
                            "Not a regular file (pipe, socket, device or similar)."));
                }
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
