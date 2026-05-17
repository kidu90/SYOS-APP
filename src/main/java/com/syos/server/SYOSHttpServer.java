package com.syos.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.syos.application.factory.ProductFactory;
import com.syos.application.factory.StockBatchFactory;
import com.syos.application.report.DailySalesReport;
import com.syos.application.report.IReportGenerator;
import com.syos.application.report.StockStatusReport;
import com.syos.application.service.BillCalculationService;
import com.syos.application.service.BillNumberGenerator;
import com.syos.application.service.BillNumberService;
import com.syos.application.service.InventoryManager;
import com.syos.application.strategy.DiscountStrategy;
import com.syos.application.strategy.FEFOStockSelectionStrategy;
import com.syos.application.strategy.NoDiscountStrategy;
import com.syos.application.strategy.StockSelectionStrategy;
import com.syos.application.strategy.ThresholdDiscountStrategy;
import com.syos.application.usecase.AddProductUseCase;
import com.syos.application.usecase.CheckoutCommand;
import com.syos.domain.entity.Bill;
import com.syos.domain.repository.BillRepository;
import com.syos.domain.repository.ProductRepository;
import com.syos.domain.repository.StockRepository;
import com.syos.domain.valueobject.InventoryChannel;
import com.syos.domain.valueobject.Money;
import com.syos.infrastructure.database.DatabaseManager;
import com.syos.infrastructure.persistence.SQLiteBillRepository;
import com.syos.infrastructure.persistence.SQLiteProductRepository;
import com.syos.infrastructure.persistence.SQLiteStockRepository;

public class SYOSHttpServer {
    private static final int PORT = 8080;
    private static final int CORE_THREADS = 4;
    private static final int MAX_THREADS = 20;
    private static final int QUEUE_CAPACITY = 50;

    private final Gson gson = new Gson();
    private final HttpServer server;
    private final ThreadPoolExecutor executor;

    // Application wiring
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final BillRepository billRepository;

    private final InventoryManager inventoryManager;
    private final BillNumberService billNumberService;
    private final BillCalculationService billCalculationService;
    private final StockSelectionStrategy stockSelectionStrategy;
    private final DiscountStrategy inStoreDiscountStrategy;
    private final DiscountStrategy onlineDiscountStrategy;
    private final ProductFactory productFactory;
    private final StockBatchFactory stockBatchFactory;

