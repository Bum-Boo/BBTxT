package app.ui;

import app.i18n.AppLanguage;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

public final class AppTheme {

    public static final Color BASE = Color.decode("#F2F6FB");
    public static final Color PANEL = Color.decode("#E6EEF8");
    public static final Color ELEVATED_WHITE = Color.WHITE;
    public static final Color STRUCTURAL_BLUE = Color.decode("#2B63D9");
    public static final Color DEEP_BLUE = Color.decode("#163A7A");
    public static final Color SIGNAL_RED = Color.decode("#D94A4A");
    public static final Color SIGNAL_YELLOW = Color.decode("#F2C84B");
    public static final Color FRAME = Color.decode("#2D3442");
    public static final Color TEXT = Color.decode("#132238");
    public static final Color BORDER = Color.decode("#C4D2E7");
    public static final Color BORDER_STRONG = Color.decode("#A8BCD7");
    public static final Color ROW_ALT = Color.decode("#F7FAFE");
    public static final Color SELECTION = Color.decode("#D8E6FF");
    public static final Color DANGER_BORDER = Color.decode("#A44747");
    public static final Color NOTICE_BACKGROUND = Color.decode("#FFF8E4");
    public static final Color NOTICE_TEXT = Color.decode("#7A5A14");
    private static Font uiFont = new Font("Dialog", Font.PLAIN, 13);
    private static Font tableFont = new Font("Dialog", Font.PLAIN, 13);
    private static Font inputFont = new Font("Dialog", Font.PLAIN, 13);
    private static int baseSize = 13;

    private AppTheme() {
    }

    public static void install() {
        install(AppLanguage.ENGLISH);
    }

    public static void install(AppLanguage ignoredLanguage) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        Font lafFont = UIManager.getFont("Label.font");
        baseSize = lafFont == null ? 13 : Math.max(lafFont.getSize(), 13);
        applyLanguage(ignoredLanguage);
    }

    public static void applyLanguage(AppLanguage ignoredLanguage) {
        UiFontResolver.ResolvedFonts resolvedFonts = UiFontResolver.resolve(baseSize);
        uiFont = resolvedFonts.uiFont();
        tableFont = resolvedFonts.contentFont();
        inputFont = resolvedFonts.inputFont();

        installUiDefaults();
    }

    private static void installUiDefaults() {
        UIManager.put("Panel.background", BASE);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TextField.font", inputFont);
        UIManager.put("TextArea.font", tableFont);
        UIManager.put("TextPane.font", tableFont);
        UIManager.put("EditorPane.font", tableFont);
        UIManager.put("PasswordField.font", inputFont);
        UIManager.put("FormattedTextField.font", inputFont);
        UIManager.put("Button.font", uiFont);
        UIManager.put("Label.font", uiFont);
        UIManager.put("CheckBox.font", uiFont);
        UIManager.put("ComboBox.font", uiFont);
        UIManager.put("TabbedPane.font", uiFont);
        UIManager.put("List.font", uiFont);
        UIManager.put("Menu.font", uiFont);
        UIManager.put("MenuItem.font", uiFont);
        UIManager.put("ToolTip.font", uiFont);
        UIManager.put("OptionPane.messageFont", uiFont);
        UIManager.put("OptionPane.buttonFont", uiFont);
        UIManager.put("Table.font", tableFont);
        UIManager.put("TableHeader.font", uiFont.deriveFont(Font.BOLD, (float) baseSize));
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("TabbedPane.selected", ELEVATED_WHITE);
        UIManager.put("TabbedPane.contentAreaColor", ELEVATED_WHITE);
    }

    public static Font uiFont() {
        return uiFont;
    }

    public static Font tableFont() {
        return tableFont;
    }

    public static Font contentFont() {
        return tableFont;
    }

    public static Font inputFont() {
        return inputFont;
    }

    public static boolean supportsMultilingualUi(Font font) {
        return UiFontResolver.supportsMultilingual(font);
    }

    public static boolean supportsLanguage(AppLanguage language, Font font) {
        return UiFontResolver.supportsLanguage(language, font);
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        );
    }

    public static Border accentSectionBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, SIGNAL_YELLOW),
                BorderFactory.createEmptyBorder(0, 0, 8, 0)
        );
    }

    public static Border inputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_STRONG),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        );
    }

    public static Border elevatedBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_STRONG),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );
    }

    public static Border noticeBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, SIGNAL_YELLOW),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)
        );
    }

    public static void stylePanelSurface(JComponent component, Color background) {
        component.setOpaque(true);
        component.setBackground(background);
        component.setForeground(TEXT);
    }

    public static void styleTextInput(JTextComponent component) {
        component.setBackground(ELEVATED_WHITE);
        component.setForeground(TEXT);
        component.setCaretColor(DEEP_BLUE);
        component.setFont(inputFont());
    }

    public static void styleInputScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(inputBorder());
        scrollPane.setBackground(ELEVATED_WHITE);
        scrollPane.getViewport().setBackground(ELEVATED_WHITE);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    }

    public static void styleReadOnlySummary(JTextArea area) {
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setForeground(TEXT);
        area.setFont(contentFont());
        area.setMargin(new Insets(0, 0, 0, 0));
        area.setBorder(BorderFactory.createEmptyBorder());
    }

    public static void styleScrollPane(JScrollPane scrollPane, Color viewportColor) {
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_STRONG));
        scrollPane.getViewport().setBackground(viewportColor);
        scrollPane.setBackground(viewportColor);
    }

    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(ELEVATED_WHITE);
        comboBox.setForeground(TEXT);
        comboBox.setBorder(BorderFactory.createLineBorder(BORDER_STRONG));
        comboBox.setFont(uiFont());
    }

    public static void styleBadge(JLabel label) {
        label.setOpaque(true);
        label.setBackground(Color.decode("#EDF4FF"));
        label.setForeground(DEEP_BLUE);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#C9D9F0")),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        label.setFont(uiFont().deriveFont(Font.BOLD, 12f));
    }

    public static void stylePrimaryButton(JButton button) {
        styleFilledButton(button, STRUCTURAL_BLUE, DEEP_BLUE, Color.WHITE);
    }

    public static void styleNeutralButton(JButton button) {
        styleFilledButton(button, ELEVATED_WHITE, BORDER_STRONG, TEXT);
    }

    public static void styleDangerButton(JButton button) {
        styleFilledButton(button, SIGNAL_RED, DANGER_BORDER, Color.WHITE);
    }

    private static void styleFilledButton(JButton button, Color fill, Color borderColor, Color textColor) {
        button.setUI(new FilledButtonUI(textColor));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBackground(fill);
        button.setForeground(textColor);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(uiFont().deriveFont(Font.BOLD, 13f));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
    }

    private static final class FilledButtonUI extends BasicButtonUI {

        private final Color textColor;

        private FilledButtonUI(Color textColor) {
            this.textColor = textColor;
        }

        @Override
        protected void paintText(Graphics graphics, AbstractButton button, Rectangle textRect, String text) {
            FontMetrics metrics = graphics.getFontMetrics();
            int textX = textRect.x;
            int textY = textRect.y + metrics.getAscent();
            graphics.setColor(button.isEnabled() ? textColor : textColor);
            BasicGraphicsUtils.drawStringUnderlineCharAt(
                    graphics,
                    text,
                    button.getDisplayedMnemonicIndex(),
                    textX,
                    textY
            );
        }
    }
}
