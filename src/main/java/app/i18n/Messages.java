package app.i18n;

import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public final class Messages {

    private static final String BUNDLE_BASE_NAME = "i18n.messages";
    private static final String LANGUAGE_PREFERENCE_KEY = "ui.language";
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(Messages.class);

    private Messages() {
    }

    public static AppLanguage loadSavedLanguage() {
        return AppLanguage.fromTag(PREFERENCES.get(LANGUAGE_PREFERENCE_KEY, null));
    }

    public static void saveLanguage(AppLanguage language) {
        PREFERENCES.put(LANGUAGE_PREFERENCE_KEY, language.tag());
    }

    public static Localizer localizer(AppLanguage language) {
        return new Localizer(language, ResourceBundle.getBundle(BUNDLE_BASE_NAME, language.locale()));
    }
}
