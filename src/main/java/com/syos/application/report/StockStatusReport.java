package com.syos.application.report;

import com.syos.domain.entity.Product;
import com.syos.domain.entity.StockBatch;
import com.syos.domain.repository.ProductRepository;
import com.syos.domain.repository.StockRepository;
import com.syos.domain.valueobject.ProductId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StockStatusReport extends ReportGenerator {
    private final StockRepository stockRepository;
    private final ProductRepository productRepository;

    public StockStatusReport(StockRepository stockRepository, ProductRepository productRepository) {
        this.stockRepository = stockRepository;
        this.productRepository = productRepository;
    }

    @Override
    protected String getReportHeader() {
        return "=".repeat(80) + "\n" +
               "STOCK STATUS REPORT - SYNEX OUTLET STORE\n" +
               "=".repeat(80);
    }

    @Override
    protected String generateReportBody() {
        List<StockBatch> allBatches = stockRepository.findAll();
        List<Product> allProducts = productRepository.findAll();

        Map<ProductId, List<StockBatch>> batchesByProduct = allBatches.stream()
            .collect(Collectors.groupingBy(StockBatch::getProductId));

        StringBuilder body = new StringBuilder();
        body.append("\nCURRENT STOCK LEVELS:\n");
        body.append("-".repeat(80)).append("\n");
        body.append(String.format("%-10s %-30s %-15s %-10s%n", "Product ID", "Name", "Total Qty", "Batches"));
        body.append("-".repeat(80)).append("\n");

        for (Product product : allProducts) {
            int totalQty = stockRepository.getTotalQuantityForProduct(product.getId());
            int batchCount = batchesByProduct.getOrDefault(product.getId(), List.of()).size();
            
            body.append(String.format("%-10s %-30s %-15d %-10d%n",
                product.getId(),
                product.getName(),
                totalQty,
                batchCount));
        }

        List<StockBatch> expiringSoon = stockRepository.findExpiringSoon(7);
        if (!expiringSoon.isEmpty()) {
            body.append("\nEXPIRING SOON (within 7 days):\n");
            body.append("-".repeat(80)).append("\n");
            body.append(String.format("%-15s %-10s %-15s %-15s%n", "Batch", "Product", "Quantity", "Expiry Date"));
            body.append("-".repeat(80)).append("\n");

            for (StockBatch batch : expiringSoon) {
                body.append(String.format("%-15s %-10s %-15d %-15s%n",
                    batch.getBatchNumber(),
                    batch.getProductId(),
                    batch.getQuantity(),
                    batch.getExpiryDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
            }
        }

        List<StockBatch> expired = stockRepository.findExpired();
        if (!expired.isEmpty()) {
            body.append("\nEXPIRED STOCK:\n");
            body.append("-".repeat(80)).append("\n");
            body.append(String.format("%-15s %-10s %-15s %-15s%n", "Batch", "Product", "Quantity", "Expiry Date"));
            body.append("-".repeat(80)).append("\n");

            for (StockBatch batch : expired) {
                body.append(String.format("%-15s %-10s %-15d %-15s%n",
                    batch.getBatchNumber(),
                    batch.getProductId(),
                    batch.getQuantity(),
                    batch.getExpiryDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
            }
        }

        return body.toString();
    }
}
