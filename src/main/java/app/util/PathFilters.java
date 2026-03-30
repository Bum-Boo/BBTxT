package app.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public final class PathFilters {

    private PathFilters() {
    }

    public static boolean isHiddenLike(Path path) {
        Path fileName = path.getFileName();
        String name = fileName == null ? path.toString() : fileName.toString();
        if (name.startsWith(".")) {
            return true;
        }
        try {
            return Files.isHidden(path);
        } catch (IOException exception) {
            return false;
        }
    }

    public static boolean matchesExtension(Path path, Set<String> allowedExtensions) {
        String extension = extensionOf(path);
        return !extension.isEmpty() && allowedExtensions.contains(extension);
    }

    public static String extensionOf(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return "";
        }

        String name = fileName.toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
