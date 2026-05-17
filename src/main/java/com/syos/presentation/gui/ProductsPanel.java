package com.syos.presentation.gui;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import com.syos.domain.entity.Product;

public class ProductsPanel extends JPanel {
    private final SYOSGuiApplication app;
    private final DefaultTableModel model;

    public ProductsPanel(SYOSGuiApplication app) {
        this.app = app;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        model = new DefaultTableModel(new Object[]{"ID", "Name", "Category", "Price", "Unit"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        TableUtils.configureTable(table);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void onShow() {
        new SwingWorker<List<Product>, Void>() {
            @Override
            protected List<Product> doInBackground() {
                return app.getProductRepository().findAll();
            }

            @Override
            protected void done() {
                try {
                    List<Product> products = get();
                    model.setRowCount(0);
                    for (Product product : products) {
                        model.addRow(new Object[]{
                            product.getId().getValue(),
                            product.getName(),
                            product.getCategory(),
                            product.getUnitPrice(),
                            product.getUnit()
                        });
                    }
                } catch (Exception ex) {
                    app.showError("View Products", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                }
            }
        }.execute();
    }
}
