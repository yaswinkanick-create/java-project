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

final class DashboardPanel extends AppPanel {
            private final JLabel products = new JLabel("0");
            private final JLabel units = new JLabel("0");
            private final JLabel lowStock = new JLabel("0");
            private final JLabel stockValue = new JLabel("\u20B90");
            private final JLabel netSales = new JLabel("\u20B90");
            private final DefaultTableModel recentModel = readOnlyModel(new Object[]{"Date", "Type", "Reference", "Product", "Qty", "Amount"});
            private final SalesTrendChart salesTrendChart = new SalesTrendChart();
            private final StockStatusPieChart stockStatusPieChart = new StockStatusPieChart();
            private final StockAlertBanner stockAlertBanner;

            DashboardPanel(MainFrame app) {
            super(app);
                stockAlertBanner = new StockAlertBanner(app);
                setLayout(new BorderLayout(0, 12));
                setBackground(UiTheme.CANVAS);
                setBorder(BorderFactory.createEmptyBorder(16, 4, 4, 4));
                JLabel heading = new JLabel("Business overview");
                heading.setFont(new Font("SansSerif", Font.BOLD, 20));
                heading.setForeground(UiTheme.INK);
                JLabel description = new JLabel("A live summary of stock, sales and purchase activity stored on this device.");
                description.setForeground(UiTheme.MUTED);
                JPanel titleArea = new JPanel(new GridLayout(2, 1, 0, 4));
                titleArea.setOpaque(false);
                titleArea.add(heading);
                titleArea.add(description);
                JPanel metrics = new JPanel(new GridLayout(1, 5, 12, 0));
                metrics.setOpaque(false);
                metrics.add(card("Products", products, UiTheme.ACCENT));
                metrics.add(card("Units on hand", units, UiTheme.TEAL));
                metrics.add(card("Low stock", lowStock, UiTheme.AMBER));
                metrics.add(card("Stock value", stockValue, UiTheme.PURPLE));
                metrics.add(card("Net sales", netSales, UiTheme.SUCCESS));
                JPanel top = new JPanel(new BorderLayout(0, 12));
                top.setOpaque(false);
                top.add(titleArea, BorderLayout.NORTH);
                top.add(metrics, BorderLayout.CENTER);
                top.add(stockAlertBanner, BorderLayout.SOUTH);

                JPanel charts = new JPanel(new GridLayout(1, 2, 12, 0));
                charts.setOpaque(false);
                charts.add(salesTrendChart);
                charts.add(stockStatusPieChart);

                JTable recent = styledTable(recentModel);
                JPanel recentPanel = new JPanel(new BorderLayout(0, 8));
                recentPanel.setOpaque(false);
                JLabel recentLabel = new JLabel("Recent activity");
                recentLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
                recentLabel.setForeground(UiTheme.INK);
                recentPanel.add(recentLabel, BorderLayout.NORTH);
                recentPanel.add(scroll(recent), BorderLayout.CENTER);

                JSplitPane lowerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, charts, recentPanel);
                lowerSplit.setBorder(null);
                lowerSplit.setOpaque(false);
                lowerSplit.setContinuousLayout(true);
                lowerSplit.setResizeWeight(0.52);
                lowerSplit.setDividerSize(7);
                lowerSplit.setDividerLocation(265);
                add(top, BorderLayout.NORTH);
                add(lowerSplit, BorderLayout.CENTER);
            }
            void refresh() {
                products.setText(Integer.toString(service.getProductCount()));
                units.setText(Integer.toString(service.getTotalUnits()));
                lowStock.setText(Integer.toString(service.getLowStockCount()));
                stockAlertBanner.refresh(service.getLowStockProducts());
                stockValue.setText(currency(service.getStockValue()));
                netSales.setText(currency(service.getNetSales()));
                List<Product> currentProducts = service.getProducts();
                List<TransactionRecord> currentTransactions = service.getTransactions();
                salesTrendChart.updateData(currentTransactions);
                stockStatusPieChart.updateData(currentProducts);
                recentModel.setRowCount(0);
                for (TransactionRecord t : currentTransactions.stream().limit(10).toList()) {
                    recentModel.addRow(new Object[]{t.getDateTime(), t.getType(), t.getReferenceId(),
                            t.getProductName(), t.getQuantity(), currency(t.getTotal())});
                }
            }
        }
