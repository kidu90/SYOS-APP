package com.syos.presentation.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.syos.application.factory.ProductFactory;
import com.syos.application.factory.StockBatchFactory;
import com.syos.application.report.IReportGenerator;
import com.syos.application.service.BillCalculationService;
import com.syos.application.service.BillNumberService;
import com.syos.application.service.InventoryManager;
import com.syos.application.service.UserService;
import com.syos.application.strategy.DiscountStrategy;
import com.syos.application.strategy.StockSelectionStrategy;
import com.syos.application.usecase.AddProductUseCase;
import com.syos.domain.entity.Bill;
import com.syos.domain.entity.User;
import com.syos.domain.repository.BillRepository;
import com.syos.domain.repository.ProductRepository;
import com.syos.domain.repository.StockRepository;
import com.syos.presentation.console.BillPrinter;

public class SYOSGuiApplication {
    private final ProductRepository productRepository;
    private final BillRepository billRepository;
    private final StockRepository stockRepository;
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
    private final UserService userService;

    private JFrame frame;
    private JLabel headerLabel;
    private JLabel statusLabel;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private User currentUser;

    private InStoreTransactionPanel inStorePanel;
    private OnlineTransactionPanel onlinePanel;
    private ProductsPanel productsPanel;
    private ReportsPanel reportsPanel;

    public SYOSGuiApplication(ProductRepository productRepository,
                              BillRepository billRepository,
                              StockRepository stockRepository,
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
                              IReportGenerator stockStatusReport,
                              UserService userService) {
        this.productRepository = productRepository;
        this.billRepository = billRepository;
        this.stockRepository = stockRepository;
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
        this.userService = userService;
    }

    public void start(String startupMessage) {
        configureLookAndFeel();
        SwingUtilities.invokeLater(() -> buildAndShow(startupMessage));
    }

    public JFrame getFrame() {
        return frame;
    }

    public ProductRepository getProductRepository() {
        return productRepository;
    }

    public BillRepository getBillRepository() {
        return billRepository;
    }

