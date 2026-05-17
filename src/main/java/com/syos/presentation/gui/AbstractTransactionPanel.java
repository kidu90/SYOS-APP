package com.syos.presentation.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import com.syos.application.strategy.DiscountStrategy;
import com.syos.application.usecase.CheckoutCommand;
import com.syos.domain.entity.Bill;
import com.syos.domain.entity.BillItem;
import com.syos.domain.entity.Product;
import com.syos.domain.repository.ProductRepository;
import com.syos.domain.valueobject.BatchNumber;
import com.syos.domain.valueobject.Money;
import com.syos.domain.valueobject.ProductId;

abstract class AbstractTransactionPanel extends JPanel {
    protected final SYOSGuiApplication app;
    protected final ProductRepository productRepository;
    protected final JPanel topPanel;
    protected final List<CartLine> cartLines = new ArrayList<>();
    protected final JComboBox<ProductOption> productCombo = new JComboBox<>();
    protected final JTextField quantityField = new JTextField(8);
    protected final DefaultTableModel cartModel;
    protected final JTable cartTable;
    protected final JLabel subtotalValue = new JLabel("Rs. 0.00");
    protected final JLabel discountValue = new JLabel("Rs. 0.00");
    protected final JLabel totalValue = new JLabel("Rs. 0.00");

