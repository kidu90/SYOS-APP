package com.syos.presentation.console;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.syos.application.factory.ProductFactory;
import com.syos.application.factory.StockBatchFactory;
import com.syos.application.report.IReportGenerator;
import com.syos.application.service.BillCalculationService;
import com.syos.application.service.BillNumberService;
import com.syos.application.service.InventoryManager;
import com.syos.application.strategy.DiscountStrategy;
import com.syos.application.strategy.StockSelectionStrategy;
import com.syos.application.usecase.AddProductUseCase;
import com.syos.application.usecase.CheckoutCommand;
import com.syos.domain.entity.Bill;
import com.syos.domain.entity.Product;
import com.syos.domain.entity.StockBatch;
import com.syos.domain.repository.BillWriteRepository;
import com.syos.domain.repository.ProductReadRepository;
import com.syos.domain.valueobject.InventoryChannel;
import com.syos.domain.valueobject.ProductId;

public class SYOSConsoleUI {
    private final ProductReadRepository productReadRepository;
    private final BillWriteRepository billWriteRepository;
    private final InventoryManager inventoryManager;
    private final AddProductUseCase addProductUseCase;
    private final ProductFactory productFactory;
    private final StockBatchFactory stockBatchFactory;
    private final BillNumberService billNumberService;
    private final BillCalculationService billCalculationService;
    private final StockSelectionStrategy stockSelectionStrategy;
    private final DiscountStrategy inStoreDiscountStrategy;
    private final DiscountStrategy onlineDiscountStrategy;
    private final BillPrinter billPrinter;
    private final IReportGenerator dailySalesReport;
    private final IReportGenerator stockStatusReport;
    
    // User management
    private final Map<String, User> registeredUsers;
    private User currentUser;
    private final Scanner scanner;
    
    // Simple User class for authentication
    private static class User {
        String username;
        String password;
        String fullName;
        String address;
        
        User(String username, String password, String fullName, String address) {
            this.username = username;
            this.password = password;
            this.fullName = fullName;
            this.address = address;
        }
    }

    public SYOSConsoleUI(ProductReadRepository productReadRepository,
                         BillWriteRepository billWriteRepository,
                         InventoryManager inventoryManager,
                         AddProductUseCase addProductUseCase,
                         ProductFactory productFactory,
                         StockBatchFactory stockBatchFactory,
                         BillNumberService billNumberService,
                         BillCalculationService billCalculationService,
                         StockSelectionStrategy stockSelectionStrategy,
                         DiscountStrategy inStoreDiscountStrategy,
                         DiscountStrategy onlineDiscountStrategy,
                         BillPrinter billPrinter,
                         IReportGenerator dailySalesReport,
                         IReportGenerator stockStatusReport) {
        this.productReadRepository = productReadRepository;
        this.billWriteRepository = billWriteRepository;
        this.inventoryManager = inventoryManager;
        this.addProductUseCase = addProductUseCase;
        this.productFactory = productFactory;
        this.stockBatchFactory = stockBatchFactory;
        this.billNumberService = billNumberService;
        this.billCalculationService = billCalculationService;
        this.stockSelectionStrategy = stockSelectionStrategy;
        this.inStoreDiscountStrategy = inStoreDiscountStrategy;
        this.onlineDiscountStrategy = onlineDiscountStrategy;
        this.billPrinter = billPrinter;
        this.dailySalesReport = dailySalesReport;
        this.stockStatusReport = stockStatusReport;
        this.registeredUsers = new HashMap<>();
        this.scanner = new Scanner(System.in);
        this.currentUser = null;
        
    }

    public void run() {
        printHeader();
        initializeSampleData();
        
        boolean running = true;
        while (running) {
            showMainMenu();
        }
    }
    
