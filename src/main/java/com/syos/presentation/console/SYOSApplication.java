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
import com.syos.application.strategy.DiscountStrategy;
import com.syos.application.strategy.FEFOStockSelectionStrategy;
import com.syos.application.strategy.NoDiscountStrategy;
import com.syos.application.strategy.StockSelectionStrategy;
import com.syos.application.strategy.ThresholdDiscountStrategy;
import com.syos.application.usecase.AddProductUseCase;
import com.syos.domain.repository.BillRepository;
import com.syos.domain.repository.ProductRepository;
import com.syos.domain.repository.StockRepository;
import com.syos.domain.valueobject.Money;
import com.syos.infrastructure.database.DatabaseManager;
import com.syos.infrastructure.persistence.SQLiteBillRepository;
import com.syos.infrastructure.persistence.SQLiteProductRepository;
import com.syos.infrastructure.persistence.SQLiteStockRepository;

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

        SYOSConsoleUI consoleUI = new SYOSConsoleUI(
            productRepository,
            billRepository,
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
            stockStatusReport
        );

        consoleUI.run();
    }
}
