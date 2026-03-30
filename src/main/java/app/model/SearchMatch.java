package app.model;

import java.nio.file.Path;

public record SearchMatch(
        Path path,
        int lineNumber,
        int columnNumber,
        String preview,
        String charsetName
) {
}
