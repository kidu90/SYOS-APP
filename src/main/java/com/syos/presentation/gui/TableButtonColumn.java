package com.syos.presentation.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class TableButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener {
    private final JTable table;
    private final JButton renderButton;
    private final JButton editButton;
    private final ActionListener callback;
    private int editingRow = -1;

    public TableButtonColumn(JTable table, String label, ActionListener callback) {
        this.table = table;
        this.callback = callback;
        this.renderButton = new JButton(label);
        this.editButton = new JButton(label);
        this.editButton.addActionListener(this);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return renderButton;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        editingRow = row;
        return editButton;
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        fireEditingStopped();
        callback.actionPerformed(new ActionEvent(table, ActionEvent.ACTION_PERFORMED, Integer.toString(editingRow)));
    }
}
