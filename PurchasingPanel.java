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

final class PurchasingPanel extends AppPanel {
            private final JComboBox<ProductChoice> productBox = new JComboBox<>();
            private final JComboBox<SupplierChoice> supplierBox = new JComboBox<>();
            private final JTextField orderedQuantity = new JTextField("1", 6);
            private final JTextField unitCost = new JTextField("0.00", 8);
            private final JTextField note = new JTextField(14);
            private final DefaultTableModel orderModel = readOnlyModel(new Object[]{"Order ID", "Created", "Product", "Supplier", "Ordered", "Received", "Remaining", "Unit cost", "Status", "Note"});
            private final JTable orderTable = styledTable(orderModel);
            private final DefaultTableModel receiptModel = readOnlyModel(new Object[]{"Date", "Receipt ID", "Purchase order", "Product", "Qty received", "Total", "Supplier", "Note"});

            PurchasingPanel(MainFrame app) {
            super(app);
                JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
                form.setBackground(Color.WHITE);
                form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.BORDER),
                        BorderFactory.createEmptyBorder(7, 8, 7, 8)));
                form.add(new JLabel("Product")); form.add(productBox);
                form.add(new JLabel("Supplier")); form.add(supplierBox);
                form.add(new JLabel("Qty ordered")); form.add(orderedQuantity);
                form.add(new JLabel("Unit cost \u20B9")); form.add(unitCost);
                form.add(new JLabel("Note")); form.add(note);
                form.add(primary("Create draft order", e -> createOrder()));
                productBox.addActionListener(e -> updateCostFromProduct());

                JPanel top = new JPanel(new BorderLayout(0, 10));
                top.setOpaque(false);
                JLabel description = new JLabel("Create a draft, mark it as ordered, then receive deliveries. Partial deliveries update stock only when accepted.");
                description.setForeground(UiTheme.MUTED);
                top.add(description, BorderLayout.NORTH);
                top.add(form, BorderLayout.CENTER);

                JPanel orderPanel = new JPanel(new BorderLayout(0, 8));
                orderPanel.setBackground(UiTheme.CANVAS);
                JPanel orderToolbar = toolbar();
                orderToolbar.add(primary("Mark as ordered", e -> markSelectedOrdered()));
                orderToolbar.add(secondary("Receive delivery", e -> receiveDelivery()));
                orderToolbar.add(secondary("Cancel order", e -> cancelSelectedOrder()));
                orderToolbar.add(secondary("Refresh", e -> refresh()));
                JLabel ordersTitle = new JLabel("Purchase orders");
                ordersTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
                ordersTitle.setForeground(UiTheme.INK);
                JPanel orderHeading = new JPanel(new BorderLayout(0, 6));
                orderHeading.setOpaque(false);
                orderHeading.add(ordersTitle, BorderLayout.NORTH);
                orderHeading.add(orderToolbar, BorderLayout.CENTER);
                orderPanel.add(orderHeading, BorderLayout.NORTH);
                orderPanel.add(scroll(orderTable), BorderLayout.CENTER);

                JPanel receiptsPanel = new JPanel(new BorderLayout(0, 8));
                receiptsPanel.setBackground(UiTheme.CANVAS);
                JLabel receiptsTitle = new JLabel("Received goods history");
                receiptsTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
                receiptsTitle.setForeground(UiTheme.INK);
                receiptsPanel.add(receiptsTitle, BorderLayout.NORTH);
                receiptsPanel.add(scroll(styledTable(receiptModel)), BorderLayout.CENTER);

                javax.swing.JSplitPane split = new javax.swing.JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT, orderPanel, receiptsPanel);
                split.setResizeWeight(0.66);
                split.setDividerLocation(0.64);
                split.setBorder(null);
                split.setOpaque(false);

                setLayout(new BorderLayout(0, 12));
                setBackground(UiTheme.CANVAS);
                setBorder(BorderFactory.createEmptyBorder(16, 4, 4, 4));
                add(top, BorderLayout.NORTH);
                add(split, BorderLayout.CENTER);
            }

            private void updateCostFromProduct() {
                ProductChoice selected = (ProductChoice) productBox.getSelectedItem();
                if (selected != null) {
                    Product product = service.findProduct(selected.id());
                    if (product != null) unitCost.setText(String.format(Locale.ROOT, "%.2f", product.getUnitCost()));
                }
            }

            private void createOrder() {
                ProductChoice product = (ProductChoice) productBox.getSelectedItem();
                SupplierChoice supplier = (SupplierChoice) supplierBox.getSelectedItem();
                if (product == null) { showInfo("Add a product before creating a purchase order."); return; }
                if (supplier == null) { showInfo("Add a supplier before creating a purchase order."); return; }
                final int qty;
                final double cost;
                try {
                    qty = parseInt(orderedQuantity.getText(), "Ordered quantity");
                    cost = parseDouble(unitCost.getText(), "Unit cost");
                } catch (IllegalArgumentException exception) { showError(exception.getMessage()); return; }
                perform("Purchase order creation", () -> {
                    String orderId = service.createPurchaseOrder(product.id(), supplier.id(), qty, cost, note.getText());
                    orderedQuantity.setText("1");
                    note.setText("");
                    showInfo("Draft purchase order created.\nOrder ID: " + orderId + "\nNo stock has been added yet.");
                });
            }

            private String selectedOrderId() {
                int row = orderTable.getSelectedRow();
                if (row < 0) return null;
                return String.valueOf(orderModel.getValueAt(orderTable.convertRowIndexToModel(row), 0));
            }

            private void markSelectedOrdered() {
                String id = selectedOrderId();
                if (id == null) { showInfo("Select a purchase order first."); return; }
                perform("Purchase order status update", () -> service.markPurchaseOrderOrdered(id));
            }

            private void cancelSelectedOrder() {
                String id = selectedOrderId();
                if (id == null) { showInfo("Select a purchase order first."); return; }
                int answer = JOptionPane.showConfirmDialog(app,
                        "Cancel the outstanding quantity on " + id + "? Any quantity already received will remain in stock.",
                        "Confirm order cancellation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (answer == JOptionPane.OK_OPTION) perform("Purchase order cancellation", () -> service.cancelPurchaseOrder(id));
            }

            private void receiveDelivery() {
                int viewRow = orderTable.getSelectedRow();
                if (viewRow < 0) { showInfo("Select a purchase order first."); return; }
                int row = orderTable.convertRowIndexToModel(viewRow);
                String id = String.valueOf(orderModel.getValueAt(row, 0));
                String status = String.valueOf(orderModel.getValueAt(row, 8));
                if (status.equals("CANCELLED")) { showInfo("This purchase order was cancelled; no further delivery can be received."); return; }
                String remainingText = String.valueOf(orderModel.getValueAt(row, 6));
                int remaining;
                try { remaining = Integer.parseInt(remainingText); }
                catch (NumberFormatException exception) { showError("Could not read the outstanding quantity for this order."); return; }
                if (remaining <= 0) { showInfo("This purchase order has already been fully received."); return; }
                String input = JOptionPane.showInputDialog(this,
                        "Enter the quantity received (outstanding: " + remaining + "):", Integer.toString(remaining));
                if (input == null) return;
                final int received;
                try { received = parseInt(input, "Received quantity"); }
                catch (IllegalArgumentException exception) { showError(exception.getMessage()); return; }
                String deliveryNote = JOptionPane.showInputDialog(this, "Delivery note (optional):", "");
                if (deliveryNote == null) return;
                perform("Purchase delivery", () -> {
                    String receiptId = service.receivePurchaseOrderDelivery(id, received, deliveryNote);
                    showInfo("Delivery recorded.\nReceipt ID: " + receiptId + "\nStock has been updated for the accepted quantity.");
                });
            }
            void refresh() {
                String previousProduct = productBox.getSelectedItem() instanceof ProductChoice p ? p.id() : null;
                String previousSupplier = supplierBox.getSelectedItem() instanceof SupplierChoice s ? s.id() : null;
                productBox.removeAllItems();
                for (Product p : service.getProducts()) {
                    productBox.addItem(new ProductChoice(p.getId(), p.getId() + " - " + p.getName() + " (" + p.getQuantity() + " on hand)"));
                }
                supplierBox.removeAllItems();
                for (Supplier s : service.getSuppliers()) supplierBox.addItem(new SupplierChoice(s.getId(), s.getId() + " - " + s.getName()));
                selectProduct(previousProduct);
                selectSupplier(previousSupplier);
                updateCostFromProduct();

                orderModel.setRowCount(0);
                for (var order : service.getPurchaseOrders()) {
                    orderModel.addRow(new Object[]{order.getId(), order.getCreatedAt(), order.getProductName(), order.getSupplierName(),
                            order.getOrderedQuantity(), order.getReceivedQuantity(), order.getRemainingQuantity(),
                            currency(order.getUnitCost()), order.getStatus(), order.getNote()});
                }
                receiptModel.setRowCount(0);
                for (TransactionRecord t : service.getTransactionsByType("PURCHASE")) {
                    receiptModel.addRow(new Object[]{t.getDateTime(), t.getReferenceId(), t.getNote().split(" | ", 2)[0],
                            t.getProductName(), t.getQuantity(), currency(t.getTotal()), t.getPartyName(), t.getNote()});
                }
            }

            private void selectProduct(String id) {
                if (id == null) return;
                for (int i = 0; i < productBox.getItemCount(); i++) if (productBox.getItemAt(i).id().equals(id)) { productBox.setSelectedIndex(i); return; }
            }
            private void selectSupplier(String id) {
                if (id == null) return;
                for (int i = 0; i < supplierBox.getItemCount(); i++) if (supplierBox.getItemAt(i).id().equals(id)) { supplierBox.setSelectedIndex(i); return; }
            }
        }
