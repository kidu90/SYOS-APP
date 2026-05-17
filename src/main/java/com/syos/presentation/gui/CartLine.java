package com.syos.presentation.gui;

import com.syos.domain.valueobject.Money;

public class CartLine {
    private final String productId;
    private final String productName;
    private final String unit;
    private final Money unitPrice;
    private int quantity;

    public CartLine(String productId, String productName, String unit, Money unitPrice, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getUnit() {
        return unit;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void increase(int amount) {
        this.quantity += amount;
    }

    public Money getLineTotal() {
        return unitPrice.multiply(quantity);
    }
}
