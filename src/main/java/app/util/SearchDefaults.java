package app.util;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class SearchDefaults {

    public static final Set<String> DEFAULT_EXTENSIONS = Set.of(
            "txt", "md", "log", "json", "yaml", "yml", "xml", "properties",
            "java", "kt", "kts", "py", "c", "cpp", "h", "hpp", "cs",
            "js", "ts", "jsx", "tsx", "html", "css", "sql", "csv",
            "ini", "bat", "cmd", "gradle"
    );

    private SearchDefaults() {
    }

    public static Set<String> defaultExtensions() {
        return new LinkedHashSet<>(DEFAULT_EXTENSIONS);
    }

    public static String defaultExtensionsText() {
        return DEFAULT_EXTENSIONS.stream()
                .sorted()
                .map(value -> "." + value)
                .collect(Collectors.joining(", "));
    }
}
