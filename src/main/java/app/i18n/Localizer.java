package app.i18n;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.ResourceBundle;

public final class Localizer {

    private final AppLanguage language;
    private final ResourceBundle bundle;

    public Localizer(AppLanguage language, ResourceBundle bundle) {
        this.language = Objects.requireNonNull(language, "language");
        this.bundle = Objects.requireNonNull(bundle, "bundle");
    }

    public AppLanguage language() {
        return language;
    }

    public String text(String key) {
        return bundle.getString(key);
    }

    public String format(String key, Object... arguments) {
        return MessageFormat.format(text(key), arguments);
    }
}
