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

final class RestockRequestsPanel extends AppPanel {
            private final DefaultTableModel model = readOnlyModel(new Object[]{"Request ID", "Created", "Customer", "Product", "Qty needed", "Status", "Order ID"});
            private final JTable table = styledTable(model);

            RestockRequestsPanel(MainFrame app) {
            super(app);
                JPanel actions = toolbar();
                actions.add(primary("Create purchase order for selected request", e -> createPurchaseOrderForRequest()));
                actions.add(secondary("Refresh", e -> refresh()));
                JLabel description = new JLabel("Customer orders awaiting stock appear here. Create a purchase order, mark it ordered in Purchasing, and record delivery to update stock.");
                description.setForeground(UiTheme.MUTED);
                JPanel content = new JPanel(new BorderLayout(0, 8));
                content.setOpaque(false);
                content.add(description, BorderLayout.NORTH);
                content.add(scroll(table), BorderLayout.CENTER);
                setLayout(new BorderLayout(0, 12));
                setBackground(UiTheme.CANVAS);
                setBorder(BorderFactory.createEmptyBorder(16, 4, 4, 4));
                add(actions, BorderLayout.NORTH);
                add(content, BorderLayout.CENTER);
            }

            private void createPurchaseOrderForRequest() {
                int selected = table.getSelectedRow();
                if (selected < 0) { showInfo("Select a restock request first."); return; }
                int row = table.convertRowIndexToModel(selected);
                String requestId = String.valueOf(model.getValueAt(row, 0));
                RestockRequest request = customerData.findRestockRequest(requestId);
                if (request == null) { showError("That restock request no longer exists."); return; }
                if (!request.getStatus().equals("OPEN")) { showInfo("Only OPEN restock requests can create a purchase order."); return; }
                List<Supplier> suppliers = service.getSuppliers();
                if (suppliers.isEmpty()) { showInfo("Add a supplier in the Supplier module first."); return; }
                JComboBox<SupplierChoice> supplierBox = new JComboBox<>();
                for (Supplier supplier : suppliers) supplierBox.addItem(new SupplierChoice(supplier.getId(), supplier.getId() + " - " + supplier.getName()));
                JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
                form.add(new JLabel("Restock request")); form.add(new JLabel(request.getId()));
                form.add(new JLabel("Product")); form.add(new JLabel(request.getProductName()));
                form.add(new JLabel("Shortage quantity")); form.add(new JLabel(Integer.toString(request.getQuantityNeeded())));
                form.add(new JLabel("Supplier")); form.add(supplierBox);
                if (JOptionPane.showConfirmDialog(app, form, "Create purchase order", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
                SupplierChoice supplierChoice = (SupplierChoice) supplierBox.getSelectedItem();
                if (supplierChoice == null) return;
                Product product = service.findProduct(request.getProductId());
                if (product == null) { showError("The requested product no longer exists."); return; }
                perform("Restock purchase order", () -> {
                    service.createPurchaseOrder(request.getProductId(), supplierChoice.id(), request.getQuantityNeeded(),
                            product.getUnitCost(), "Restock request " + request.getId());
                    customerData.markRestockOrdered(request.getId());
                    app.supplierTabs.setSelectedIndex(1); // move to Purchasing so the user can mark the order as ordered
                    showInfo("Purchase order created as DRAFT. Open Supplier -> Purchasing, mark it ORDERED, then record delivery.");
                });
            }
            void refresh() {
                model.setRowCount(0);
                for (RestockRequest request : customerData.getRestockRequests()) {
                    model.addRow(new Object[]{request.getId(), request.getCreatedAt(), request.getCustomerName(),
                            request.getProductName(), request.getQuantityNeeded(), request.getStatus(), request.getOrderId()});
                }
            }
        }
