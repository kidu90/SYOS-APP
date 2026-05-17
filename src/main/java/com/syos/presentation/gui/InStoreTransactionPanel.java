package com.syos.presentation.gui;

import javax.swing.JPanel;

import com.syos.application.strategy.DiscountStrategy;
import com.syos.domain.entity.Bill;
import com.syos.domain.entity.Product;

public class InStoreTransactionPanel extends AbstractTransactionPanel {
    public InStoreTransactionPanel(SYOSGuiApplication app) {
        super(app, "In-Store Transaction");
    }

    @Override
    protected String processButtonText() {
        return "Process Sale";
    }

    @Override
    protected Bill.SaleType saleType() {
        return Bill.SaleType.IN_STORE;
    }

    @Override
    protected DiscountStrategy previewDiscountStrategy() {
        return app.getInStoreDiscountStrategy();
    }

    @Override
    protected String customerName() {
        return null;
    }

    @Override
    protected String customerAddress() {
        return null;
    }

    @Override
    protected boolean isAllowedProduct(Product product) {
        String id = product.getId().getValue();
        return "P001".equals(id) || "P002".equals(id) || "P003".equals(id);
    }

    @Override
    protected void onShow() {
        reloadProducts();
        updateTotals();
    }
}
