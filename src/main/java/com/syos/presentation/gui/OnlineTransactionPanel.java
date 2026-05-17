package com.syos.presentation.gui;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.syos.application.strategy.DiscountStrategy;
import com.syos.domain.entity.Bill;
import com.syos.domain.entity.Product;

public class OnlineTransactionPanel extends AbstractTransactionPanel {
    private final JTextField addressField = new JTextField(24);
    private final JLabel customerNameLabel = new JLabel();

    public OnlineTransactionPanel(SYOSGuiApplication app) {
        super(app, "Online Transaction");
        attachExtraTopPanel();
    }

    @Override
    protected JPanel buildExtraTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.add(new JLabel("Customer"));
        panel.add(customerNameLabel);
        panel.add(new JLabel("Delivery Address"));
        panel.add(addressField);
        return panel;
    }

    @Override
    protected String processButtonText() {
        return "Place Order";
    }

    @Override
    protected Bill.SaleType saleType() {
        return Bill.SaleType.ONLINE;
    }

    @Override
    protected DiscountStrategy previewDiscountStrategy() {
        return app.getOnlineDiscountStrategy();
    }

    @Override
    protected String customerName() {
        return app.getCurrentUser() == null ? null : app.getCurrentUser().getFullName();
    }

    @Override
    protected String customerAddress() {
        return addressField.getText().trim();
    }

    @Override
    protected boolean isAllowedProduct(Product product) {
        String id = product.getId().getValue();
        return "P001".equals(id) || "P004".equals(id) || "P005".equals(id);
    }

    @Override
    protected void onShow() {
        if (app.getCurrentUser() != null) {
            customerNameLabel.setText(app.getCurrentUser().getFullName());
            addressField.setText(app.getCurrentUser().getAddress());
        }
        reloadProducts();
        updateTotals();
    }

    @Override
    protected void onSaleCompleted() {
        if (app.getCurrentUser() != null) {
            addressField.setText(app.getCurrentUser().getAddress());
        }
    }
}
