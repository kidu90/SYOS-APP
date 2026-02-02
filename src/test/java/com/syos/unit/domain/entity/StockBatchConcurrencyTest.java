package com.syos.unit.domain.entity;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.syos.domain.entity.StockBatch;
import com.syos.domain.valueobject.BatchNumber;
import com.syos.domain.valueobject.InventoryChannel;
import com.syos.domain.valueobject.ProductId;

class StockBatchConcurrencyTest {

    @Test
    void shouldUpdateQuantitySafelyUnderConcurrentReductions() throws InterruptedException {
        StockBatch batch = new StockBatch(
            new BatchNumber("B-CONC-1"),
            new ProductId("P-CONC-1"),
            InventoryChannel.STORE,
            1000,
            LocalDate.now().plusDays(10),
            LocalDate.now()
        );

        int threads = 10;
        int perThread = 50;
        int totalReductions = threads * perThread;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < perThread; i++) {
                        batch.reduceQuantity(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertTrue(finished, "Concurrent reductions did not finish in time");
        assertTrue(batch.getQuantity() == 1000 - totalReductions,
            "Quantity mismatch after concurrent reductions");
    }

    @Test
    void shouldUpdateQuantitySafelyUnderConcurrentAdds() throws InterruptedException {
        StockBatch batch = new StockBatch(
            new BatchNumber("B-CONC-2"),
            new ProductId("P-CONC-2"),
            InventoryChannel.ONLINE,
            0,
            LocalDate.now().plusDays(15),
            LocalDate.now()
        );

        int threads = 8;
        int perThread = 75;
        int totalAdds = threads * perThread;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < perThread; i++) {
                        batch.addQuantity(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertTrue(finished, "Concurrent adds did not finish in time");
        assertTrue(batch.getQuantity() == totalAdds, "Quantity mismatch after concurrent adds");
    }
}
