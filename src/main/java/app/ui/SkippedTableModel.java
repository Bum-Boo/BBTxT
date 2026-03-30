package app.ui;

import app.i18n.Localizer;
import app.model.SkippedPath;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public final class SkippedTableModel extends AbstractTableModel {

    private String[] columns = {"Path", "Kind", "Reason", "Detail"};
    private final List<SkippedPath> rows = new ArrayList<>();
    private Localizer localizer;

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
        SkippedPath skipped = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> skipped.path().toString();
            case 1 -> localizer == null ? skipped.kind().name() : localizer.text(skipped.kind().messageKey());
            case 2 -> localizer == null ? skipped.reason().name() : localizer.text(skipped.reason().messageKey());
            case 3 -> skipped.detail();
            default -> "";
        };
    }

    public void clear() {
        rows.clear();
        fireTableDataChanged();
    }

    public void addSkipped(SkippedPath skippedPath) {
        int index = rows.size();
        rows.add(skippedPath);
        fireTableRowsInserted(index, index);
    }

    public void addSkippedEntries(List<SkippedPath> skippedPaths) {
        if (skippedPaths == null || skippedPaths.isEmpty()) {
            return;
        }

        int start = rows.size();
        rows.addAll(skippedPaths);
        fireTableRowsInserted(start, rows.size() - 1);
    }

    public void setLocalizer(Localizer localizer) {
        this.localizer = localizer;
        columns = new String[]{
                localizer.text("table.skipped.path"),
                localizer.text("table.skipped.kind"),
                localizer.text("table.skipped.reason"),
                localizer.text("table.skipped.detail")
        };
        fireTableStructureChanged();
    }
}
