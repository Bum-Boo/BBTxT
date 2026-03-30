package app.model;

import java.nio.file.Path;

public record SkippedPath(
        Path path,
        SkippedPathKind kind,
        SkipReason reason,
        String detail
) {
}
