package app.ui;

import app.i18n.AppLanguage;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

final class UiFontResolver {

    private static final String REGULAR_FONT_RESOURCE = "/fonts/source-han-sans-k/SourceHanSansK-Regular.otf";
    private static final String BOLD_FONT_RESOURCE = "/fonts/source-han-sans-k/SourceHanSansK-Bold.otf";
    private static final String MULTILINGUAL_SAMPLE = "BB TxT \uD55C\uAE00 \u65E5\u672C\u8A9E \u4E2D\u6587";
    private static final String ENGLISH_SAMPLE = "BB TxT Search Preview";
    private static final String KOREAN_SAMPLE = "\uAC80\uC0C9 \uBBF8\uB9AC\uBCF4\uAE30";
    private static final String JAPANESE_SAMPLE = "\u691C\u7D22 \u30D7\u30EC\u30D3\u30E5\u30FC";
    private static final String CHINESE_SAMPLE = "\u641C\u7D22 \u9884\u89C8";

    private static final Font REGULAR_FACE = loadFont(REGULAR_FONT_RESOURCE);
    @SuppressWarnings("unused")
    private static final Font BOLD_FACE = loadFont(BOLD_FONT_RESOURCE);

    private UiFontResolver() {
    }

    static ResolvedFonts resolve(int baseSize) {
        Font unified = REGULAR_FACE.deriveFont((float) baseSize);
        return new ResolvedFonts(unified, unified, unified);
    }

    static boolean supportsLanguage(AppLanguage language, Font font) {
        return font != null && font.canDisplayUpTo(sampleFor(language)) == -1;
    }

    static boolean supportsMultilingual(Font font) {
        return font != null && font.canDisplayUpTo(MULTILINGUAL_SAMPLE) == -1;
    }

    private static Font loadFont(String resourcePath) {
        try (InputStream input = UiFontResolver.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                return new Font(Font.DIALOG, Font.PLAIN, 13);
            }

            Font font = Font.createFont(Font.TRUETYPE_FONT, input);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (Exception exception) {
            return new Font(Font.DIALOG, Font.PLAIN, 13);
        }
    }

    private static String sampleFor(AppLanguage language) {
        return switch (language == null ? AppLanguage.ENGLISH : language) {
            case KOREAN -> KOREAN_SAMPLE;
            case JAPANESE -> JAPANESE_SAMPLE;
            case CHINESE -> CHINESE_SAMPLE;
            case ENGLISH -> ENGLISH_SAMPLE;
        };
    }

    record ResolvedFonts(Font uiFont, Font contentFont, Font inputFont) {
    }
}
