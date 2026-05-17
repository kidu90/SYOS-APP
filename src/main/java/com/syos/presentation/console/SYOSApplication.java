package com.syos.presentation.console;

import java.time.LocalDate;

import com.syos.application.factory.ProductFactory;
import com.syos.application.factory.StockBatchFactory;
import com.syos.application.report.DailySalesReport;
import com.syos.application.report.IReportGenerator;
import com.syos.application.report.StockStatusReport;
import com.syos.application.service.BillCalculationService;
import com.syos.application.service.BillNumberGenerator;
import com.syos.application.service.BillNumberService;
import com.syos.application.service.InventoryManager;
import com.syos.application.service.UserService;
import com.syos.application.strategy.DiscountStrategy;
import com.syos.application.strategy.FEFOStockSelectionStrategy;
import com.syos.application.strategy.NoDiscountStrategy;
import com.syos.application.strategy.StockSelectionStrategy;
import com.syos.application.strategy.ThresholdDiscountStrategy;
import com.syos.application.usecase.AddProductUseCase;
import com.syos.domain.entity.Product;
import com.syos.domain.entity.StockBatch;
import com.syos.domain.repository.BillRepository;
import com.syos.domain.repository.ProductRepository;
import com.syos.domain.repository.StockRepository;
import com.syos.domain.repository.UserRepository;
import com.syos.domain.valueobject.Money;
import com.syos.infrastructure.database.DatabaseManager;
import com.syos.infrastructure.persistence.SQLiteBillRepository;
import com.syos.infrastructure.persistence.SQLiteProductRepository;
import com.syos.infrastructure.persistence.SQLiteStockRepository;
import com.syos.infrastructure.persistence.SQLiteUserRepository;
import com.syos.presentation.gui.SYOSGuiApplication;

public class SYOSApplication {
    public static void main(String[] args) {
        SYOSApplication app = new SYOSApplication();
        app.start();
    }

    public void start() {
        DatabaseManager dbManager = DatabaseManager.getInstance("syos.db");
        ProductRepository productRepository = new SQLiteProductRepository(dbManager);
        StockRepository stockRepository = new SQLiteStockRepository(dbManager);
        BillRepository billRepository = new SQLiteBillRepository(dbManager);
        UserRepository userRepository = new SQLiteUserRepository(dbManager);

        ProductFactory productFactory = new ProductFactory();
        StockBatchFactory stockBatchFactory = new StockBatchFactory();
        BillNumberService billNumberService = BillNumberGenerator.getInstance();
        BillCalculationService billCalculationService = new BillCalculationService();
        InventoryManager inventoryManager = new InventoryManager(stockRepository, stockRepository);
        AddProductUseCase addProductUseCase = new AddProductUseCase(productRepository);

        StockSelectionStrategy stockSelectionStrategy = new FEFOStockSelectionStrategy();
        DiscountStrategy inStoreDiscountStrategy = new NoDiscountStrategy();
        DiscountStrategy onlineDiscountStrategy = new ThresholdDiscountStrategy(
            new Money(1000),
            new Money(50)
        );

        IReportGenerator dailySalesReport = new DailySalesReport(billRepository, LocalDate.now());
        IReportGenerator stockStatusReport = new StockStatusReport(stockRepository, productRepository);

        BillPrinter billPrinter = new BillPrinter();
        UserService userService = new UserService(userRepository);

        initializeSampleData(productFactory, stockBatchFactory, addProductUseCase, inventoryManager);

        SYOSGuiApplication guiApplication = new SYOSGuiApplication(
            productRepository,
            billRepository,
            stockRepository,
            inventoryManager,
            addProductUseCase,
            productFactory,
            stockBatchFactory,
            billNumberService,
            billCalculationService,
            stockSelectionStrategy,
            inStoreDiscountStrategy,
            onlineDiscountStrategy,
            billPrinter,
            dailySalesReport,
            stockStatusReport,
            userService
        );

        guiApplication.start("System initialization completed successfully");
    }

    private void initializeSampleData(ProductFactory productFactory,
                                      StockBatchFactory stockBatchFactory,
                                      AddProductUseCase addProductUseCase,
                                      InventoryManager inventoryManager) {
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

        StockBatch riceBatch1 = stockBatchFactory.createBatch("B001", "P001", com.syos.domain.valueobject.InventoryChannel.STORE, 100, LocalDate.now().plusMonths(6));
        StockBatch riceBatch2 = stockBatchFactory.createBatch("B002", "P001", com.syos.domain.valueobject.InventoryChannel.ONLINE, 50, LocalDate.now().plusMonths(8));
        StockBatch milkBatch = stockBatchFactory.createBatch("B003", "P002", com.syos.domain.valueobject.InventoryChannel.STORE, 80, LocalDate.now().plusDays(5));
        StockBatch breadBatch = stockBatchFactory.createBatch("B004", "P003", com.syos.domain.valueobject.InventoryChannel.STORE, 40, LocalDate.now().plusDays(2));
        StockBatch oilBatch = stockBatchFactory.createBatch("B005", "P004", com.syos.domain.valueobject.InventoryChannel.ONLINE, 60, LocalDate.now().plusMonths(12));
        StockBatch sugarBatch = stockBatchFactory.createBatch("B006", "P005", com.syos.domain.valueobject.InventoryChannel.ONLINE, 120, LocalDate.now().plusMonths(10));

        inventoryManager.addStock(riceBatch1, com.syos.domain.valueobject.InventoryChannel.STORE);
        inventoryManager.addStock(riceBatch2, com.syos.domain.valueobject.InventoryChannel.ONLINE);
        inventoryManager.addStock(milkBatch, com.syos.domain.valueobject.InventoryChannel.STORE);
        inventoryManager.addStock(breadBatch, com.syos.domain.valueobject.InventoryChannel.STORE);
        inventoryManager.addStock(oilBatch, com.syos.domain.valueobject.InventoryChannel.ONLINE);
        inventoryManager.addStock(sugarBatch, com.syos.domain.valueobject.InventoryChannel.ONLINE);
    }
}
