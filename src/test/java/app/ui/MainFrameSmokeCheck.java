package app.ui;

import app.AppBrand;
import app.i18n.AppLanguage;
import app.i18n.Messages;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.event.InputMethodEvent;
import java.text.AttributedString;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public final class MainFrameSmokeCheck {

    private MainFrameSmokeCheck() {
    }

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("MainFrame smoke check skipped because the environment is headless.");
            return;
        }

        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                AppTheme.install(Messages.loadSavedLanguage());
                verifyUnifiedFonts();
                verifyLanguageSelectorRenderer();
                verifySearchQueryFieldImeHandling();
                for (AppLanguage language : AppLanguage.values()) {
                    Messages.localizer(language).text("button.search");
                    Messages.localizer(language).text("notice.regexDisablesWholeWord");
                }

                MainFrame frame = new MainFrame();
                if (!AppBrand.NAME.equals(frame.getTitle())) {
                    throw new IllegalStateException("Main window title should stay fixed to the product name.");
                }
                frame.dispose();
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });

        if (failure.get() != null) {
            throw new IllegalStateException("MainFrame smoke check failed.", failure.get());
        }

        System.out.println("MainFrame smoke check passed.");
    }

    private static void verifyUnifiedFonts() {
        String unifiedFamily = null;
        for (AppLanguage language : AppLanguage.values()) {
            AppTheme.applyLanguage(language);

            if (!AppTheme.supportsMultilingualUi(AppTheme.uiFont())) {
                throw new IllegalStateException("UI font should support the multilingual sample.");
            }
            if (!AppTheme.supportsMultilingualUi(AppTheme.tableFont())) {
                throw new IllegalStateException("Content font should support the multilingual sample.");
            }
            if (!AppTheme.supportsMultilingualUi(AppTheme.inputFont())) {
                throw new IllegalStateException("Input font should support the multilingual sample.");
            }

            String currentFamily = AppTheme.uiFont().getFamily();
            if (!currentFamily.toLowerCase(Locale.ROOT).contains("source han sans")) {
                throw new IllegalStateException("Expected Source Han Sans family but resolved " + currentFamily + ".");
            }

            if (unifiedFamily == null) {
                unifiedFamily = currentFamily;
            } else if (!unifiedFamily.equals(currentFamily)) {
                throw new IllegalStateException("UI font family changed across languages: " + unifiedFamily + " -> " + currentFamily);
            }

            if (!currentFamily.equals(AppTheme.tableFont().getFamily()) || !currentFamily.equals(AppTheme.inputFont().getFamily())) {
                throw new IllegalStateException("UI, content, and input fonts should share the same family.");
            }
        }

        AppTheme.applyLanguage(Messages.loadSavedLanguage());
    }

    private static void verifyLanguageSelectorRenderer() {
        for (AppLanguage uiLanguage : AppLanguage.values()) {
            AppTheme.applyLanguage(uiLanguage);

            LanguageSelectorRenderer renderer = new LanguageSelectorRenderer();
            JList<AppLanguage> list = new JList<>(AppLanguage.values());

            for (AppLanguage itemLanguage : AppLanguage.values()) {
                assertRenderedFont(renderer, list, itemLanguage, -1);
                assertRenderedFont(renderer, list, itemLanguage, 0);
            }
        }

        AppTheme.applyLanguage(Messages.loadSavedLanguage());
    }

    private static void verifySearchQueryFieldImeHandling() {
        SearchQueryField field = new SearchQueryField();
        AppTheme.styleTextInput(field);

        if (field.getFont() == null || !AppTheme.supportsMultilingualUi(field.getFont())) {
            throw new IllegalStateException("Search query field should use the multilingual UI font.");
        }
        if (!field.isManagedCaretInstalled()) {
            throw new IllegalStateException("Search query field should start with its managed caret installed.");
        }

        field.processInputMethodEvent(inputMethodTextChanged(field, "\u314E", 0));
        if (!field.isImeCompositionActive()) {
            throw new IllegalStateException("Search query field should track active IME composition.");
        }
        if (!field.isManagedCaretInstalled()) {
            throw new IllegalStateException("Search query field should restore its managed caret during IME composition.");
        }

        field.processInputMethodEvent(inputMethodTextChanged(field, "\uD55C", 1));
        if (field.isImeCompositionActive()) {
            throw new IllegalStateException("Search query field should clear IME composition state after commit.");
        }
        if (!field.isManagedCaretInstalled()) {
            throw new IllegalStateException("Search query field should keep its managed caret after IME commit.");
        }
        if (!"\uD55C".equals(field.getText())) {
            throw new IllegalStateException("Committed IME text should remain in the search query field.");
        }
    }

    private static void assertRenderedFont(
            LanguageSelectorRenderer renderer,
            JList<AppLanguage> list,
            AppLanguage language,
            int index
    ) {
        JLabel label = (JLabel) renderer.getListCellRendererComponent(list, language, index, false, false);
        if (label.getFont() == null || !AppTheme.supportsMultilingualUi(label.getFont())) {
            throw new IllegalStateException("Language selector renderer should use the multilingual UI font at index " + index + ".");
        }
        if (!label.getFont().getFamily().equals(AppTheme.uiFont().getFamily())) {
            throw new IllegalStateException("Language selector renderer should match the unified UI family.");
        }
    }

    private static InputMethodEvent inputMethodTextChanged(SearchQueryField field, String text, int committedCharacterCount) {
        return new InputMethodEvent(
                field,
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                new AttributedString(text).getIterator(),
                committedCharacterCount,
                null,
                null
        );
    }
}
