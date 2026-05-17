package com.syos.presentation.gui;

import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

public class ReportsPanel extends JPanel {
    private final SYOSGuiApplication app;

    public ReportsPanel(SYOSGuiApplication app) {
        this.app = app;
        setLayout(new FlowLayout(FlowLayout.LEFT, 12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JButton dailySales = new JButton("Daily Sales Report");
        dailySales.addActionListener(e -> loadDailySales());

        JButton stockStatus = new JButton("Stock Status Report");
        stockStatus.addActionListener(e -> loadStockStatus());

        add(dailySales);
        add(stockStatus);
    }

    public void onShow() {
        // No-op. Buttons fetch the reports on demand using background workers.
    }

    private void loadDailySales() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return app.getDailySalesReport().generateReport();
            }

            @Override
            protected void done() {
                try {
                    app.showReportText("Daily Sales Report", get());
                } catch (Exception ex) {
                    app.showError("Reports", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                }
            }
        }.execute();
    }

    private void loadStockStatus() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return app.getStockStatusReport().generateReport();
            }

            @Override
            protected void done() {
                try {
                    app.showReportText("Stock Status Report", get());
                } catch (Exception ex) {
                    app.showError("Reports", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                }
            }
        }.execute();
    }
}
