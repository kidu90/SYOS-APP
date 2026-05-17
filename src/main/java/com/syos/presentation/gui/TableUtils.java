package com.syos.presentation.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

public final class TableUtils {
    private TableUtils() {
    }

    public static void configureTable(JTable table) {
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new StripedRenderer());
    }

    private static class StripedRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                component.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 247, 250));
            } else {
                component.setBackground(new Color(214, 227, 248));
            }
            if (column == 1 || column == 2 || column == 3 || column == 4) {
                setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }
            return component;
        }
    }
}
