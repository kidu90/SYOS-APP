package com.syos.presentation.swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class SYOSClientApplication {
    private static final String SERVER_BASE = "http://localhost:8080";
    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile String lastProductsSnapshot = "";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SYOSClientApplication().createAndShowGui());
    }

    private void createAndShowGui() {
        JFrame frame = new JFrame("SYOS - Client (Swing)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JPanel panel = new JPanel(new BorderLayout());

        JTextArea output = new JTextArea();
        output.setEditable(false);
        panel.add(new JScrollPane(output), BorderLayout.CENTER);

        JPanel controls = new JPanel();
        JButton btnRefresh = new JButton(new AbstractAction("Refresh Products") {
            @Override
            public void actionPerformed(ActionEvent e) {
                executor.submit(() -> {
                    List<Map<String, Object>> products = fetchProducts();
                    if (products != null) {
                        String text = products.stream().map(p -> p.get("id") + " - " + p.get("name") + " (" + p.get("category") + ") ₹" + p.get("unitPrice") + "/" + p.get("unit")).collect(Collectors.joining("\n"));
                        SwingUtilities.invokeLater(() -> output.setText(text));
                        lastProductsSnapshot = text;
                    } else {
                        SwingUtilities.invokeLater(() -> output.setText("Failed to fetch products"));
                    }
                });
            }
        });

        JButton btnSimpleCheckout = new JButton(new AbstractAction("Simple In-Store Checkout (P001 x1)") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Map<String, Integer> cart = new HashMap<>();
                cart.put("P001", 1);
                executor.submit(() -> {
                    String resp = performCheckout(cart, "IN_STORE", null, null);
                    String out = resp == null ? "Checkout failed" : resp;
                    SwingUtilities.invokeLater(() -> output.setText(out));
                    // trigger immediate refresh after checkout
                    executor.submit(() -> {
                        List<Map<String, Object>> products = fetchProducts();
                        if (products != null) {
                            String text = products.stream().map(p -> p.get("id") + " - " + p.get("name") + " (" + p.get("category") + ") ₹" + p.get("unitPrice") + "/" + p.get("unit")).collect(Collectors.joining("\n"));
                            lastProductsSnapshot = text;
                            SwingUtilities.invokeLater(() -> output.setText(text + "\n\n" + out));
                        }
                    });
                });
            }
        });

        JButton btnDailyReport = new JButton(new AbstractAction("Daily Sales Report") {
            @Override
            public void actionPerformed(ActionEvent e) {
                executor.submit(() -> {
                    String report = fetchReport("/reports/dailySales");
                    SwingUtilities.invokeLater(() -> output.setText(report == null ? "Failed to fetch report" : report));
                });
            }
        });

        controls.add(btnRefresh);
        controls.add(btnSimpleCheckout);
        controls.add(btnDailyReport);

        panel.add(controls, BorderLayout.NORTH);

        frame.setContentPane(panel);
        frame.setVisible(true);

        // Start polling to auto-refresh products when changes occur
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Map<String, Object>> products = fetchProducts();
                if (products != null) {
                    String text = products.stream().map(p -> p.get("id") + " - " + p.get("name") + " (" + p.get("category") + ") ₹" + p.get("unitPrice") + "/" + p.get("unit")).collect(Collectors.joining("\n"));
                    if (!Objects.equals(text, lastProductsSnapshot)) {
                        lastProductsSnapshot = text;
                        SwingUtilities.invokeLater(() -> output.setText(text));
                    }
                }
            } catch (Exception ignored) {
            }
        }, 0, 2, TimeUnit.SECONDS);

        // Shutdown executors on window close
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdownNow();
            executor.shutdownNow();
        }));
    }

    private List<Map<String, Object>> fetchProducts() {
        try {
            URL url = new URL(SERVER_BASE + "/products");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);

            int code = conn.getResponseCode();
            if (code != 200) return null;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                List<Map<String, Object>> products = gson.fromJson(br, new TypeToken<List<Map<String, Object>>>(){}.getType());
                return products;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String fetchReport(String path) {
        try {
            URL url = new URL(SERVER_BASE + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);

            int code = conn.getResponseCode();
            if (code != 200) return null;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                Map<String, Object> resp = gson.fromJson(br, new TypeToken<Map<String, Object>>(){}.getType());
                return (String) resp.get("report");
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String performCheckout(Map<String, Integer> cart, String saleType, String name, String address) {
        try {
            URL url = new URL(SERVER_BASE + "/checkout");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            Map<String, Object> payload = new HashMap<>();
            payload.put("cart", cart);
            payload.put("saleType", saleType);
            if (name != null) {
                payload.put("customerName", name);
            }
            if (address != null) {
                payload.put("customerAddress", address);
            }
            String json = gson.toJson(payload);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
            }

            int code = conn.getResponseCode();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String body = br.lines().collect(Collectors.joining("\n"));
                return body;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
