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

final class StockPanel extends AppPanel {
            private final JComboBox<ProductChoice> productBox = new JComboBox<>();
            private final JTextField counted = new JTextField("0", 8);
            private final JTextField reason = new JTextField(22);
            private final DefaultTableModel model = readOnlyModel(new Object[]{"Date", "Adjustment ID", "Product", "Change", "New reason / note"});

            StockPanel(MainFrame app) {
            super(app);
                JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
                form.setBackground(Color.WHITE);
                form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.BORDER),
                        BorderFactory.createEmptyBorder(7, 8, 7, 8)));
                form.add(new JLabel("Product")); form.add(productBox);
                form.add(new JLabel("Physical count")); form.add(counted);
                form.add(new JLabel("Reason")); form.add(reason);
                form.add(primary("Save stock count", e -> saveCount()));
                productBox.addActionListener(e -> updateCountFromProduct());
                JPanel north = new JPanel(new BorderLayout(0, 10));
                north.setOpaque(false);
                JLabel desc = new JLabel("Compare physical stock with system stock. The adjustment is recorded with a reason.");
                desc.setForeground(UiTheme.MUTED);
                north.add(desc, BorderLayout.NORTH);
                north.add(form, BorderLayout.CENTER);
                setLayout(new BorderLayout(0, 12));
                setBackground(UiTheme.CANVAS);
                setBorder(BorderFactory.createEmptyBorder(16, 4, 4, 4));
                add(north, BorderLayout.NORTH);
                add(scroll(styledTable(model)), BorderLayout.CENTER);
            }

            private void saveCount() {
                ProductChoice choice = (ProductChoice) productBox.getSelectedItem();
                if (choice == null) { showInfo("Add a product first."); return; }
                int qty;
                try { qty = parseInt(counted.getText(), "Physical count"); }
                catch (IllegalArgumentException exception) { showError(exception.getMessage()); return; }
                if (reason.getText().isBlank()) { showError("Enter a reason for the stock adjustment."); return; }
                perform("Stock count", () -> {
                    service.adjustStock(choice.id(), qty, reason.getText());
                    reason.setText("");
                    updateCountFromProduct();
                    showInfo("Stock count saved and adjustment recorded.");
                });
            }

            private void updateCountFromProduct() {
                ProductChoice selected = (ProductChoice) productBox.getSelectedItem();
                if (selected != null) {
                    Product p = service.findProduct(selected.id());
                    if (p != null) counted.setText(Integer.toString(p.getQuantity()));
                }
            }
            void refresh() {
                String previous = productBox.getSelectedItem() instanceof ProductChoice p ? p.id() : null;
                productBox.removeAllItems();
                for (Product p : service.getProducts()) productBox.addItem(new ProductChoice(p.getId(), p.getId() + " - " + p.getName() + " (system: " + p.getQuantity() + ")"));
                if (previous != null) for (int i = 0; i < productBox.getItemCount(); i++) if (productBox.getItemAt(i).id().equals(previous)) { productBox.setSelectedIndex(i); break; }
                updateCountFromProduct();
                model.setRowCount(0);
                for (TransactionRecord t : service.getTransactionsByType("ADJUSTMENT")) {
                    model.addRow(new Object[]{t.getDateTime(), t.getReferenceId(), t.getProductName(), signed(t.getQuantity()), t.getNote()});
                }
            }

            private String signed(int number) { return number > 0 ? "+" + number : Integer.toString(number); }
        }
