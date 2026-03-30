package app.util;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class ExtensionFilterParser {

    private ExtensionFilterParser() {
    }

    public static Set<String> parse(String rawValue) {
        return parse(rawValue, "Enter at least one extension or leave the field blank to use defaults.");
    }

    public static Set<String> parse(String rawValue, String validationMessage) {
        if (rawValue == null || rawValue.isBlank()) {
            return SearchDefaults.defaultExtensions();
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String token : rawValue.split("[,;\\s]+")) {
            String normalized = token.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith(".")) {
                normalized = normalized.substring(1);
            }
            if (!normalized.isEmpty()) {
                values.add(normalized);
            }
        }

        if (values.isEmpty()) {
            throw new IllegalArgumentException(validationMessage);
        }

        return values;
    }

    public static String format(Set<String> extensions) {
        return extensions.stream()
                .sorted()
                .map(extension -> "." + extension)
                .collect(Collectors.joining(", "));
    }
}
