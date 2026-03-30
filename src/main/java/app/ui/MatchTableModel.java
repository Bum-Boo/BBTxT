package app.ui;

import app.i18n.Localizer;
import app.model.SearchMatch;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public final class MatchTableModel extends AbstractTableModel {

    private String[] columns = {"Path", "Line", "Column", "Encoding", "Preview"};
    private final List<SearchMatch> rows = new ArrayList<>();

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        SearchMatch match = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> match.path().toString();
            case 1 -> match.lineNumber();
            case 2 -> match.columnNumber();
            case 3 -> match.charsetName();
            case 4 -> match.preview();
            default -> "";
        };
    }

    public void clear() {
        rows.clear();
        fireTableDataChanged();
    }

    public void addMatch(SearchMatch match) {
        int index = rows.size();
        rows.add(match);
        fireTableRowsInserted(index, index);
    }

    public void addMatches(List<SearchMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return;
        }

        int start = rows.size();
        rows.addAll(matches);
        fireTableRowsInserted(start, rows.size() - 1);
    }

    public void setLocalizer(Localizer localizer) {
        columns = new String[]{
                localizer.text("table.match.path"),
                localizer.text("table.match.line"),
                localizer.text("table.match.column"),
                localizer.text("table.match.encoding"),
                localizer.text("table.match.preview")
        };
        fireTableStructureChanged();
    }
}
