package com.syos.presentation.console;

import com.syos.application.factory.ProductFactory;
import com.syos.application.factory.StockBatchFactory;
import com.syos.application.report.DailySalesReport;
import com.syos.application.report.StockStatusReport;
import com.syos.application.strategy.*;
import com.syos.application.usecase.AddProductUseCase;
import com.syos.application.usecase.AddStockUseCase;
import com.syos.application.usecase.CheckoutCommand;
import com.syos.domain.entity.Bill;
import com.syos.domain.entity.Product;
import com.syos.domain.entity.StockBatch;
import com.syos.domain.repository.BillRepository;
import com.syos.domain.repository.ProductRepository;
import com.syos.domain.repository.StockRepository;
import com.syos.domain.valueobject.Money;
import com.syos.infrastructure.database.DatabaseManager;
import com.syos.infrastructure.persistence.SQLiteBillRepository;
import com.syos.infrastructure.persistence.SQLiteProductRepository;
import com.syos.infrastructure.persistence.SQLiteStockRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class SYOSApplication {
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final BillRepository billRepository;
    private final ProductFactory productFactory;
    private final StockBatchFactory stockBatchFactory;

    public SYOSApplication() {
        DatabaseManager dbManager = DatabaseManager.getInstance("syos.db");
        this.productRepository = new SQLiteProductRepository(dbManager);
        this.stockRepository = new SQLiteStockRepository(dbManager);
        this.billRepository = new SQLiteBillRepository(dbManager);
        this.productFactory = new ProductFactory();
        this.stockBatchFactory = new StockBatchFactory();
    }

    public static void main(String[] args) {
        SYOSApplication app = new SYOSApplication();
        app.run();
    }

    public void run() {
        printHeader();
        
        System.out.println("\n>>> INITIALIZING SYSTEM <<<\n");
        initializeSampleData();
        
        System.out.println("\n>>> SCENARIO 1: IN-STORE TRANSACTION <<<\n");
        performInStoreTransaction();
        
        System.out.println("\n>>> SCENARIO 2: ONLINE TRANSACTION <<<\n");
        performOnlineTransaction();
        
        System.out.println("\n>>> GENERATING REPORTS <<<\n");
        generateReports();
        
        printFooter();
    }

    private void printHeader() {
        System.out.println("=".repeat(80));
        System.out.println(" ".repeat(20) + "SYNEX OUTLET STORE (SYOS)");
        System.out.println(" ".repeat(15) + "Billing & Stock Management System");
        System.out.println("=".repeat(80));
    }

    private void printFooter() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" ".repeat(25) + "System Demonstration Complete");
        System.out.println("=".repeat(80));
    }

    private void initializeSampleData() {
        System.out.println("Adding products to catalog...");
        
        AddProductUseCase addProductUseCase = new AddProductUseCase(productRepository);
        
        Product rice = productFactory.createProduct("P001", "Basmati Rice", "Grains", 85.00, "kg");
        Product milk = productFactory.createProduct("P002", "Full Cream Milk", "Dairy", 65.00, "liter");
        Product bread = productFactory.createProduct("P003", "Whole Wheat Bread", "Bakery", 45.00, "loaf");
        Product oil = productFactory.createProduct("P004", "Sunflower Oil", "Oil", 180.00, "liter");
        Product sugar = productFactory.createProduct("P005", "White Sugar", "Grains", 55.00, "kg");
        
        addProductUseCase.execute(rice);
        addProductUseCase.execute(milk);
        addProductUseCase.execute(bread);
        addProductUseCase.execute(oil);
        addProductUseCase.execute(sugar);
        
        System.out.println("✓ Added 5 products to catalog");
        
        System.out.println("\nAdding stock batches...");
        
        AddStockUseCase addStockUseCase = new AddStockUseCase(stockRepository);
        
        StockBatch riceBatch1 = stockBatchFactory.createBatch("B001", "P001", 100, LocalDate.now().plusMonths(6));
        StockBatch riceBatch2 = stockBatchFactory.createBatch("B002", "P001", 50, LocalDate.now().plusMonths(8));
        StockBatch milkBatch = stockBatchFactory.createBatch("B003", "P002", 80, LocalDate.now().plusDays(5));
        StockBatch breadBatch = stockBatchFactory.createBatch("B004", "P003", 40, LocalDate.now().plusDays(2));
        StockBatch oilBatch = stockBatchFactory.createBatch("B005", "P004", 60, LocalDate.now().plusMonths(12));
        StockBatch sugarBatch = stockBatchFactory.createBatch("B006", "P005", 120, LocalDate.now().plusMonths(10));
        
        addStockUseCase.execute(riceBatch1);
        addStockUseCase.execute(riceBatch2);
        addStockUseCase.execute(milkBatch);
        addStockUseCase.execute(breadBatch);
        addStockUseCase.execute(oilBatch);
        addStockUseCase.execute(sugarBatch);
        
        System.out.println("✓ Added 6 stock batches");
        System.out.println("✓ System initialized successfully");
    }

    private void performInStoreTransaction() {
        System.out.println("Processing in-store sale at POS terminal...");
        System.out.println("\nCustomer Cart:");
        System.out.println("  - Basmati Rice (P001): 5 kg");
        System.out.println("  - Full Cream Milk (P002): 3 liters");
        System.out.println("  - Whole Wheat Bread (P003): 2 loaves");
        
        Map<String, Integer> cart = new HashMap<>();
        cart.put("P001", 5);
        cart.put("P002", 3);
        cart.put("P003", 2);
        
        StockSelectionStrategy stockStrategy = new FEFOStockSelectionStrategy();
        DiscountStrategy discountStrategy = new NoDiscountStrategy();
        
        CheckoutCommand checkout = new CheckoutCommand(
            productRepository,
            stockRepository,
            billRepository,
            stockStrategy,
            discountStrategy,
            cart,
            Bill.SaleType.IN_STORE,
            null,
            null
        );
        
        checkout.execute();
        Bill bill = checkout.getGeneratedBill();
        
        printBill(bill);
    }

    private void performOnlineTransaction() {
        System.out.println("Processing online order...");
        System.out.println("\nCustomer: Rajesh Kumar");
        System.out.println("Delivery Address: 123 MG Road, Bangalore");
        System.out.println("\nOrder Items:");
        System.out.println("  - Sunflower Oil (P004): 2 liters");
        System.out.println("  - White Sugar (P005): 10 kg");
        System.out.println("  - Basmati Rice (P001): 15 kg");
        
        Map<String, Integer> cart = new HashMap<>();
        cart.put("P004", 2);
        cart.put("P005", 10);
        cart.put("P001", 15);
        
        StockSelectionStrategy stockStrategy = new FEFOStockSelectionStrategy();
        DiscountStrategy discountStrategy = new ThresholdDiscountStrategy(
            new Money(1000),
            new Money(50)
        );
        
        CheckoutCommand checkout = new CheckoutCommand(
            productRepository,
            stockRepository,
            billRepository,
            stockStrategy,
            discountStrategy,
            cart,
            Bill.SaleType.ONLINE,
            "Rajesh Kumar",
            "123 MG Road, Bangalore"
        );
        
        checkout.execute();
        Bill bill = checkout.getGeneratedBill();
        
        printBill(bill);
    }

    private void printBill(Bill bill) {
        System.out.println("\n" + "-".repeat(80));
        System.out.println(" ".repeat(25) + "SYNEX OUTLET STORE");
        System.out.println(" ".repeat(20) + "GST No: 29XXXXX1234X1Z5");
        System.out.println("-".repeat(80));
        System.out.println("Bill No: " + bill.getBillNumber());
        System.out.println("Date/Time: " + bill.getTimestamp());
        System.out.println("Type: " + bill.getSaleType());
        
        if (bill.getSaleType() == Bill.SaleType.ONLINE) {
            System.out.println("Customer: " + bill.getCustomerName());
            System.out.println("Address: " + bill.getCustomerAddress());
        }
        
        System.out.println("-".repeat(80));
        System.out.printf("%-30s %-10s %-12s %-15s%n", "Item", "Qty", "Rate", "Amount");
        System.out.println("-".repeat(80));
        
        bill.getItems().forEach(item -> {
            System.out.printf("%-30s %-10d %-12s %-15s%n",
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
            System.out.printf("  Batch: %s%n", item.getBatchNumber());
        });
        
        System.out.println("-".repeat(80));
        System.out.printf("%-52s %s%n", "Subtotal:", bill.getSubtotal());
        System.out.printf("%-52s %s%n", "Discount:", bill.getDiscount());
        System.out.println("-".repeat(80));
        System.out.printf("%-52s %s%n", "TOTAL:", bill.getTotal());
        System.out.println("-".repeat(80));
        System.out.println(" ".repeat(25) + "Thank you for shopping!");
        System.out.println("-".repeat(80));
    }

    private void generateReports() {
        System.out.println("Generating daily sales report...\n");
        
        DailySalesReport salesReport = new DailySalesReport(billRepository, LocalDate.now());
        System.out.println(salesReport.generateReport());
        
        System.out.println("\n\nGenerating stock status report...\n");
        
        StockStatusReport stockReport = new StockStatusReport(stockRepository, productRepository);
        System.out.println(stockReport.generateReport());
    }
}
