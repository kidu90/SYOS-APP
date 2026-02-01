package com.syos.domain.repository;

import com.syos.domain.entity.StockBatch;
import com.syos.domain.valueobject.BatchNumber;
import com.syos.domain.valueobject.ProductId;
import java.util.List;
import java.util.Optional;

public interface StockRepository {
    void save(StockBatch batch);
    Optional<StockBatch> findByBatchNumber(BatchNumber batchNumber);
    List<StockBatch> findByProductId(ProductId productId);
    List<StockBatch> findAll();
    List<StockBatch> findExpired();
    List<StockBatch> findExpiringSoon(int daysThreshold);
    int getTotalQuantityForProduct(ProductId productId);
    void update(StockBatch batch);
}
