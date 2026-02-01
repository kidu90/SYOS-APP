package com.syos.application.factory;

import com.syos.domain.entity.StockBatch;
import com.syos.domain.valueobject.BatchNumber;
import com.syos.domain.valueobject.ProductId;
import java.time.LocalDate;

public class StockBatchFactory {
    public StockBatch createBatch(String batchNumber, String productId, int quantity,
                                   LocalDate expiryDate, LocalDate receivedDate) {
        return new StockBatch(
            new BatchNumber(batchNumber),
            new ProductId(productId),
            quantity,
            expiryDate,
            receivedDate
        );
    }

    public StockBatch createBatch(String batchNumber, String productId, int quantity, LocalDate expiryDate) {
        return createBatch(batchNumber, productId, quantity, expiryDate, LocalDate.now());
    }
}
