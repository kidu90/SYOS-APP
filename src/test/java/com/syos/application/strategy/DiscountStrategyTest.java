package com.syos.application.strategy;

import com.syos.domain.entity.Bill;
import com.syos.domain.entity.BillItem;
import com.syos.domain.valueobject.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DiscountStrategyTest {

    @Test
    void noDiscountStrategyShouldReturnZero() {
        Bill bill = createSampleBill(500.00);
        DiscountStrategy strategy = new NoDiscountStrategy();
        
        Money discount = strategy.calculateDiscount(bill);
        
        assertEquals(new Money(0), discount);
    }

    @Test
    void percentageDiscountStrategyShouldCalculateCorrectly() {
        Bill bill = createSampleBill(500.00);
        DiscountStrategy strategy = new PercentageDiscountStrategy(BigDecimal.valueOf(10));
        
        Money discount = strategy.calculateDiscount(bill);
        
        assertEquals(new Money(50.00), discount);
    }

    @Test
    void thresholdDiscountStrategyShouldApplyWhenThresholdMet() {
        Bill bill = createSampleBill(1500.00);
        DiscountStrategy strategy = new ThresholdDiscountStrategy(
            new Money(1000.00),
            new Money(100.00)
        );
        
        Money discount = strategy.calculateDiscount(bill);
        
        assertEquals(new Money(100.00), discount);
    }

    @Test
    void thresholdDiscountStrategyShouldNotApplyWhenThresholdNotMet() {
        Bill bill = createSampleBill(500.00);
        DiscountStrategy strategy = new ThresholdDiscountStrategy(
            new Money(1000.00),
            new Money(100.00)
        );
        
        Money discount = strategy.calculateDiscount(bill);
        
        assertEquals(new Money(0), discount);
    }

    private Bill createSampleBill(double totalAmount) {
        Bill bill = new Bill(
            new BillNumber("TEST-001"),
            LocalDateTime.now(),
            Bill.SaleType.IN_STORE
        );

        BillItem item = new BillItem(
            new ProductId("P001"),
            "Test Product",
            1,
            new Money(totalAmount),
            new BatchNumber("B001")
        );

        bill.addItem(item);
        return bill;
    }
}
