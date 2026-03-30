package app.i18n;

import java.util.Locale;

public enum AppLanguage {
    ENGLISH("en", Locale.ENGLISH, "English"),
    KOREAN("ko", Locale.KOREAN, "한국어"),
    JAPANESE("ja", Locale.JAPANESE, "日本語"),
    CHINESE("zh", Locale.SIMPLIFIED_CHINESE, "中文");

    private final String tag;
    private final Locale locale;
    private final String displayName;

    AppLanguage(String tag, Locale locale, String displayName) {
        this.tag = tag;
        this.locale = locale;
        this.displayName = displayName;
    }

    public Locale locale() {
        return locale;
    }

    public String tag() {
        return tag;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static AppLanguage fromTag(String tag) {
        if (tag != null) {
            for (AppLanguage language : values()) {
                if (language.tag.equalsIgnoreCase(tag)) {
                    return language;
                }
            }
        }
        return fromLocale(Locale.getDefault());
    }

    public static AppLanguage fromLocale(Locale locale) {
        if (locale != null) {
            String languageCode = locale.getLanguage();
            for (AppLanguage language : values()) {
                if (language.tag.equalsIgnoreCase(languageCode)) {
                    return language;
                }
            }
        }
        return ENGLISH;
    }
}
