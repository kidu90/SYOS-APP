package com.syos.presentation.gui;

import com.syos.domain.entity.Product;

public class ProductOption {
    private final String id;
    private final String name;
    private final String category;
    private final String unit;
    private final double price;

    public ProductOption(Product product) {
        this.id = product.getId().getValue();
        this.name = product.getName();
        this.category = product.getCategory();
        this.unit = product.getUnit();
        this.price = product.getUnitPrice().getAmount().doubleValue();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getUnit() {
        return unit;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return String.format("%s - %s (₹%.2f/%s)", id, name, price, unit);
    }
}
