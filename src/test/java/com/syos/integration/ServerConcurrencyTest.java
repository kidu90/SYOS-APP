package com.syos.integration;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.syos.application.factory.ProductFactory;
import com.syos.application.factory.StockBatchFactory;
import com.syos.application.service.BillCalculationService;
import com.syos.application.service.BillNumberGenerator;
import com.syos.application.service.BillNumberService;
import com.syos.application.service.InventoryManager;
import com.syos.application.strategy.FEFOStockSelectionStrategy;
import com.syos.application.strategy.NoDiscountStrategy;
import com.syos.application.strategy.StockSelectionStrategy;
import com.syos.application.usecase.AddProductUseCase;
import com.syos.application.usecase.CheckoutCommand;
import com.syos.domain.entity.Bill;
import com.syos.domain.entity.Product;
import com.syos.domain.entity.StockBatch;
import com.syos.domain.valueobject.InventoryChannel;
import com.syos.domain.valueobject.ProductId;
import com.syos.infrastructure.database.DatabaseManager;
import com.syos.infrastructure.persistence.SQLiteBillRepository;
import com.syos.infrastructure.persistence.SQLiteProductRepository;
import com.syos.infrastructure.persistence.SQLiteStockRepository;

public class ServerConcurrencyTest {
    private DatabaseManager dbManager;

    @BeforeEach
    public void setup() {
        DatabaseManager.resetInstance();
        dbManager = DatabaseManager.getInstance("test_concurrency.db");
    }

    @AfterEach
    public void cleanup() {
        dbManager.resetDatabase();
    }

    @Test
    public void concurrentCheckoutsReduceStockCorrectly() throws Exception {
        SQLiteProductRepository productRepo = new SQLiteProductRepository(dbManager);
        SQLiteStockRepository stockRepo = new SQLiteStockRepository(dbManager);
        SQLiteBillRepository billRepo = new SQLiteBillRepository(dbManager);

        ProductFactory productFactory = new ProductFactory();
        StockBatchFactory stockBatchFactory = new StockBatchFactory();

        // add product
        Product p = productFactory.createProduct("PT1", "Test Prod", "Test", 10.0, "unit");
        AddProductUseCase addProductUseCase = new AddProductUseCase(productRepo);
        addProductUseCase.execute(p);

        // add stock batch with 20 units
        StockBatch batch = stockBatchFactory.createBatch("BTEST", "PT1", InventoryChannel.STORE, 20, LocalDate.now().plusDays(30));
        InventoryManager inventoryManager = new InventoryManager(stockRepo, stockRepo);
        inventoryManager.addStock(batch, InventoryChannel.STORE);

        BillNumberService billNumberService = BillNumberGenerator.getInstance();
        BillCalculationService billCalculationService = new BillCalculationService();
        StockSelectionStrategy stockSelectionStrategy = new FEFOStockSelectionStrategy();
        NoDiscountStrategy discountStrategy = new NoDiscountStrategy();

        int concurrentRequests = 10;
        ExecutorService ex = Executors.newFixedThreadPool(8);

        CompletableFuture<?>[] futures = new CompletableFuture[concurrentRequests];
        for (int i = 0; i < concurrentRequests; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                Map<String,Integer> cart = new HashMap<>();
                cart.put("PT1", 1);
                CheckoutCommand checkout = new CheckoutCommand(productRepo, inventoryManager, billRepo, billNumberService, billCalculationService, stockSelectionStrategy, discountStrategy, cart, Bill.SaleType.IN_STORE, "Tester", "Addr");
                checkout.execute();
                Bill bill = checkout.getGeneratedBill();
                Assertions.assertNotNull(bill);
            }, ex);
        }

        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        ex.shutdownNow();

        // Check remaining quantity
        int remaining = stockRepo.getTotalQuantityForProductAndChannel(new ProductId("PT1"), InventoryChannel.STORE);
        Assertions.assertEquals(20 - concurrentRequests, remaining);
    }
}
