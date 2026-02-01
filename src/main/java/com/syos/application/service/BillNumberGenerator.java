package com.syos.application.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class BillNumberGenerator {
    private static BillNumberGenerator instance;
    private final AtomicLong counter;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private BillNumberGenerator() {
        this.counter = new AtomicLong(1);
    }

    public static synchronized BillNumberGenerator getInstance() {
        if (instance == null) {
            instance = new BillNumberGenerator();
        }
        return instance;
    }

    public String generateBillNumber(String prefix) {
        String date = LocalDateTime.now().format(DATE_FORMAT);
        long sequence = counter.getAndIncrement();
        return String.format("%s-%s-%05d", prefix, date, sequence);
    }

    public void reset() {
        counter.set(1);
    }
}
