package app.ui;

import app.AppBrand;
import app.i18n.AppLanguage;
import app.i18n.Localizer;
import app.i18n.Messages;
import app.model.SearchEvent;
import app.model.SearchMatch;
import app.model.SearchMode;
import app.model.SearchOptions;
import app.model.SearchProgress;
import app.model.SearchSummary;
import app.model.SkippedPath;
import app.search.SearchEngine;
import app.util.ExtensionFilterParser;
import app.util.SearchDefaults;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public final class MainFrame extends JFrame {

    private final JTextField rootField = new JTextField();
    private final SearchQueryField queryField = new SearchQueryField();
    private final JComboBox<AppLanguage> languageSelector = new JComboBox<>(AppLanguage.values());
    private final JTextArea extensionsSummaryArea = new JTextArea(4, 18);
    private final JLabel appTitleLabel = new JLabel();
    private final JLabel languageLabel = new JLabel();
    private final JLabel rootFieldLabel = new JLabel();
    private final JLabel queryFieldLabel = new JLabel();
    private final JLabel extensionsCountLabel = new JLabel();
    private final JLabel searchOptionsTitleLabel = new JLabel();
    private final JLabel supportedExtensionsTitleLabel = new JLabel();
    private final JLabel extensionsHelperLabel = new JLabel();
    private final JLabel searchResultsTitleLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JLabel detailLabel = new JLabel();
    private final JLabel transientNoticeLabel = new JLabel();
    private final JCheckBox caseSensitiveBox = new JCheckBox();
    private final JCheckBox wholeWordBox = new JCheckBox();
    private final JCheckBox regexBox = new JCheckBox();
    private final JCheckBox includeHiddenBox = new JCheckBox();
    private final JButton browseButton = new JButton();
    private final JButton searchButton = new JButton();
    private final JButton stopButton = new JButton();
    private final JButton editExtensionsButton = new JButton();

    private final MatchTableModel matchTableModel = new MatchTableModel();
    private final SkippedTableModel skippedTableModel = new SkippedTableModel();
    private final JTable matchTable = new JTable(matchTableModel);
    private final JTable skippedTable = new JTable(skippedTableModel);
    private final JTabbedPane resultTabs = new JTabbedPane();

    private final SearchEngine searchEngine = new SearchEngine();
    private final Timer transientNoticeTimer = new Timer(2600, event -> clearTransientNotice());
    private SearchWorker currentWorker;
    private Set<String> configuredExtensions = SearchDefaults.defaultExtensions();
    private AppLanguage currentLanguage = Messages.loadSavedLanguage();
    private Localizer localizer = Messages.localizer(currentLanguage);
    private SearchProgress latestProgress;
    private SearchSummary latestSummary;
    private boolean applyingLanguageSelection;
    private String activeNoticeKey;
    private long lastNoticeAt;

    public MainFrame() {
        super(AppBrand.NAME);
        transientNoticeTimer.setRepeats(false);
        AppTheme.applyLanguage(currentLanguage);
        setIconImage(loadWindowIcon());

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1280, 840);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        applyInitialState();
        wireEvents();
        applyLocalizedText();
        updateExtensionsSummary();
        getRootPane().setDefaultButton(searchButton);
    }

    private Image loadWindowIcon() {
        try (InputStream input = MainFrame.class.getResourceAsStream("/assets/icons/BTXTB.png")) {
            if (input != null) {
                Image icon = ImageIO.read(input);
                if (icon != null) {
                    return icon;
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        root.setBackground(AppTheme.BASE);

        root.add(buildToolbarCard(), BorderLayout.NORTH);
        root.add(buildCenterSplit(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        return root;
    }

    private JPanel buildToolbarCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        AppTheme.stylePanelSurface(card, AppTheme.ELEVATED_WHITE);
        card.setBorder(AppTheme.cardBorder());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JPanel headerRow = new JPanel(new BorderLayout(8, 8));
        headerRow.setOpaque(false);
        appTitleLabel.setFont(AppTheme.uiFont().deriveFont(Font.BOLD, 16f));
        appTitleLabel.setForeground(AppTheme.TEXT);
        headerRow.add(appTitleLabel, BorderLayout.WEST);

        JPanel languagePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        languagePanel.setOpaque(false);
        languagePanel.add(languageLabel);
        languagePanel.add(languageSelector);
        headerRow.add(languagePanel, BorderLayout.EAST);

        JPanel folderRow = new JPanel(new BorderLayout(10, 10));
        folderRow.setOpaque(false);
        folderRow.add(rootFieldLabel, BorderLayout.WEST);
        folderRow.add(rootField, BorderLayout.CENTER);

        JPanel browsePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        browsePanel.setOpaque(false);
        browsePanel.add(browseButton);
        folderRow.add(browsePanel, BorderLayout.EAST);

        JPanel queryRow = new JPanel(new BorderLayout(10, 10));
        queryRow.setOpaque(false);
        queryRow.add(queryFieldLabel, BorderLayout.WEST);
        queryRow.add(queryField, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);
        actionPanel.add(searchButton);
        actionPanel.add(stopButton);
        queryRow.add(actionPanel, BorderLayout.EAST);

        content.add(headerRow);
        content.add(Box.createVerticalStrut(12));
        content.add(folderRow);
        content.add(Box.createVerticalStrut(10));
        content.add(queryRow);

        JPanel accent = new JPanel();
        accent.setPreferredSize(new Dimension(0, 2));
        accent.setBackground(AppTheme.SIGNAL_YELLOW);

        card.add(content, BorderLayout.CENTER);
        card.add(accent, BorderLayout.SOUTH);
        return card;
    }

    private JSplitPane buildCenterSplit() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildOptionsPanel(), buildResultsPanel());
        splitPane.setResizeWeight(0.25);
        splitPane.setDividerLocation(320);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setBackground(AppTheme.BASE);
        return splitPane;
    }

    private JPanel buildOptionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        AppTheme.stylePanelSurface(panel, AppTheme.PANEL);
        panel.setBorder(AppTheme.cardBorder());

        panel.add(buildSectionHeader(searchOptionsTitleLabel), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        content.add(caseSensitiveBox);
        content.add(Box.createVerticalStrut(6));
        content.add(wholeWordBox);
        content.add(Box.createVerticalStrut(6));
        content.add(regexBox);
        content.add(Box.createVerticalStrut(6));
        content.add(includeHiddenBox);
        content.add(Box.createVerticalStrut(10));
        content.add(buildTransientNotice());
        content.add(Box.createVerticalStrut(18));
        content.add(buildExtensionsCard());
        content.add(Box.createVerticalGlue());

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTransientNotice() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        transientNoticeLabel.setVisible(false);
        transientNoticeLabel.setOpaque(true);
        transientNoticeLabel.setBackground(AppTheme.NOTICE_BACKGROUND);
        transientNoticeLabel.setForeground(AppTheme.NOTICE_TEXT);
        transientNoticeLabel.setBorder(AppTheme.noticeBorder());
        transientNoticeLabel.setFont(AppTheme.uiFont().deriveFont(Font.BOLD, 12f));

        wrapper.add(transientNoticeLabel, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildExtensionsCard() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);

        JPanel headerRow = new JPanel(new BorderLayout(8, 8));
        headerRow.setOpaque(false);

        supportedExtensionsTitleLabel.setFont(AppTheme.uiFont().deriveFont(Font.BOLD, 13f));
        supportedExtensionsTitleLabel.setForeground(AppTheme.DEEP_BLUE);
        headerRow.add(supportedExtensionsTitleLabel, BorderLayout.WEST);
        headerRow.add(extensionsCountLabel, BorderLayout.EAST);

        JPanel card = new JPanel(new BorderLayout(0, 10));
        AppTheme.stylePanelSurface(card, AppTheme.ELEVATED_WHITE);
        card.setBorder(AppTheme.elevatedBorder());

        AppTheme.styleReadOnlySummary(extensionsSummaryArea);
        card.add(extensionsSummaryArea, BorderLayout.CENTER);

        extensionsHelperLabel.setForeground(AppTheme.DEEP_BLUE);

        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        footer.add(extensionsHelperLabel, BorderLayout.CENTER);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionRow.setOpaque(false);
        actionRow.add(editExtensionsButton);
        footer.add(actionRow, BorderLayout.SOUTH);

        card.add(footer, BorderLayout.SOUTH);

        wrapper.add(headerRow, BorderLayout.NORTH);
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        AppTheme.stylePanelSurface(panel, AppTheme.ELEVATED_WHITE);
        panel.setBorder(AppTheme.cardBorder());

        panel.add(buildSectionHeader(searchResultsTitleLabel), BorderLayout.NORTH);

        configureTable(matchTable, new int[]{470, 55, 65, 120, 500});
        configureTable(skippedTable, new int[]{470, 80, 170, 420});

        JScrollPane matchScrollPane = new JScrollPane(matchTable);
        JScrollPane skippedScrollPane = new JScrollPane(skippedTable);
        AppTheme.styleScrollPane(matchScrollPane, AppTheme.ELEVATED_WHITE);
        AppTheme.styleScrollPane(skippedScrollPane, AppTheme.ELEVATED_WHITE);

        resultTabs.setBackground(AppTheme.ELEVATED_WHITE);
        resultTabs.setFont(AppTheme.uiFont());
        resultTabs.addTab("", matchScrollPane);
        resultTabs.addTab("", skippedScrollPane);

        panel.add(resultTabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSectionHeader(JLabel titleLabel) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(AppTheme.accentSectionBorder());

        titleLabel.setFont(AppTheme.uiFont().deriveFont(Font.BOLD, 15f));
        titleLabel.setForeground(AppTheme.TEXT);
        header.add(titleLabel, BorderLayout.WEST);
        return header;
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.FRAME),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        panel.setBackground(AppTheme.PANEL);

        statusLabel.setForeground(AppTheme.TEXT);
        statusLabel.setFont(AppTheme.uiFont());
        detailLabel.setForeground(AppTheme.DEEP_BLUE);
        detailLabel.setFont(AppTheme.uiFont());
        detailLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(detailLabel, BorderLayout.CENTER);

        return panel;
    }

    private void configureTable(JTable table, int[] widths) {
        table.setRowHeight(26);
        table.setGridColor(AppTheme.BORDER);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setBackground(AppTheme.ELEVATED_WHITE);
        table.setForeground(AppTheme.TEXT);
        table.setFont(AppTheme.tableFont());
        table.getTableHeader().setBackground(AppTheme.PANEL);
        table.getTableHeader().setForeground(AppTheme.TEXT);
        table.getTableHeader().setFont(AppTheme.uiFont().deriveFont(Font.BOLD, 13f));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_STRONG));

        DefaultTableCellRenderer leftRenderer = new ThemedTableCellRenderer(SwingConstants.LEFT);
        DefaultTableCellRenderer centerRenderer = new ThemedTableCellRenderer(SwingConstants.CENTER);

        for (int index = 0; index < widths.length; index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
            table.getColumnModel().getColumn(index).setCellRenderer(index == 1 || (table == matchTable && index == 2) ? centerRenderer : leftRenderer);
        }
    }
    
    private void configureLanguageSelectorRenderer() {
        languageSelector.setMaximumRowCount(AppLanguage.values().length);

        languageSelector.setRenderer(new LanguageSelectorRenderer());
    }
    private void wireEvents() {
        browseButton.addActionListener(event -> chooseFolder());
        searchButton.addActionListener(event -> startSearch());
        stopButton.addActionListener(event -> stopSearch());
        editExtensionsButton.addActionListener(event -> editExtensions());
        queryField.addActionListener(event -> startSearch());
        regexBox.addActionListener(event -> handleRegexToggle());
        languageSelector.addActionListener(event -> {
            if (!applyingLanguageSelection) {
                changeLanguage((AppLanguage) languageSelector.getSelectedItem());
            }
        });
    }

    private void applyInitialState() {
        applyThemeFonts();

        rootField.setBorder(AppTheme.inputBorder());
        queryField.setBorder(AppTheme.inputBorder());

        int inputHeight = rootField.getPreferredSize().height;
        queryField.setPreferredSize(new Dimension(260, inputHeight));
        queryField.setMinimumSize(new Dimension(140, inputHeight));

        stopButton.setEnabled(false);
        caseSensitiveBox.setOpaque(false);
        wholeWordBox.setOpaque(false);
        regexBox.setOpaque(false);
        includeHiddenBox.setOpaque(false);
        rootFieldLabel.setForeground(AppTheme.TEXT);
        queryFieldLabel.setForeground(AppTheme.TEXT);
        languageLabel.setForeground(AppTheme.DEEP_BLUE);
        queryField.putClientProperty("JComponent.sizeVariant", "regular");

        applyingLanguageSelection = true;
        languageSelector.setSelectedItem(currentLanguage);
        applyingLanguageSelection = false;
    }

    private void applyThemeFonts() {
        AppTheme.styleTextInput(rootField);
        AppTheme.styleTextInput(queryField);
        AppTheme.styleComboBox(languageSelector);
        configureLanguageSelectorRenderer();
        AppTheme.styleReadOnlySummary(extensionsSummaryArea);
        AppTheme.styleBadge(extensionsCountLabel);
        AppTheme.stylePrimaryButton(searchButton);
        AppTheme.styleDangerButton(stopButton);
        AppTheme.styleNeutralButton(browseButton);
        AppTheme.styleNeutralButton(editExtensionsButton);

        caseSensitiveBox.setFont(AppTheme.uiFont());
        wholeWordBox.setFont(AppTheme.uiFont());
        regexBox.setFont(AppTheme.uiFont());
        includeHiddenBox.setFont(AppTheme.uiFont());
        languageLabel.setFont(AppTheme.uiFont().deriveFont(Font.BOLD, 12f));
        rootFieldLabel.setFont(AppTheme.uiFont().deriveFont(Font.BOLD));
        queryFieldLabel.setFont(AppTheme.uiFont().deriveFont(Font.BOLD));
        appTitleLabel.setFont(AppTheme.uiFont().deriveFont(Font.BOLD, 16f));
        transientNoticeLabel.setFont(AppTheme.uiFont().deriveFont(Font.BOLD, 12f));
        supportedExtensionsTitleLabel.setFont(AppTheme.uiFont().deriveFont(Font.BOLD, 13f));
        searchOptionsTitleLabel.setFont(AppTheme.uiFont().deriveFont(Font.BOLD, 15f));
        searchResultsTitleLabel.setFont(AppTheme.uiFont().deriveFont(Font.BOLD, 15f));
        extensionsHelperLabel.setFont(AppTheme.uiFont());
        statusLabel.setFont(AppTheme.uiFont());
        detailLabel.setFont(AppTheme.uiFont());
        resultTabs.setFont(AppTheme.uiFont());
        configureTable(matchTable, new int[]{470, 55, 65, 120, 500});
        configureTable(skippedTable, new int[]{470, 80, 170, 420});
    }

    private void applyLocalizedText() {
        setTitle(AppBrand.NAME);
        appTitleLabel.setText(AppBrand.NAME);
        languageLabel.setText(localizer.text("label.language"));
        rootFieldLabel.setText(localizer.text("label.rootFolder"));
        queryFieldLabel.setText(localizer.text("label.searchQuery"));
        browseButton.setText(localizer.text("button.browse"));
        searchButton.setText(localizer.text("button.search"));
        stopButton.setText(localizer.text("button.stop"));
        editExtensionsButton.setText(localizer.text("button.edit"));

        searchOptionsTitleLabel.setText(localizer.text("section.searchOptions"));
        supportedExtensionsTitleLabel.setText(localizer.text("option.supportedExtensions"));
        extensionsHelperLabel.setText("<html>" + localizer.text("option.extensionsHelp") + "</html>");
        searchResultsTitleLabel.setText(localizer.text("section.searchResults"));

        caseSensitiveBox.setText(localizer.text("option.caseSensitive"));
        wholeWordBox.setText(localizer.text("option.wholeWord"));
        regexBox.setText(localizer.text("option.regexMode"));
        includeHiddenBox.setText(localizer.text("option.includeHidden"));

        matchTableModel.setLocalizer(localizer);
        skippedTableModel.setLocalizer(localizer);
        configureTable(matchTable, new int[]{470, 55, 65, 120, 500});
        configureTable(skippedTable, new int[]{470, 80, 170, 420});

        resultTabs.setTitleAt(0, localizer.text("tab.matches"));
        resultTabs.setTitleAt(1, localizer.text("tab.skippedErrors"));

        updateExtensionsSummary();

        if (activeNoticeKey != null && transientNoticeLabel.isVisible()) {
            transientNoticeLabel.setText(localizer.text(activeNoticeKey));
        }

        if (currentWorker != null) {
            if (latestProgress != null) {
                renderProgress(latestProgress);
            } else {
                statusLabel.setText(localizer.text("status.searching"));
            }
        } else if (latestSummary != null) {
            renderSummary(latestSummary);
        } else {
            renderReadyState();
        }
    }

    private void changeLanguage(AppLanguage language) {
        if (language == null || language == currentLanguage) {
            return;
        }
        currentLanguage = language;
        localizer = Messages.localizer(language);
        Messages.saveLanguage(language);
        AppTheme.applyLanguage(language);
        applyThemeFonts();
        applyLocalizedText();
    }

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            rootField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void editExtensions() {
        Set<String> updated = ExtensionsEditorDialog.showDialog(this, configuredExtensions, localizer);
        if (updated != null) {
            configuredExtensions = updated;
            updateExtensionsSummary();
        }
    }

    private void updateExtensionsSummary() {
        extensionsCountLabel.setText(localizer.format("option.extensionsCount", configuredExtensions.size()));
        extensionsSummaryArea.setText(ExtensionFilterParser.format(configuredExtensions));
        extensionsSummaryArea.setCaretPosition(0);
    }

    private void handleRegexToggle() {
        boolean regexMode = regexBox.isSelected();
        boolean wholeWordSelected = wholeWordBox.isSelected();
        wholeWordBox.setEnabled(!regexMode);

        if (regexMode && wholeWordSelected) {
            showTransientNotice("notice.regexDisablesWholeWord");
        } else if (!regexMode) {
            clearTransientNotice();
        }
    }

    private void showTransientNotice(String key) {
        long now = System.currentTimeMillis();
        if (key.equals(activeNoticeKey) && now - lastNoticeAt < 1500) {
            return;
        }

        activeNoticeKey = key;
        lastNoticeAt = now;
        transientNoticeLabel.setText(localizer.text(key));
        transientNoticeLabel.setVisible(true);
        transientNoticeTimer.restart();
    }

    private void clearTransientNotice() {
        transientNoticeTimer.stop();
        transientNoticeLabel.setVisible(false);
        activeNoticeKey = null;
    }

    private void startSearch() {
        SearchOptions options;
        try {
            options = buildOptions();
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), localizer.text("message.invalidSearchOptionsTitle"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (currentWorker != null) {
            currentWorker.requestCancel();
        }

        latestProgress = null;
        latestSummary = null;
        matchTableModel.clear();
        skippedTableModel.clear();
        resultTabs.setSelectedIndex(0);
        setSearchingState(true);
        statusLabel.setText(localizer.text("status.searching"));
        detailLabel.setText(options.rootDirectory().toString());

        currentWorker = new SearchWorker(
                searchEngine,
                options,
                this::consumeEvents,
                this::handleSummary,
                this::handleFailure
        );
        currentWorker.execute();
    }

    private SearchOptions buildOptions() {
        String folderText = rootField.getText().trim();
        if (folderText.isEmpty()) {
            throw new IllegalArgumentException(localizer.text("message.invalidRootMissing"));
        }

        Path root = Paths.get(folderText);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            throw new IllegalArgumentException(localizer.text("message.invalidRootNotDirectory"));
        }

        String query = queryField.getText();
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(localizer.text("message.invalidQuery"));
        }

        SearchMode mode = regexBox.isSelected() ? SearchMode.REGEX : SearchMode.LITERAL;
        if (mode == SearchMode.REGEX) {
            Pattern.compile(query);
        }

        return new SearchOptions(
                root,
                query,
                mode,
                caseSensitiveBox.isSelected(),
                wholeWordBox.isSelected(),
                includeHiddenBox.isSelected(),
                configuredExtensions
        );
    }

    private void stopSearch() {
        if (currentWorker != null) {
            currentWorker.requestCancel();
            statusLabel.setText(localizer.text("status.stopping"));
        }
    }

    private void consumeEvents(List<SearchEvent> events) {
        List<SearchMatch> matchesToAdd = new java.util.ArrayList<>();
        List<SkippedPath> skippedToAdd = new java.util.ArrayList<>();
        SearchProgress progressToRender = null;

        for (SearchEvent event : events) {
            if (event instanceof SearchEvent.MatchFound matchFound) {
                matchesToAdd.add(matchFound.match());
            } else if (event instanceof SearchEvent.SkipFound skipFound) {
                skippedToAdd.add(skipFound.skippedPath());
            } else if (event instanceof SearchEvent.ProgressChanged progressChanged) {
                progressToRender = progressChanged.progress();
            }
        }

        matchTableModel.addMatches(matchesToAdd);
        skippedTableModel.addSkippedEntries(skippedToAdd);

        if (progressToRender != null) {
            latestProgress = progressToRender;
            renderProgress(progressToRender);
        }
    }

    private void renderProgress(SearchProgress progress) {
        statusLabel.setText(localizer.format(
                "status.progress",
                progress.filesVisited(),
                progress.filesSearched(),
                progress.matchesFound(),
                progress.skippedEntries()
        ));
        if (progress.currentPath() != null) {
            detailLabel.setText(progress.currentPath().toString());
        }
    }
    

    private void handleSummary(SearchSummary summary) {
        currentWorker = null;
        latestSummary = summary;
        latestProgress = null;
        setSearchingState(false);
        renderSummary(summary);
    }

    private void renderSummary(SearchSummary summary) {
        statusLabel.setText(localizer.format(
                summary.cancelled() ? "status.summary.cancelled" : "status.summary.completed",
                summary.filesVisited(),
                summary.filesSearched(),
                summary.matchesFound(),
                summary.skippedEntries(),
                summary.elapsedMillis()
        ));
        detailLabel.setText(summary.cancelled()
                ? localizer.text("status.detail.cancelled")
                : localizer.text("status.detail.next"));
    }

    private void handleFailure(Throwable throwable) {
        currentWorker = null;
        latestSummary = null;
        latestProgress = null;
        setSearchingState(false);
        statusLabel.setText(localizer.text("status.failed"));
        detailLabel.setText(throwable.getClass().getSimpleName());
        JOptionPane.showMessageDialog(
                this,
                throwable.getMessage() == null ? throwable.toString() : throwable.getMessage(),
                localizer.text("message.searchFailureTitle"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void setSearchingState(boolean searching) {
        searchButton.setEnabled(!searching);
        browseButton.setEnabled(!searching);
        editExtensionsButton.setEnabled(!searching);
        languageSelector.setEnabled(!searching);
        stopButton.setEnabled(searching);
    }

    private void renderReadyState() {
        statusLabel.setText(localizer.text("status.ready"));
        detailLabel.setText(localizer.text("status.detail.ready"));
    }

    private static final class ThemedTableCellRenderer extends DefaultTableCellRenderer {

        private final int alignment;

        private ThemedTableCellRenderer(int alignment) {
            this.alignment = alignment;
            setBorder(new EmptyBorder(0, 8, 0, 8));
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(alignment);
            setFont(AppTheme.tableFont());
            setForeground(AppTheme.TEXT);

            if (isSelected) {
                setBackground(AppTheme.SELECTION);
            } else {
                setBackground(row % 2 == 0 ? AppTheme.ELEVATED_WHITE : AppTheme.ROW_ALT);
            }

            return this;
        }
    }
}
