package com.syos.application.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProductLockManager {
    private static final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();

    public static Object getLock(String productId) {
        return locks.computeIfAbsent(productId, k -> new Object());
    }
}
