package com.syos.application.usecase;

import com.syos.application.builder.BillBuilder;
import com.syos.application.service.BillNumberGenerator;
import com.syos.application.strategy.DiscountStrategy;
import com.syos.application.strategy.StockSelectionStrategy;
import com.syos.domain.entity.Bill;
import com.syos.domain.entity.BillItem;
import com.syos.domain.entity.Product;
import com.syos.domain.entity.StockBatch;
import com.syos.domain.repository.BillRepository;
import com.syos.domain.repository.ProductRepository;
import com.syos.domain.repository.StockRepository;
import com.syos.domain.valueobject.Money;
import com.syos.domain.valueobject.ProductId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutCommand implements Command {
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final BillRepository billRepository;
    private final StockSelectionStrategy stockSelectionStrategy;
    private final DiscountStrategy discountStrategy;
    private final Map<String, Integer> cartItems;
    private final Bill.SaleType saleType;
    private final String customerName;
    private final String customerAddress;
    private Bill generatedBill;

    public CheckoutCommand(ProductRepository productRepository,
                          StockRepository stockRepository,
                          BillRepository billRepository,
                          StockSelectionStrategy stockSelectionStrategy,
                          DiscountStrategy discountStrategy,
                          Map<String, Integer> cartItems,
                          Bill.SaleType saleType,
                          String customerName,
                          String customerAddress) {
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.billRepository = billRepository;
        this.stockSelectionStrategy = stockSelectionStrategy;
        this.discountStrategy = discountStrategy;
        this.cartItems = new HashMap<>(cartItems);
        this.saleType = saleType;
        this.customerName = customerName;
        this.customerAddress = customerAddress;
    }

    @Override
    public void execute() {
        String prefix = saleType == Bill.SaleType.ONLINE ? "ONL" : "POS";
        String billNumber = BillNumberGenerator.getInstance().generateBillNumber(prefix);

        BillBuilder builder = new BillBuilder()
            .withBillNumber(billNumber)
            .withSaleType(saleType);

        if (saleType == Bill.SaleType.ONLINE) {
            builder.forOnline(customerName, customerAddress);
        }

        Bill bill = builder.build();

        for (Map.Entry<String, Integer> entry : cartItems.entrySet()) {
            String productIdStr = entry.getKey();
            int quantity = entry.getValue();

            ProductId productId = new ProductId(productIdStr);
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productIdStr));

            List<StockBatch> availableBatches = stockRepository.findByProductId(productId);
            List<StockBatch> selectedBatches = stockSelectionStrategy.selectBatches(availableBatches, quantity);

            int remainingQuantity = quantity;
            for (StockBatch batch : selectedBatches) {
                int quantityToTake = Math.min(remainingQuantity, batch.getQuantity());
                
                BillItem item = new BillItem(
                    productId,
                    product.getName(),
                    quantityToTake,
                    product.getUnitPrice(),
                    batch.getBatchNumber()
                );
                
                bill.addItem(item);
                batch.reduceQuantity(quantityToTake);
                stockRepository.update(batch);
                
                remainingQuantity -= quantityToTake;
            }
        }

        Money discount = discountStrategy.calculateDiscount(bill);
        bill.applyDiscount(discount);

        billRepository.save(bill);
        this.generatedBill = bill;
    }

    public Bill getGeneratedBill() {
        return generatedBill;
    }
}
