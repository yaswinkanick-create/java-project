import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.text.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;

final class ReportsPanel extends AppPanel {
            private final JLabel sales = new JLabel("\u20B90");
            private final JLabel refunds = new JLabel("\u20B90");
            private final JLabel purchases = new JLabel("\u20B90");
            private final JLabel profit = new JLabel("\u20B90");
            private final JLabel stock = new JLabel("\u20B90");
            private final JComboBox<String> typeBox = new JComboBox<>(new String[]{"All activity", "PURCHASE", "SALE", "RETURN", "ADJUSTMENT"});
            private final DefaultTableModel model = readOnlyModel(new Object[]{"Date", "Type", "Reference", "Product", "Qty", "Unit price", "Total", "Party", "Note"});

            ReportsPanel(MainFrame app) {
            super(app);
                JPanel metrics = new JPanel(new GridLayout(1, 5, 10, 0));
                metrics.setOpaque(false);
                metrics.add(card("Sales before refunds", sales, UiTheme.ACCENT));
                metrics.add(card("Refunds", refunds, UiTheme.DANGER));
                metrics.add(card("Purchases", purchases, new Color(124, 58, 237)));
                metrics.add(card("Estimated gross profit", profit, UiTheme.SUCCESS));
                metrics.add(card("Current stock value", stock, new Color(13, 148, 136)));
                JPanel top = new JPanel(new BorderLayout(0, 12));
                top.setOpaque(false);
                JLabel note = new JLabel("Profit is an estimate based on recorded sale costs and restocked returns; it is not a full accounting report.");
                note.setForeground(UiTheme.MUTED);
                JPanel filter = toolbar();
                filter.add(new JLabel("Show")); filter.add(typeBox);
                top.add(metrics, BorderLayout.NORTH);
                top.add(note, BorderLayout.CENTER);
                top.add(filter, BorderLayout.SOUTH);
                typeBox.addActionListener(e -> refreshTable());
                setLayout(new BorderLayout(0, 12));
                setBackground(UiTheme.CANVAS);
                setBorder(BorderFactory.createEmptyBorder(16, 4, 4, 4));
                add(top, BorderLayout.NORTH);
                add(scroll(styledTable(model)), BorderLayout.CENTER);
            }
            void refresh() {
                sales.setText(currency(service.getSalesTotal()));
                refunds.setText(currency(service.getRefundTotal()));
                purchases.setText(currency(service.getPurchaseTotal()));
                profit.setText(currency(service.getGrossProfitEstimate()));
                stock.setText(currency(service.getStockValue()));
                refreshTable();
            }
            void refreshTable() {
                model.setRowCount(0);
                String type = String.valueOf(typeBox.getSelectedItem());
                for (TransactionRecord t : service.getTransactions()) {
                    if (!type.equals("All activity") && !type.equals(t.getType())) continue;
                    model.addRow(new Object[]{t.getDateTime(), t.getType(), t.getReferenceId(), t.getProductName(),
                            t.getQuantity(), currency(t.getUnitPrice()), currency(t.getTotal()), t.getPartyName(), t.getNote()});
                }
            }
        }
