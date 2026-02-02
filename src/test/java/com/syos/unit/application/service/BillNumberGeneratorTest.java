package com.syos.unit.application.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.syos.application.service.BillNumberGenerator;

class BillNumberGeneratorTest {

    @BeforeEach
    void setUp() {
        BillNumberGenerator.getInstance().reset();
    }

    @Test
    void shouldGenerateUniqueBillNumbers() {
        BillNumberGenerator generator = BillNumberGenerator.getInstance();

        String billNumber1 = generator.generateBillNumber("POS");
        String billNumber2 = generator.generateBillNumber("POS");

        assertNotEquals(billNumber1, billNumber2);
    }

    @Test
    void shouldIncludePrefix() {
        BillNumberGenerator generator = BillNumberGenerator.getInstance();

        String billNumber = generator.generateBillNumber("POS");

        assertTrue(billNumber.startsWith("POS-"));
    }

    @Test
    void shouldIncludeDate() {
        BillNumberGenerator generator = BillNumberGenerator.getInstance();

        String billNumber = generator.generateBillNumber("POS");

        String expectedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertTrue(billNumber.contains(expectedDate));
    }

    @Test
    void shouldGenerateSequentialNumbers() {
        BillNumberGenerator generator = BillNumberGenerator.getInstance();

        String billNumber1 = generator.generateBillNumber("POS");
        String billNumber2 = generator.generateBillNumber("POS");

        assertTrue(billNumber1.endsWith("00001"));
        assertTrue(billNumber2.endsWith("00002"));
    }

    @Test
    void shouldReturnSameInstance() {
        BillNumberGenerator instance1 = BillNumberGenerator.getInstance();
        BillNumberGenerator instance2 = BillNumberGenerator.getInstance();

        assertSame(instance1, instance2);
    }

    @Test
    void shouldGenerateUniqueBillNumbersConcurrently() throws InterruptedException {
        BillNumberGenerator generator = BillNumberGenerator.getInstance();

        int threads = 10;
        int perThread = 200;
        int total = threads * perThread;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        Set<String> results = ConcurrentHashMap.newKeySet();

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < perThread; i++) {
                        results.add(generator.generateBillNumber("POS"));
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

        assertTrue(finished, "Concurrent generation did not finish in time");
        assertTrue(results.size() == total, "Duplicate bill numbers detected under concurrency");
    }
}