    private void showAuthenticationMenu() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" ".repeat(25) + "AUTHENTICATION MENU");
        System.out.println(" ".repeat(20) + "(Required for Online Transactions)");
        System.out.println("=".repeat(80));
        System.out.println("1. Login");
        System.out.println("2. Sign Up");
        System.out.println("3. Back to Main Menu");
        System.out.print("\nEnter your choice: ");
        
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1":
                login();
                break;
            case "2":
                signUp();
                break;
            case "3":
                // Return to main menu
                return;
            default:
                System.out.println("\n Invalid choice. Please try again.");
                waitForEnter();
                showAuthenticationMenu();
        }
    }
    
    private void login() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println(" ".repeat(30) + "USER LOGIN");
        System.out.println("-".repeat(80));
        
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        
        User user = registeredUsers.get(username);
        
        if (user != null && user.password.equals(password)) {
            currentUser = user;
            System.out.println("\n✓ Login successful! Welcome, " + user.fullName + "!");
            waitForEnter();
        } else {
            System.out.println("\n Invalid username or password. Please try again.");
            waitForEnter();
        }
    }
    
    private void signUp() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println(" ".repeat(30) + "USER SIGN UP");
        System.out.println("-".repeat(80));
        
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        
        if (registeredUsers.containsKey(username)) {
            System.out.println("\n Username already exists. Please choose a different username.");
            waitForEnter();
            return;
        }
        
        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();
        
        System.out.print("Enter full name: ");
        String fullName = scanner.nextLine().trim();
        
        System.out.print("Enter address: ");
        String address = scanner.nextLine().trim();
        
        User newUser = new User(username, password, fullName, address);
        registeredUsers.put(username, newUser);
        
        System.out.println("\n✓ Sign up successful! You can now login with your credentials.");
        waitForEnter();
    }
    
    private void showMainMenu() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" ".repeat(25) + "MAIN MENU");
        if (currentUser != null) {
            System.out.println(" ".repeat(20) + "Logged in as: " + currentUser.fullName);
        }
        System.out.println("=".repeat(80));
        System.out.println("1. In-Store Transaction");
        System.out.println("2. Online Transaction");
        System.out.println("3. View Products");
        System.out.println("4. View Reports");
        System.out.println("5. Exit");
        System.out.print("\nEnter your choice: ");
        
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1":
                performInStoreTransactionInteractive();
                break;
            case "2":
                performOnlineTransactionInteractive();
                break;
            case "3":
                viewProducts();
                break;
            case "4":
                generateReportsInteractive();
                break;
            case "5":
                System.out.println("\nThank you for using SYOS! Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("\n Invalid choice. Please try again.");
                waitForEnter();
        }
    }
    
    private void logout() {
        System.out.println("\n✓ Logged out successfully. Goodbye, " + currentUser.fullName + "!");
        currentUser = null;
        waitForEnter();
    }
    
    private void waitForEnter() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
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
        System.out.println("\n>>> INITIALIZING SYSTEM <<<\n");
        System.out.println("Adding products to catalog...");

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

        StockBatch riceBatch1 = stockBatchFactory.createBatch("B001", "P001", InventoryChannel.STORE, 100, LocalDate.now().plusMonths(6));
        StockBatch riceBatch2 = stockBatchFactory.createBatch("B002", "P001", InventoryChannel.ONLINE, 50, LocalDate.now().plusMonths(8));
        StockBatch milkBatch = stockBatchFactory.createBatch("B003", "P002", InventoryChannel.STORE, 80, LocalDate.now().plusDays(5));
        StockBatch breadBatch = stockBatchFactory.createBatch("B004", "P003", InventoryChannel.STORE, 40, LocalDate.now().plusDays(2));
        StockBatch oilBatch = stockBatchFactory.createBatch("B005", "P004", InventoryChannel.ONLINE, 60, LocalDate.now().plusMonths(12));
        StockBatch sugarBatch = stockBatchFactory.createBatch("B006", "P005", InventoryChannel.ONLINE, 120, LocalDate.now().plusMonths(10));

        inventoryManager.addStock(riceBatch1, InventoryChannel.STORE);
        inventoryManager.addStock(riceBatch2, InventoryChannel.ONLINE);
        inventoryManager.addStock(milkBatch, InventoryChannel.STORE);
        inventoryManager.addStock(breadBatch, InventoryChannel.STORE);
        inventoryManager.addStock(oilBatch, InventoryChannel.ONLINE);
        inventoryManager.addStock(sugarBatch, InventoryChannel.ONLINE);

        System.out.println("✓ Added 6 stock batches");
        System.out.println("✓ System initialized successfully");
    }
    
    private void viewProducts() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println(" ".repeat(30) + "PRODUCT CATALOG");
        System.out.println("-".repeat(80));
        
        String[] productIds = {"P001", "P002", "P003", "P004", "P005"};
        
        System.out.printf("%-10s %-25s %-15s %-15s %-10s%n", 
                         "ID", "Name", "Category", "Price", "Unit");
        System.out.println("-".repeat(80));
        
        for (String id : productIds) {
            Product product = productReadRepository.findById(new ProductId(id)).orElse(null);
            if (product != null) {
                System.out.printf("%-10s %-25s %-15s ₹%-14.2f %-10s%n",
                                 product.getId().getValue(),
                                 product.getName(),
                                 product.getCategory(),
                                 product.getUnitPrice().getAmount(),
                                 product.getUnit());
            }
        }
        
        waitForEnter();
    }

    private void performInStoreTransactionInteractive() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println(" ".repeat(25) + "IN-STORE TRANSACTION");
        System.out.println("-".repeat(80));
        System.out.println("Processing in-store sale at POS terminal...");
        
        Map<String, Integer> cart = new HashMap<>();
        boolean addingItems = true;
        
        while (addingItems) {
            System.out.println("\n--- Available Products ---");
            System.out.println("P001 - Basmati Rice (₹85.00/kg)");
            System.out.println("P002 - Full Cream Milk (₹65.00/liter)");
            System.out.println("P003 - Whole Wheat Bread (₹45.00/loaf)");
            System.out.println("P004 - Sunflower Oil (₹180.00/liter) [Online Only]");
            System.out.println("P005 - White Sugar (₹55.00/kg) [Online Only]");
            
            System.out.print("\nEnter Product ID (or 'done' to checkout): ");
            String productId = scanner.nextLine().trim().toUpperCase();
            
            if (productId.equals("DONE")) {
                if (cart.isEmpty()) {
                    System.out.println("\n Cart is empty! Please add at least one item.");
                    continue;
                }
                addingItems = false;
            } else if (productId.equals("P004") || productId.equals("P005")) {
                System.out.println("\n This product is only available for online orders!");
            } else if (productId.equals("P001") || productId.equals("P002") || productId.equals("P003")) {
                System.out.print("Enter quantity: ");
                try {
                    int quantity = Integer.parseInt(scanner.nextLine().trim());
                    if (quantity > 0) {
                        cart.put(productId, cart.getOrDefault(productId, 0) + quantity);
                        System.out.println("✓ Added to cart!");
                    } else {
                        System.out.println(" Quantity must be positive!");
                    }
                } catch (NumberFormatException e) {
                    System.out.println(" Invalid quantity!");
                }
            } else {
                System.out.println(" Invalid Product ID!");
            }
        }
        
        System.out.println("\n--- Cart Summary ---");
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Product product = productReadRepository.findById(new ProductId(entry.getKey())).orElse(null);
            if (product != null) {
                System.out.println("  - " + product.getName() + ": " + entry.getValue() + " " + product.getUnit());
            }
        }

        CheckoutCommand checkout = new CheckoutCommand(
            productReadRepository,
            inventoryManager,
            billWriteRepository,
            billNumberService,
            billCalculationService,
            stockSelectionStrategy,
            inStoreDiscountStrategy,
            cart,
            Bill.SaleType.IN_STORE,
            null,
            null
        );

        checkout.execute();
        Bill bill = checkout.getGeneratedBill();

        System.out.println("\n");
        billPrinter.print(bill);
        waitForEnter();
    }

    private void performOnlineTransactionInteractive() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println(" ".repeat(25) + "ONLINE TRANSACTION");
        System.out.println("-".repeat(80));
        
        // Check if user is logged in, if not show authentication menu
        if (currentUser == null) {
            showAuthenticationMenu();
            if (currentUser == null) {
                // User chose to exit from authentication menu
                return;
            }
        }
        
        System.out.println("Processing online order...");
        
        System.out.println("\nCustomer Name: " + currentUser.fullName);
        System.out.println("Delivery Address: " + currentUser.address);
        
        System.out.print("\nWould you like to update delivery address? (y/n): ");
        String updateAddress = scanner.nextLine().trim().toLowerCase();
        
        String deliveryAddress = currentUser.address;
        if (updateAddress.equals("y") || updateAddress.equals("yes")) {
            System.out.print("Enter new delivery address: ");
            deliveryAddress = scanner.nextLine().trim();
        }
        
        Map<String, Integer> cart = new HashMap<>();
        boolean addingItems = true;
        
        while (addingItems) {
            System.out.println("\n--- Available Products for Online Orders ---");
            System.out.println("P001 - Basmati Rice (₹85.00/kg)");
            System.out.println("P004 - Sunflower Oil (₹180.00/liter)");
            System.out.println("P005 - White Sugar (₹55.00/kg)");
            
            System.out.print("\nEnter Product ID (or 'done' to checkout): ");
            String productId = scanner.nextLine().trim().toUpperCase();
            
            if (productId.equals("DONE")) {
                if (cart.isEmpty()) {
                    System.out.println("\n Cart is empty! Please add at least one item.");
                    continue;
                }
                addingItems = false;
            } else if (productId.equals("P001") || productId.equals("P004") || productId.equals("P005")) {
                System.out.print("Enter quantity: ");
                try {
                    int quantity = Integer.parseInt(scanner.nextLine().trim());
                    if (quantity > 0) {
                        cart.put(productId, cart.getOrDefault(productId, 0) + quantity);
                        System.out.println("✓ Added to cart!");
                    } else {
                        System.out.println(" Quantity must be positive!");
                    }
                } catch (NumberFormatException e) {
                    System.out.println(" Invalid quantity!");
                }
            } else {
                System.out.println(" Invalid Product ID or not available for online orders!");
            }
        }
        
        System.out.println("\n--- Order Summary ---");
        System.out.println("Customer: " + currentUser.fullName);
        System.out.println("Delivery Address: " + deliveryAddress);
        System.out.println("\nOrder Items:");
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Product product = productReadRepository.findById(new ProductId(entry.getKey())).orElse(null);
            if (product != null) {
                System.out.println("  - " + product.getName() + ": " + entry.getValue() + " " + product.getUnit());
            }
        }

        CheckoutCommand checkout = new CheckoutCommand(
            productReadRepository,
            inventoryManager,
            billWriteRepository,
            billNumberService,
            billCalculationService,
            stockSelectionStrategy,
            onlineDiscountStrategy,
            cart,
            Bill.SaleType.ONLINE,
            currentUser.fullName,
            deliveryAddress
        );

        checkout.execute();
        Bill bill = checkout.getGeneratedBill();

        System.out.println("\n");
        billPrinter.print(bill);
        waitForEnter();
    }

    private void generateReportsInteractive() {
        System.out.println("\n" + "-".repeat(80));
        System.out.println(" ".repeat(30) + "REPORTS");
        System.out.println("-".repeat(80));
        System.out.println("1. Daily Sales Report");
        System.out.println("2. Stock Status Report");
        System.out.println("3. Back to Main Menu");
        System.out.print("\nEnter your choice: ");
        
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1":
                System.out.println("\n--- DAILY SALES REPORT ---\n");
                System.out.println(dailySalesReport.generateReport());
                waitForEnter();
                break;
            case "2":
                System.out.println("\n--- STOCK STATUS REPORT ---\n");
                System.out.println(stockStatusReport.generateReport());
                waitForEnter();
                break;
            case "3":
                return;
            default:
                System.out.println("\n Invalid choice!");
                waitForEnter();
        }
    }
}
