package com.syos.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import com.syos.domain.entity.Bill;
import com.syos.domain.entity.BillItem;

public final class ReceiptDialog {
    private ReceiptDialog() {
    }

    public static void show(JFrame owner, Bill bill) {
        JDialog dialog = new JDialog(owner, "Receipt", true);
        JTextArea textArea = new JTextArea(renderReceipt(bill));
        textArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        textArea.setEditable(false);
        textArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        dialog.setLayout(new BorderLayout());
        dialog.add(new JScrollPane(textArea), BorderLayout.CENTER);
        dialog.setSize(760, 520);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    public static String renderReceipt(Bill bill) {
        StringBuilder builder = new StringBuilder();
        builder.append("\n").append("-".repeat(80)).append('\n');
        builder.append(" ".repeat(25)).append("SYNEX OUTLET STORE\n");
        builder.append(" ".repeat(20)).append("GST No: 29XXXXX1234X1Z5\n");
        builder.append("-".repeat(80)).append('\n');
        builder.append("Bill No: ").append(bill.getBillNumber()).append('\n');
        builder.append("Date/Time: ").append(bill.getTimestamp()).append('\n');
        builder.append("Type: ").append(bill.getSaleType()).append('\n');

        if (bill.getSaleType() == Bill.SaleType.ONLINE) {
            builder.append("Customer: ").append(bill.getCustomerName()).append('\n');
            builder.append("Address: ").append(bill.getCustomerAddress()).append('\n');
        }

        builder.append("-".repeat(80)).append('\n');
        builder.append(String.format("%-30s %-8s %-12s %-12s\n", "Item", "Qty", "Rate", "Amount"));
        builder.append("-".repeat(80)).append('\n');

        for (BillItem item : bill.getItems()) {
            builder.append(String.format("%-30s %-8d %-12s %-12s\n",
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()));
        }

        builder.append("-".repeat(80)).append('\n');
        builder.append(String.format("%-50s %-12s\n", "Subtotal:", bill.getSubtotal()));
        builder.append(String.format("%-50s %-12s\n", "Discount:", bill.getDiscount()));
        builder.append(String.format("%-50s %-12s\n", "TOTAL:", bill.getTotal()));
        builder.append("-".repeat(80)).append('\n');
        return builder.toString();
    }
}