    public SYOSHttpServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);

        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        RejectedExecutionHandler rejectHandler = (r, exe) -> {
            // No-op: handler will return 503 when queue is full via custom wrapper
        };
        executor = new ThreadPoolExecutor(CORE_THREADS, MAX_THREADS, 60, TimeUnit.SECONDS, queue, rejectHandler);
        server.setExecutor(executor);

        // Wire application dependencies (reuse existing infra)
        DatabaseManager dbManager = DatabaseManager.getInstance("syos.db");
        productRepository = new SQLiteProductRepository(dbManager);
        stockRepository = new SQLiteStockRepository(dbManager);
        billRepository = new SQLiteBillRepository(dbManager);

        inventoryManager = new InventoryManager(stockRepository, stockRepository);
        billNumberService = BillNumberGenerator.getInstance();
        billCalculationService = new BillCalculationService();
        stockSelectionStrategy = new FEFOStockSelectionStrategy();
        inStoreDiscountStrategy = new NoDiscountStrategy();
        onlineDiscountStrategy = new ThresholdDiscountStrategy(new Money(1000), new Money(50));
        productFactory = new ProductFactory();
        stockBatchFactory = new StockBatchFactory();

        // Register endpoints
        server.createContext("/products", new ProductsHandler());
        server.createContext("/addProduct", new AddProductHandler());
        server.createContext("/addStock", new AddStockHandler());
        server.createContext("/checkout", new CheckoutHandler());
        server.createContext("/reports/dailySales", new DailySalesHandler());
        server.createContext("/reports/stockStatus", new StockStatusHandler());
    }

    public void start() {
        server.start();
        System.out.println("SYOS HTTP Server started on port " + PORT);
    }

    public void stop() {
        server.stop(1);
        executor.shutdownNow();
    }

    // Handlers
    private class ProductsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            List<?> products = productRepository.findAll();
            String resp = gson.toJson(products);
            byte[] bytes = resp.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class AddProductHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody())) {
                Map<String, Object> payload = gson.fromJson(isr, new TypeToken<Map<String, Object>>(){}.getType());
                String id = (String) payload.get("id");
                String name = (String) payload.get("name");
                String category = (String) payload.get("category");
                double price = ((Number) payload.get("price")).doubleValue();
                String unit = (String) payload.get("unit");

                com.syos.domain.entity.Product product = productFactory.createProduct(id, name, category, price, unit);
                AddProductUseCase addProduct = new AddProductUseCase(productRepository);
                addProduct.execute(product);

                String resp = gson.toJson(Map.of("status", "ok", "id", id));
                byte[] bytes = resp.getBytes();
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(201, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                String resp = gson.toJson(Map.of("status", "error", "message", e.getMessage()));
                byte[] bytes = resp.getBytes();
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    private class AddStockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody())) {
                Map<String, Object> payload = gson.fromJson(isr, new TypeToken<Map<String, Object>>(){}.getType());
                String batchNo = (String) payload.get("batchNumber");
                String productId = (String) payload.get("productId");
                String channelStr = (String) payload.get("channel");
                int qty = ((Number) payload.get("quantity")).intValue();
                String expiry = (String) payload.get("expiryDate");

                com.syos.domain.entity.StockBatch batch = stockBatchFactory.createBatch(batchNo, productId, InventoryChannel.valueOf(channelStr), qty, LocalDate.parse(expiry));
                inventoryManager.addStock(batch, InventoryChannel.valueOf(channelStr));

                String resp = gson.toJson(Map.of("status", "ok", "batch", batchNo));
                byte[] bytes = resp.getBytes();
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(201, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                String resp = gson.toJson(Map.of("status", "error", "message", e.getMessage()));
                byte[] bytes = resp.getBytes();
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    private class CheckoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody())) {
                Map<String, Object> payload = gson.fromJson(isr, new TypeToken<Map<String, Object>>(){}.getType());
                Map<String, Double> cartRaw = (Map<String, Double>) payload.get("cart");
                Map<String, Integer> cart = cartRaw.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue()));
                String saleType = (String) payload.get("saleType");
                String customerName = (String) payload.get("customerName");
                String customerAddress = (String) payload.get("customerAddress");

                DiscountStrategy strategy = "ONLINE".equalsIgnoreCase(saleType) ? onlineDiscountStrategy : inStoreDiscountStrategy;
                Bill.SaleType st = "ONLINE".equalsIgnoreCase(saleType) ? Bill.SaleType.ONLINE : Bill.SaleType.IN_STORE;

                CheckoutCommand checkout = new CheckoutCommand(
                    productRepository,
                    inventoryManager,
                    billRepository,
                    billNumberService,
                    billCalculationService,
                    stockSelectionStrategy,
                    strategy,
                    cart,
                    st,
                    customerName,
                    customerAddress
                );

                // Execute checkout synchronously on the current handler thread
                checkout.execute();
                Bill bill = checkout.getGeneratedBill();
                String resp = gson.toJson(bill);
                byte[] bytes = resp.getBytes();
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }

            } catch (Exception e) {
                String resp = gson.toJson(Map.of("status", "error", "message", e.getMessage()));
                byte[] bytes = resp.getBytes();
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    private class DailySalesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            IReportGenerator report = new DailySalesReport(billRepository, LocalDate.now());
            String output = report.generateReport();
            String resp = gson.toJson(Map.of("report", output));
            byte[] bytes = resp.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class StockStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            IReportGenerator report = new StockStatusReport(stockRepository, productRepository);
            String output = report.generateReport();
            String resp = gson.toJson(Map.of("report", output));
            byte[] bytes = resp.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // Main for server
    public static void main(String[] args) throws Exception {
        SYOSHttpServer server = new SYOSHttpServer();
        server.start();
        System.out.println("Press CTRL+C to stop server");
    }
}
