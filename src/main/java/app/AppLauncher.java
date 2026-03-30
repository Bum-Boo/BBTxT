package app;

import app.i18n.Messages;
import app.ui.AppTheme;
import app.ui.MainFrame;

import javax.swing.SwingUtilities;

public final class AppLauncher {

    private AppLauncher() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppTheme.install(Messages.loadSavedLanguage());
            new MainFrame().setVisible(true);
        });
    }
}
