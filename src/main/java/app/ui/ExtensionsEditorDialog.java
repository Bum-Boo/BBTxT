package app.ui;

import app.i18n.Localizer;
import app.util.ExtensionFilterParser;
import app.util.SearchDefaults;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.Set;

public final class ExtensionsEditorDialog extends JDialog {

    private final Localizer localizer;
    private final JTextArea editorArea = new JTextArea(12, 36);
    private Set<String> result;

    private ExtensionsEditorDialog(Frame owner, Set<String> currentExtensions, Localizer localizer) {
        super(owner, localizer.text("dialog.extensions.title"), true);
        this.localizer = localizer;
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(buildContent(currentExtensions));
        pack();
        setLocationRelativeTo(owner);
    }

    public static Set<String> showDialog(Frame owner, Set<String> currentExtensions, Localizer localizer) {
        ExtensionsEditorDialog dialog = new ExtensionsEditorDialog(owner, currentExtensions, localizer);
        dialog.setVisible(true);
        return dialog.result;
    }

    private JPanel buildContent(Set<String> currentExtensions) {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        root.setBackground(AppTheme.BASE);

        JLabel title = new JLabel(localizer.text("dialog.extensions.header"));
        title.setFont(AppTheme.uiFont().deriveFont(java.awt.Font.BOLD, 15f));

        JLabel helper = new JLabel("<html>" + localizer.text("dialog.extensions.help") + "</html>");
        helper.setForeground(AppTheme.DEEP_BLUE);
        helper.setFont(AppTheme.uiFont());

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(AppTheme.accentSectionBorder());
        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(helper);

        editorArea.setText(ExtensionFilterParser.format(currentExtensions));
        editorArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        editorArea.setFont(AppTheme.inputFont());
        editorArea.setBackground(AppTheme.ELEVATED_WHITE);
        editorArea.setForeground(AppTheme.TEXT);
        editorArea.setCaretColor(AppTheme.DEEP_BLUE);
        editorArea.setLineWrap(true);
        editorArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(editorArea);
        AppTheme.styleScrollPane(scrollPane, AppTheme.ELEVATED_WHITE);

        JButton resetButton = new JButton(localizer.text("button.resetDefaults"));
        JButton cancelButton = new JButton(localizer.text("button.cancel"));
        JButton applyButton = new JButton(localizer.text("button.apply"));
        AppTheme.styleNeutralButton(resetButton);
        AppTheme.styleNeutralButton(cancelButton);
        AppTheme.stylePrimaryButton(applyButton);

        resetButton.addActionListener(event -> editorArea.setText(ExtensionFilterParser.format(SearchDefaults.defaultExtensions())));
        cancelButton.addActionListener(event -> dispose());
        applyButton.addActionListener(event -> applyAndClose());

        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.setOpaque(false);

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftButtons.setOpaque(false);
        leftButtons.add(resetButton);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightButtons.setOpaque(false);
        rightButtons.add(cancelButton);
        rightButtons.add(applyButton);

        buttonRow.add(leftButtons, BorderLayout.WEST);
        buttonRow.add(rightButtons, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);
        root.add(buttonRow, BorderLayout.SOUTH);
        return root;
    }

    private void applyAndClose() {
        try {
            result = ExtensionFilterParser.parse(editorArea.getText(), localizer.text("dialog.extensions.invalidMessage"));
            dispose();
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    exception.getMessage(),
                    localizer.text("dialog.extensions.invalidTitle"),
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }
}
