package app.util;

import java.text.Normalizer;

public final class TextNormalization {

    private TextNormalization() {
    }

    public static boolean isAscii(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) > 0x7F) {
                return false;
            }
        }
        return true;
    }

    public static String normalizeLiteral(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        if (isAscii(value)) {
            return value;
        }
        return Normalizer.normalize(value, Normalizer.Form.NFC);
    }
}