    public StockRepository getStockRepository() {
        return stockRepository;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public AddProductUseCase getAddProductUseCase() {
        return addProductUseCase;
    }

    public ProductFactory getProductFactory() {
        return productFactory;
    }

    public StockBatchFactory getStockBatchFactory() {
        return stockBatchFactory;
    }

    public BillNumberService getBillNumberService() {
        return billNumberService;
    }

    public BillCalculationService getBillCalculationService() {
        return billCalculationService;
    }

    public StockSelectionStrategy getStockSelectionStrategy() {
        return stockSelectionStrategy;
    }

    public DiscountStrategy getInStoreDiscountStrategy() {
        return inStoreDiscountStrategy;
    }

    public DiscountStrategy getOnlineDiscountStrategy() {
        return onlineDiscountStrategy;
    }

    public BillPrinter getBillPrinter() {
        return billPrinter;
    }

    public IReportGenerator getDailySalesReport() {
        return dailySalesReport;
    }

    public IReportGenerator getStockStatusReport() {
        return stockStatusReport;
    }

    public UserService getUserService() {
        return userService;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        updateHeader();
    }

    public void setStatusMessage(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message == null ? "" : message);
        }
    }

    public void showSuccess(String title, String message) {
        showMessageDialog(title, message, new Color(240, 249, 244));
    }

    public void showError(String title, String message) {
        showMessageDialog(title, message, new Color(252, 240, 240));
    }

    public void showReceipt(Bill bill) {
        ReceiptDialog.show(frame, bill);
    }

    public void showReportText(String title, String text) {
        JDialog dialog = new JDialog(frame, title, true);
        javax.swing.JTextArea textArea = new javax.swing.JTextArea(text);
        textArea.setEditable(false);
        textArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        dialog.add(new javax.swing.JScrollPane(textArea), BorderLayout.CENTER);
        dialog.setSize(820, 520);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    public User showAuthDialog() {
        AuthDialog dialog = new AuthDialog(frame, this);
        dialog.setVisible(true);
        return dialog.getAuthenticatedUser();
    }

    public void logout() {
        currentUser = null;
        if (frame != null) {
            frame.setVisible(false);
        }
        showAuthenticationAndDashboard();
    }

    public void showCard(String cardName) {
        if (contentPanel != null) {
            cardLayout.show(contentPanel, cardName);
        }
    }

    public void refreshTransactionPanels() {
        if (inStorePanel != null) {
            inStorePanel.onShow();
        }
        if (onlinePanel != null) {
            onlinePanel.onShow();
        }
        if (productsPanel != null) {
            productsPanel.onShow();
        }
    }

    private void buildAndShow(String startupMessage) {
        frame = new JFrame("SYOS - Synex Outlet Store");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1180, 760));
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x1F3864));
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JLabel title = new JLabel("SYOS - Synex Outlet Store");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        headerLabel = new JLabel("Logged in as: ");
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
        header.add(title, BorderLayout.WEST);
        header.add(headerLabel, BorderLayout.EAST);

        JPanel sidebar = new JPanel();
        sidebar.setBackground(new Color(0x1F3864));
        sidebar.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(250, 0));

        JButton inStoreButton = sidebarButton("In-Store Transaction", e -> showCard("IN_STORE"));
        JButton onlineButton = sidebarButton("Online Transaction", e -> {
            if (currentUser == null) {
                User user = showAuthDialog();
                if (user == null) {
                    return;
                }
                setCurrentUser(user);
            }
            showCard("ONLINE");
            onlinePanel.onShow();
        });
        JButton productsButton = sidebarButton("View Products", e -> {
            showCard("PRODUCTS");
            productsPanel.onShow();
        });
        JButton reportsButton = sidebarButton("View Reports", e -> {
            showCard("REPORTS");
            reportsPanel.onShow();
        });
        JButton logoutButton = sidebarButton("Logout/Exit", e -> logout());

        sidebar.add(inStoreButton);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(onlineButton);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(productsButton);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(reportsButton);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(logoutButton);

        contentPanel = new JPanel(new CardLayout());
        cardLayout = (CardLayout) contentPanel.getLayout();

        inStorePanel = new InStoreTransactionPanel(this);
        onlinePanel = new OnlineTransactionPanel(this);
        productsPanel = new ProductsPanel(this);
        reportsPanel = new ReportsPanel(this);

        contentPanel.add(inStorePanel, "IN_STORE");
        contentPanel.add(onlinePanel, "ONLINE");
        contentPanel.add(productsPanel, "PRODUCTS");
        contentPanel.add(reportsPanel, "REPORTS");

        JPanel center = new JPanel(new BorderLayout());
        center.add(contentPanel, BorderLayout.CENTER);

        frame.add(header, BorderLayout.NORTH);
        frame.add(sidebar, BorderLayout.WEST);
        frame.add(center, BorderLayout.CENTER);

        statusLabel = new JLabel(startupMessage == null ? "" : startupMessage);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(245, 247, 250));
        frame.add(statusLabel, BorderLayout.SOUTH);

        showAuthenticationAndDashboard();
    }

    private void showAuthenticationAndDashboard() {
        User user = showAuthDialog();
        if (user == null) {
            if (frame != null) {
                frame.dispose();
            }
            return;
        }
        setCurrentUser(user);
        frame.setVisible(true);
        showCard("IN_STORE");
        refreshTransactionPanels();
    }

    private void updateHeader() {
        if (headerLabel != null) {
            headerLabel.setText(currentUser == null ? "Logged in as: " : "Logged in as: " + currentUser.getFullName());
        }
    }

    private JButton sidebarButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setBackground(new Color(0x2E4B7A));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.addActionListener(listener);
        return button;
    }

    private void showMessageDialog(String title, String message, Color background) {
        JDialog dialog = new JDialog(frame, title, true);
        javax.swing.JTextArea textArea = new javax.swing.JTextArea(message);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        textArea.setBackground(background);
        dialog.add(textArea);
        dialog.setSize(480, 260);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void configureLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to default look and feel when Nimbus is unavailable.
        }
    }
}
