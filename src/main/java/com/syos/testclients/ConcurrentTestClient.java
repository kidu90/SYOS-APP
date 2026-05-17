package com.syos.testclients;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ConcurrentTestClient {
    private static final String SERVER_BASE = "http://localhost:8080";
    private final Gson gson = new Gson();

    public void run(String clientName, int requests) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(8);

        CompletableFuture<?>[] futures = IntStream.range(0, requests).mapToObj(i -> CompletableFuture.runAsync(() -> {
            try {
                long start = System.currentTimeMillis();
                Map<String, Integer> cart = new HashMap<>();
                cart.put("P001", 1);

                Map<String, Object> payload = Map.of(
                    "cart", cart,
                    "saleType", "IN_STORE",
                    "customerName", clientName,
                    "customerAddress", "Test Address"
                );

                URL url = new URL(SERVER_BASE + "/checkout");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                String json = gson.toJson(payload);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes());
                }

                int code = conn.getResponseCode();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String body = br.lines().reduce("", (a, b) -> a + b + "\n");
                    String billNumber = extractBillNumber(body);
                    long duration = System.currentTimeMillis() - start;
                    System.out.printf("%s - request %d -> code=%d bill=%s time=%dms at %s\n", clientName, i, code, billNumber, duration, Instant.now().toString());
                }
            } catch (Exception e) {
                System.out.printf("%s - request %d -> FAILED: %s at %s\n", clientName, i, e.getMessage(), Instant.now().toString());
            }
        }, ex)).toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).get(60, TimeUnit.SECONDS);
        ex.shutdownNow();
    }

    private String extractBillNumber(String body) {
        try {
            Map<String, Object> map = gson.fromJson(body, new TypeToken<Map<String, Object>>(){}.getType());
            if (map.containsKey("billNumber")) {
                Object bn = map.get("billNumber");
                if (bn instanceof Map) {
                    Map<?,?> m = (Map<?,?>) bn;
                    Object v = m.get("value");
                    return v == null ? m.toString() : v.toString();
                } else {
                    return bn.toString();
                }
            }
        } catch (Exception ignored) {}
        return "<unknown>";
    }

    public static void main(String[] args) throws Exception {
        ConcurrentTestClient client = new ConcurrentTestClient();
        Thread t1 = new Thread(() -> {
            try { client.run("TestClient1", 10); } catch (Exception e) { e.printStackTrace(); }
        });
        Thread t2 = new Thread(() -> {
            try { client.run("TestClient2", 10); } catch (Exception e) { e.printStackTrace(); }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("All test requests complete.");
    }
}
