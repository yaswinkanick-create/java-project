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

final class SalesPanel extends AppPanel {
            private final JComboBox<ProductChoice> productBox = new JComboBox<>();
            private final JTextField quantityField = new JTextField("1", 5);
            private final LinkedHashMap<String, CartLine> cart = new LinkedHashMap<>();
            private final DefaultTableModel cartModel = readOnlyModel(new Object[]{"Product ID", "Product", "Qty", "Unit price", "Line total"});
            private final JTable cartTable = styledTable(cartModel);
            private final JTextField customerField = new JTextField("", 15);
            private final JComboBox<String> paymentBox = new JComboBox<>(new String[]{"Cash", "UPI", "Card", "Bank transfer"});
            private final JTextField discountField = new JTextField("0", 5);
            private final JLabel totalLabel = new JLabel("Total: \u20B90.00");

            SalesPanel(MainFrame app) {
            super(app);
                JPanel addLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 7));
                addLine.setBackground(Color.WHITE);
                addLine.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.BORDER),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)));
                addLine.add(new JLabel("Product")); addLine.add(productBox);
                addLine.add(new JLabel("Qty")); addLine.add(quantityField);
                addLine.add(primary("Add to bill", e -> addToCart()));

                JPanel invoiceDetails = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 7));
                invoiceDetails.setBackground(Color.WHITE);
                invoiceDetails.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.BORDER),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)));
                invoiceDetails.add(new JLabel("Customer (optional)")); invoiceDetails.add(customerField);
                invoiceDetails.add(new JLabel("Payment")); invoiceDetails.add(paymentBox);
                invoiceDetails.add(new JLabel("Discount %")); invoiceDetails.add(discountField);

                JPanel toolbar = toolbar();
                toolbar.add(secondary("Remove selected line", e -> removeFromCart()));
                toolbar.add(secondary("Clear bill", e -> { cart.clear(); refreshCart(); }));
                totalLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
                totalLabel.setForeground(UiTheme.INK);
                discountField.getDocument().addDocumentListener(new DocumentListener() {
                    private void update() { refreshCart(); }
                    @Override public void insertUpdate(DocumentEvent event) { update(); }
                    @Override public void removeUpdate(DocumentEvent event) { update(); }
                    @Override public void changedUpdate(DocumentEvent event) { update(); }
                });
                JButton complete = primary("Complete sale", e -> completeSale());
                JPanel bottom = new JPanel(new BorderLayout(12, 0));
                bottom.setBackground(UiTheme.CANVAS);
                bottom.add(totalLabel, BorderLayout.WEST);
                bottom.add(complete, BorderLayout.EAST);
                JPanel north = new JPanel(new GridLayout(3, 1, 0, 8));
                north.setOpaque(false);
                north.add(addLine);
                north.add(invoiceDetails);
                north.add(toolbar);
                JPanel center = new JPanel(new BorderLayout(0, 10));
                center.setOpaque(false);
                JLabel hint = new JLabel("Add one or more products, verify quantities, then complete the sale to create a bill record.");
                hint.setForeground(UiTheme.MUTED);
                center.add(hint, BorderLayout.NORTH);
                center.add(scroll(cartTable), BorderLayout.CENTER);
                center.add(bottom, BorderLayout.SOUTH);
                setLayout(new BorderLayout(0, 12));
                setBackground(UiTheme.CANVAS);
                setBorder(BorderFactory.createEmptyBorder(16, 4, 4, 4));
                add(north, BorderLayout.NORTH);
                add(center, BorderLayout.CENTER);
            }

            private void addToCart() {
                ProductChoice selected = (ProductChoice) productBox.getSelectedItem();
                if (selected == null) { showInfo("Add a product first."); return; }
                int quantity;
                try { quantity = parseInt(quantityField.getText(), "Quantity"); }
                catch (IllegalArgumentException exception) { showError(exception.getMessage()); return; }
                Product product = service.findProduct(selected.id());
                if (product == null) { showError("That product is no longer available."); return; }
                if (quantity <= 0) { showError("Quantity must be greater than zero."); return; }
                int existingQty = cart.containsKey(product.getId()) ? cart.get(product.getId()).quantity() : 0;
                if (existingQty + quantity > product.getQuantity()) {
                    showError("Insufficient stock. Available: " + product.getQuantity() + ". Already in bill: " + existingQty + ".");
                    return;
                }
                cart.put(product.getId(), new CartLine(product.getId(), product.getName(), existingQty + quantity, product.getSellingPrice()));
                quantityField.setText("1");
                refreshCart();
            }

            private void removeFromCart() {
                int row = cartTable.getSelectedRow();
                if (row < 0) { showInfo("Select a bill line to remove."); return; }
                String id = String.valueOf(cartModel.getValueAt(cartTable.convertRowIndexToModel(row), 0));
                cart.remove(id);
                refreshCart();
            }

            private void refreshCart() {
                cartModel.setRowCount(0);
                double gross = 0;
                for (CartLine line : cart.values()) {
                    cartModel.addRow(new Object[]{line.productId(), line.name(), line.quantity(), currency(line.price()), currency(line.total())});
                    gross += line.total();
                }
                double discount = 0;
                try { discount = Double.parseDouble(discountField.getText().trim()); } catch (RuntimeException ignored) { }
                if (discount < 0 || discount > 100) discount = 0;
                totalLabel.setText("Total: " + currency(gross * (1 - discount / 100.0)));
            }

            private void completeSale() {
                if (cart.isEmpty()) { showInfo("Add at least one item to the bill."); return; }
                List<InventoryService.CartItem> items = new ArrayList<>();
                for (CartLine line : cart.values()) items.add(new InventoryService.CartItem(line.productId(), line.quantity()));
                final double discount;
                try { discount = parseDouble(discountField.getText(), "Discount"); }
                catch (IllegalArgumentException exception) { showError(exception.getMessage()); return; }
                double net = cart.values().stream().mapToDouble(CartLine::total).sum() * (1 - discount / 100.0);
                int confirm = JOptionPane.showConfirmDialog(app,
                        "Complete this sale for " + currency(net) + "?\nThis will reduce stock and save the bill.",
                        "Confirm sale", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (confirm != JOptionPane.OK_OPTION) return;
                perform("Sale", () -> {
                    String id = service.createSale(items, customerField.getText(), String.valueOf(paymentBox.getSelectedItem()), discount);
                    cart.clear();
                    customerField.setText("");
                    discountField.setText("0");
                    refreshCart();
                    showInfo("Sale saved successfully.\nBill reference: " + id + "\nNet total: " + currency(net));
                });
            }
            void refreshProducts() {
                String previous = productBox.getSelectedItem() instanceof ProductChoice p ? p.id() : null;
                productBox.removeAllItems();
                for (Product p : service.getProducts()) {
                    if (p.getQuantity() > 0) productBox.addItem(new ProductChoice(p.getId(), p.getId() + " - " + p.getName() + " (" + p.getQuantity() + " available)"));
                }
                if (previous != null) {
                    for (int i = 0; i < productBox.getItemCount(); i++) if (productBox.getItemAt(i).id().equals(previous)) { productBox.setSelectedIndex(i); break; }
                }
                refreshCart();
            }
        }
