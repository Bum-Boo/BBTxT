package app.util;

public final class PreviewBuilder {

    private static final int CONTEXT_CHARS = 70;

    private PreviewBuilder() {
    }

    public static String fromLine(String line, int matchStart, int matchLength) {
        if (line == null) {
            return "";
        }
        int start = Math.max(0, matchStart - CONTEXT_CHARS);
        int end = Math.min(line.length(), matchStart + matchLength + CONTEXT_CHARS);
        return compact(line.substring(start, end), start > 0, end < line.length());
    }

    public static String fromText(String content, int matchStart, int matchEnd) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        int start = Math.max(0, matchStart - CONTEXT_CHARS);
        int end = Math.min(content.length(), matchEnd + CONTEXT_CHARS);
        return compact(content.substring(start, end), start > 0, end < content.length());
    }

    private static String compact(String text, boolean prefixEllipsis, boolean suffixEllipsis) {
        StringBuilder compacted = new StringBuilder(text.length() + 8);
        boolean previousSpace = true;

        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            boolean whitespace = value == '\r' || value == '\n' || value == '\t' || value == ' ';

            if (whitespace) {
                if (!previousSpace) {
                    compacted.append(' ');
                }
                previousSpace = true;
            } else {
                compacted.append(value);
                previousSpace = false;
            }
        }

        int length = compacted.length();
        if (length > 0 && compacted.charAt(length - 1) == ' ') {
            compacted.setLength(length - 1);
        }

        if (prefixEllipsis) {
            compacted.insert(0, "... ");
        }
        if (suffixEllipsis) {
            compacted.append(" ...");
        }
        return compacted.toString();
    }
}
