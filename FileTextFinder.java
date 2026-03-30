import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * FileTextFinder
 *
 * 기능:
 * 1) 폴더 선택
 * 2) 입력한 문자열을 하위 폴더까지 재귀 검색
 * 3) 어떤 파일의 몇 번째 줄(line), 몇 번째 칸(column)에 있는지 표시
 * 4) 일치한 줄의 미리보기 제공
 *
 * 실행:
 *   javac FileTextFinder.java
 *   java FileTextFinder
 */
public class FileTextFinder extends JFrame {

    private static final Set<String> KNOWN_TEXT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "txt", "java", "kt", "kts", "xml", "html", "htm", "css", "js", "ts", "jsx", "tsx",
            "json", "md", "yaml", "yml", "properties", "gradle", "sql", "csv", "log", "ini",
            "bat", "cmd", "py", "c", "cpp", "h", "hpp", "cs", "php", "go", "rs", "swift"
    ));

    private final JTextField folderField = new JTextField();
    private final JTextField keywordField = new JTextField();
    private final JCheckBox caseSensitiveBox = new JCheckBox("대소문자 구분", false);
    private final JCheckBox wholeWordBox = new JCheckBox("완전 일치 단어만", false);
    private final JCheckBox includeHiddenBox = new JCheckBox("숨김 파일 포함", false);
    private final JLabel statusLabel = new JLabel("대기 중");
    private final JButton searchButton = new JButton("검색");
    private final JButton stopButton = new JButton("중지");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"파일", "줄", "칸", "미리보기"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable resultTable = new JTable(tableModel);
    private SearchWorker currentWorker;

    public FileTextFinder() {
        super("파일 텍스트 탐색기");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 650);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(buildFolderPanel());
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(buildSearchPanel());

        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(430);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        resultTable.getColumnModel().getColumn(3).setPreferredWidth(500);
        resultTable.setRowHeight(24);

        JScrollPane scrollPane = new JScrollPane(resultTable);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        root.add(topPanel, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);
        root.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(root);

        searchButton.addActionListener(e -> startSearch());
        stopButton.addActionListener(e -> stopSearch());
        stopButton.setEnabled(false);
    }

    private JPanel buildFolderPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JLabel label = new JLabel("검색 폴더:");
        JButton browseButton = new JButton("폴더 선택");

        browseButton.addActionListener(e -> chooseFolder());

        panel.add(label, BorderLayout.WEST);
        panel.add(folderField, BorderLayout.CENTER);
        panel.add(browseButton, BorderLayout.EAST);

        return panel;
    }

    private JPanel buildSearchPanel() {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        JPanel row1 = new JPanel(new BorderLayout(8, 8));
        row1.add(new JLabel("검색어:"), BorderLayout.WEST);
        row1.add(keywordField, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsPanel.add(searchButton);
        buttonsPanel.add(stopButton);
        row1.add(buttonsPanel, BorderLayout.EAST);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row2.add(caseSensitiveBox);
        row2.add(wholeWordBox);
        row2.add(includeHiddenBox);

        wrapper.add(row1);
        wrapper.add(Box.createVerticalStrut(8));
        wrapper.add(row2);

        return wrapper;
    }

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            folderField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void startSearch() {
        String folderText = folderField.getText().trim();
        String keyword = keywordField.getText();

        if (folderText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "검색할 폴더를 선택하세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (keyword == null || keyword.isBlank()) {
            JOptionPane.showMessageDialog(this, "검색어를 입력하세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path rootPath = Paths.get(folderText);
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            JOptionPane.showMessageDialog(this, "유효한 폴더가 아닙니다.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        tableModel.setRowCount(0);
        statusLabel.setText("검색 준비 중...");
        searchButton.setEnabled(false);
        stopButton.setEnabled(true);

        currentWorker = new SearchWorker(
                rootPath,
                keyword,
                caseSensitiveBox.isSelected(),
                wholeWordBox.isSelected(),
                includeHiddenBox.isSelected()
        );
        currentWorker.execute();
    }

    private void stopSearch() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
            statusLabel.setText("검색 중지 요청됨");
        }
    }

    private class SearchWorker extends SwingWorker<SearchSummary, SearchResult> {
        private final Path rootPath;
        private final String keyword;
        private final boolean caseSensitive;
        private final boolean wholeWord;
        private final boolean includeHidden;

        private final AtomicInteger fileCount = new AtomicInteger();
        private final AtomicInteger hitCount = new AtomicInteger();

        SearchWorker(Path rootPath, String keyword, boolean caseSensitive, boolean wholeWord, boolean includeHidden) {
            this.rootPath = rootPath;
            this.keyword = keyword;
            this.caseSensitive = caseSensitive;
            this.wholeWord = wholeWord;
            this.includeHidden = includeHidden;
        }

        @Override
        protected SearchSummary doInBackground() {
            try (Stream<Path> pathStream = Files.walk(rootPath)) {
                pathStream
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            if (isCancelled()) return;

                            try {
                                if (!includeHidden && Files.isHidden(path)) {
                                    return;
                                }
                            } catch (IOException ignored) {
                            }

                            fileCount.incrementAndGet();
                            setProgressSafely();
                            searchInFile(path);
                        });
            } catch (IOException e) {
                return new SearchSummary(fileCount.get(), hitCount.get(), "폴더를 읽는 중 오류: " + e.getMessage(), true);
            }

            return new SearchSummary(fileCount.get(), hitCount.get(), isCancelled() ? "검색이 중지되었습니다." : "검색 완료", false);
        }
        private void searchInFile(Path file) {
            if (isProbablyBinary(file)) {
                return;
            }

            Charset detected = detectCharset(file);
            if (detected != null) {
                try {
                    searchFileWithCharset(file, detected);
                    return;
                } catch (IOException ignored) {
                }
            }

            Charset[] fallbacks = new Charset[]{
                    StandardCharsets.UTF_8,
                    Charset.forName("MS949"),
                    Charset.forName("EUC-KR"),
                    StandardCharsets.UTF_16LE,
                    StandardCharsets.UTF_16BE,
                    StandardCharsets.ISO_8859_1
            };

            for (Charset charset : fallbacks) {
                try {
                    searchFileWithCharset(file, charset);
                    return;
                } catch (MalformedInputException ignored) {
                } catch (IOException ignored) {
                }
            }
        }

        private Charset detectCharset(Path file) {
            try {
                byte[] bom = new byte[Math.min((int) Files.size(file), 4)];
                try (var in = Files.newInputStream(file)) {
                    int read = in.read(bom);
                    if (read >= 3 && (bom[0] & 0xFF) == 0xEF && (bom[1] & 0xFF) == 0xBB && (bom[2] & 0xFF) == 0xBF) {
                        return StandardCharsets.UTF_8;
                    }
                    if (read >= 2 && (bom[0] & 0xFF) == 0xFF && (bom[1] & 0xFF) == 0xFE) {
                        return StandardCharsets.UTF_16LE;
                    }
                    if (read >= 2 && (bom[0] & 0xFF) == 0xFE && (bom[1] & 0xFF) == 0xFF) {
                        return StandardCharsets.UTF_16BE;
                    }
                }
            } catch (IOException ignored) {
            }
            return null;
        }

        private void searchFileWithCharset(Path file, Charset charset) throws IOException {
            try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
                String line;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {
                    if (isCancelled()) return;
                    lineNumber++;

                    List<Integer> matches = findMatches(line, keyword, caseSensitive, wholeWord);
                    for (int columnIndex : matches) {
                        hitCount.incrementAndGet();
                        publish(new SearchResult(file.toString(), lineNumber, columnIndex + 1, shorten(line, 220)));
                    }
                }
            }
        }
        private boolean isProbablyBinary(Path file) {
            String fileName = file.getFileName().toString();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex >= 0) {
                String ext = fileName.substring(dotIndex + 1).toLowerCase();
                if (KNOWN_TEXT_EXTENSIONS.contains(ext)) {
                    return false;
                }
            }

            try {
                BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                if (attrs.size() == 0) return false;

                int sampleSize = (int) Math.min(attrs.size(), 1024);
                byte[] bytes = new byte[sampleSize];
                int len;
                try (var in = Files.newInputStream(file)) {
                    len = in.read(bytes);
                }
                if (len <= 0) return false;

                if (len >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
                    return false;
                }
                if (len >= 2 && (((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) || ((bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF))) {
                    return false;
                }

                int suspicious = 0;
                for (int i = 0; i < len; i++) {
                    byte b = bytes[i];
                    if (b == 0) return true;
                    if (b < 0x09) suspicious++;
                }
                return suspicious > len / 5;
            } catch (IOException e) {
                return true;
            }
        }

        private void setProgressSafely() {
            SwingUtilities.invokeLater(() ->
                    statusLabel.setText("검색 중... 파일 " + fileCount.get() + "개 확인, 결과 " + hitCount.get() + "건")
            );
        }

        @Override
        protected void process(List<SearchResult> chunks) {
            for (SearchResult result : chunks) {
                Vector<Object> row = new Vector<>();
                row.add(result.filePath());
                row.add(result.lineNumber());
                row.add(result.columnNumber());
                row.add(result.preview());
                tableModel.addRow(row);
            }
            statusLabel.setText("검색 중... 파일 " + fileCount.get() + "개 확인, 결과 " + hitCount.get() + "건");
        }

        @Override
        protected void done() {
            searchButton.setEnabled(true);
            stopButton.setEnabled(false);

            try {
                SearchSummary summary = get();
                statusLabel.setText(summary.message() + " | 확인 파일: " + summary.fileCount() + "개, 결과: " + summary.hitCount() + "건");
                if (summary.hasError()) {
                    JOptionPane.showMessageDialog(FileTextFinder.this, summary.message(), "오류", JOptionPane.ERROR_MESSAGE);
                }
            } catch (InterruptedException | ExecutionException e) {
                statusLabel.setText("검색 중 오류 발생: " + e.getMessage());
                JOptionPane.showMessageDialog(FileTextFinder.this, "검색 중 오류가 발생했습니다.\n" + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            } catch (java.util.concurrent.CancellationException e) {
                statusLabel.setText("검색이 중지되었습니다. | 확인 파일: " + fileCount.get() + "개, 결과: " + hitCount.get() + "건");
            }
        }
    }

    private static List<Integer> findMatches(String line, String keyword, boolean caseSensitive, boolean wholeWord) {
        java.util.ArrayList<Integer> matches = new java.util.ArrayList<>();

        String source = caseSensitive ? line : line.toLowerCase();
        String target = caseSensitive ? keyword : keyword.toLowerCase();

        int fromIndex = 0;
        while (fromIndex <= source.length() - target.length()) {
            int found = source.indexOf(target, fromIndex);
            if (found < 0) break;

            if (!wholeWord || isWholeWordMatch(line, found, keyword.length())) {
                matches.add(found);
            }
            fromIndex = found + Math.max(1, target.length());
        }

        return matches;
    }

    private static boolean isWholeWordMatch(String line, int start, int length) {
        int end = start + length;
        boolean leftOk = start == 0 || !Character.isLetterOrDigit(line.charAt(start - 1)) && line.charAt(start - 1) != '_';
        boolean rightOk = end == line.length() || !Character.isLetterOrDigit(line.charAt(end)) && line.charAt(end) != '_';
        return leftOk && rightOk;
    }

    private static String shorten(String text, int maxLength) {
        if (text == null) return "";
        String normalized = text.replace('\t', ' ').trim();
        if (normalized.length() <= maxLength) return normalized;
        return normalized.substring(0, maxLength - 3) + "...";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new FileTextFinder().setVisible(true);
        });
    }

    record SearchResult(String filePath, int lineNumber, int columnNumber, String preview) {}

    record SearchSummary(int fileCount, int hitCount, String message, boolean hasError) {}
}