    protected AbstractTransactionPanel(SYOSGuiApplication app, String title) {
        this.app = app;
        this.productRepository = app.getProductRepository();
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel header = new JPanel(new GridLayout(0, 1, 0, 8));
        header.add(new JLabel(title));

        topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.add(new JLabel("Product"));
        topPanel.add(productCombo);
        topPanel.add(new JLabel("Quantity"));
        topPanel.add(quantityField);
        JButton addToCartButton = new JButton("Add to Cart");
        addToCartButton.addActionListener(e -> addToCart());
        topPanel.add(addToCartButton);
        header.add(topPanel);
        add(header, BorderLayout.NORTH);

        cartModel = new DefaultTableModel(new Object[]{"Product Name", "Qty", "Unit Price", "Line Total", "Remove"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cartTable = new JTable(cartModel);
        TableUtils.configureTable(cartTable);
        TableButtonColumn removeColumn = new TableButtonColumn(cartTable, "Remove", e -> {
            int row = Integer.parseInt(e.getActionCommand());
            removeCartLine(row);
        });
        cartTable.getColumnModel().getColumn(4).setCellRenderer(removeColumn);
        cartTable.getColumnModel().getColumn(4).setCellEditor(removeColumn);

        JScrollPane scrollPane = new JScrollPane(cartTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 10));
        bottom.add(new JLabel("Subtotal"));
        bottom.add(subtotalValue);
        bottom.add(new JLabel("Discount"));
        bottom.add(discountValue);
        bottom.add(new JLabel("Total"));
        bottom.add(totalValue);
        JButton processButton = new JButton(processButtonText());
        processButton.addActionListener(e -> processSale());
        bottom.add(processButton);
        add(bottom, BorderLayout.SOUTH);

        refreshTableButtons();
    }

    protected JPanel buildExtraTopPanel() {
        return null;
    }

    protected final void attachExtraTopPanel() {
        JPanel extraTop = buildExtraTopPanel();
        if (extraTop != null) {
            topPanel.add(extraTop);
        }
    }

    protected abstract String processButtonText();

    protected abstract Bill.SaleType saleType();

    protected abstract DiscountStrategy previewDiscountStrategy();

    protected abstract String customerName();

    protected abstract String customerAddress();

    protected abstract boolean isAllowedProduct(Product product);

    protected abstract void onShow();

    protected void reloadProducts() {
        new SwingWorker<List<ProductOption>, Void>() {
            @Override
            protected List<ProductOption> doInBackground() {
                return productRepository.findAll().stream()
                    .filter(AbstractTransactionPanel.this::isAllowedProduct)
                    .map(ProductOption::new)
                    .collect(Collectors.toList());
            }

            @Override
            protected void done() {
                try {
                    List<ProductOption> options = get();
                    productCombo.removeAllItems();
                    for (ProductOption option : options) {
                        productCombo.addItem(option);
                    }
                } catch (Exception ex) {
                    app.showError("Load Products", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                }
            }
        }.execute();
    }

    protected void addToCart() {
        ProductOption option = (ProductOption) productCombo.getSelectedItem();
        String qtyText = quantityField.getText().trim();
        if (option == null) {
            app.showError("Validation Error", "Please select a product");
            return;
        }
        if (qtyText.isBlank()) {
            app.showError("Validation Error", "Quantity cannot be empty");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(qtyText);
        } catch (NumberFormatException ex) {
            app.showError("Validation Error", "Quantity must be a positive integer");
            return;
        }

        if (quantity <= 0) {
            app.showError("Validation Error", "Quantity must be a positive integer");
            return;
        }

        Product product = productRepository.findById(new ProductId(option.getId())).orElse(null);
        if (product == null || !isAllowedProduct(product)) {
            app.showError("Validation Error", "Product is not available for this transaction type");
            return;
        }

        CartLine existing = cartLines.stream()
            .filter(line -> line.getProductId().equals(option.getId()))
            .findFirst()
            .orElse(null);
        if (existing != null) {
            existing.increase(quantity);
        } else {
            cartLines.add(new CartLine(option.getId(), option.getName(), option.getUnit(), new Money(option.getPrice()), quantity));
        }
        rebuildCartTable();
        updateTotals();
        quantityField.setText("");
    }

    protected void removeCartLine(int row) {
        if (row < 0 || row >= cartLines.size()) {
            return;
        }
        cartLines.remove(row);
        rebuildCartTable();
        updateTotals();
    }

    protected void rebuildCartTable() {
        cartModel.setRowCount(0);
        for (CartLine line : cartLines) {
            cartModel.addRow(new Object[]{
                line.getProductName(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getLineTotal(),
                "Remove"
            });
        }
        refreshTableButtons();
    }

    protected void refreshTableButtons() {
        if (cartTable.getColumnModel().getColumnCount() >= 5) {
            TableButtonColumn removeColumn = new TableButtonColumn(cartTable, "Remove", e -> {
                int row = Integer.parseInt(e.getActionCommand());
                removeCartLine(row);
            });
            cartTable.getColumnModel().getColumn(4).setCellRenderer(removeColumn);
            cartTable.getColumnModel().getColumn(4).setCellEditor(removeColumn);
        }
    }

    protected void updateTotals() {
        Bill previewBill = new Bill(new com.syos.domain.valueobject.BillNumber("PREVIEW"), LocalDateTime.now(), saleType());
        for (CartLine line : cartLines) {
            previewBill.addItem(new BillItem(new ProductId(line.getProductId()), line.getProductName(), line.getQuantity(), line.getUnitPrice(), new BatchNumber("PREVIEW")));
        }
        Money discount = previewDiscountStrategy().calculateDiscount(previewBill);
        previewBill.applyDiscount(discount);
        subtotalValue.setText(previewBill.getSubtotal().toString());
        discountValue.setText(previewBill.getDiscount().toString());
        totalValue.setText(previewBill.getTotal().toString());
    }

    protected void processSale() {
        if (cartLines.isEmpty()) {
            app.showError("Checkout Error", "Cart is empty");
            return;
        }

        new SwingWorker<Bill, Void>() {
            @Override
            protected Bill doInBackground() {
                Map<String, Integer> cartMap = new HashMap<>();
                for (CartLine line : cartLines) {
                    cartMap.put(line.getProductId(), line.getQuantity());
                }
                CheckoutCommand checkoutCommand = new CheckoutCommand(
                    app.getProductRepository(),
                    app.getInventoryManager(),
                    app.getBillRepository(),
                    app.getBillNumberService(),
                    app.getBillCalculationService(),
                    app.getStockSelectionStrategy(),
                    previewDiscountStrategy(),
                    cartMap,
                    saleType(),
                    customerName(),
                    customerAddress()
                );
                checkoutCommand.execute();
                return checkoutCommand.getGeneratedBill();
            }

            @Override
            protected void done() {
                try {
                    Bill bill = get();
                    app.showReceipt(bill);
                    cartLines.clear();
                    rebuildCartTable();
                    updateTotals();
                    app.setStatusMessage("Transaction completed successfully");
                    onSaleCompleted();
                } catch (Exception ex) {
                    app.showError("Checkout Error", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                }
            }
        }.execute();
    }

    protected void onSaleCompleted() {
        // default no-op
    }
}
