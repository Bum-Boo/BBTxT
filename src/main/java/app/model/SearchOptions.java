package app.model;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record SearchOptions(
        Path rootDirectory,
        String query,
        SearchMode mode,
        boolean caseSensitive,
        boolean wholeWord,
        boolean includeHidden,
        Set<String> allowedExtensions
) {

    public SearchOptions {
        rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory");
        query = Objects.requireNonNull(query, "query").trim();
        mode = Objects.requireNonNull(mode, "mode");

        if (query.isEmpty()) {
            throw new IllegalArgumentException("Search query must not be blank.");
        }

        LinkedHashSet<String> normalizedExtensions = new LinkedHashSet<>();
        for (String extension : Objects.requireNonNull(allowedExtensions, "allowedExtensions")) {
            if (extension == null) {
                continue;
            }
            String normalized = extension.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith(".")) {
                normalized = normalized.substring(1);
            }
            if (!normalized.isEmpty()) {
                normalizedExtensions.add(normalized);
            }
        }

        if (normalizedExtensions.isEmpty()) {
            throw new IllegalArgumentException("At least one extension must be configured.");
        }

        allowedExtensions = Set.copyOf(normalizedExtensions);
        if (mode == SearchMode.REGEX) {
            wholeWord = false;
        }
    }

    public boolean regexMode() {
        return mode == SearchMode.REGEX;
    }
}
