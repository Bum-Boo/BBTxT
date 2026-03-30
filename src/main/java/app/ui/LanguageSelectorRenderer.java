package app.ui;

import app.i18n.AppLanguage;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JLabel;
import java.awt.Component;

final class LanguageSelectorRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
    ) {
        JLabel label = (JLabel) super.getListCellRendererComponent(
                list,
                value,
                index,
                isSelected,
                cellHasFocus
        );

        if (value instanceof AppLanguage language) {
            label.setText(language.toString());
        }

        label.setFont(AppTheme.uiFont());
        label.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        return label;
    }
}
