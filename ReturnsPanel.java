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

final class ReturnsPanel extends AppPanel {
            private final JComboBox<InventoryService.ReturnOption> itemBox = new JComboBox<>();
            private final JTextField quantity = new JTextField("1", 7);
            private final JTextField reason = new JTextField(22);
            private final JCheckBox restock = new JCheckBox("Return units to sellable stock", true);
            private final DefaultTableModel model = readOnlyModel(new Object[]{"Date", "Return ID", "Original sale", "Product", "Qty", "Refund", "Disposition", "Reason"});

            ReturnsPanel(MainFrame app) {
            super(app);
                JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
                form.setBackground(Color.WHITE);
                form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.BORDER),
                        BorderFactory.createEmptyBorder(7, 8, 7, 8)));
                form.add(new JLabel("Sale item")); form.add(itemBox);
                form.add(new JLabel("Qty")); form.add(quantity);
                form.add(restock);
                form.add(new JLabel("Reason")); form.add(reason);
                form.add(primary("Process return", e -> processReturn()));
                JPanel top = new JPanel(new BorderLayout(0, 10));
                top.setOpaque(false);
                JLabel description = new JLabel("Returns are linked to an original sale and cannot exceed the remaining sold quantity.");
                description.setForeground(UiTheme.MUTED);
                top.add(description, BorderLayout.NORTH);
                top.add(form, BorderLayout.CENTER);
                setLayout(new BorderLayout(0, 12));
                setBackground(UiTheme.CANVAS);
                setBorder(BorderFactory.createEmptyBorder(16, 4, 4, 4));
                add(top, BorderLayout.NORTH);
                add(scroll(styledTable(model)), BorderLayout.CENTER);
            }

            private void processReturn() {
                InventoryService.ReturnOption option = (InventoryService.ReturnOption) itemBox.getSelectedItem();
                if (option == null) { showInfo("There are no sale items currently eligible for return."); return; }
                int qty;
                try { qty = parseInt(quantity.getText(), "Return quantity"); }
                catch (IllegalArgumentException exception) { showError(exception.getMessage()); return; }
                perform("Customer return", () -> {
                    String id = service.processReturn(option.saleId(), option.productId(), qty, restock.isSelected(), reason.getText());
                    quantity.setText("1"); reason.setText("");
                    showInfo("Return recorded.\nReturn reference: " + id);
                });
            }
            void refresh() {
                String previous = itemBox.getSelectedItem() instanceof InventoryService.ReturnOption o ? o.saleId() + "|" + o.productId() : null;
                itemBox.removeAllItems();
                for (InventoryService.ReturnOption option : service.getReturnOptions()) itemBox.addItem(option);
                if (previous != null) {
                    for (int i = 0; i < itemBox.getItemCount(); i++) {
                        InventoryService.ReturnOption option = itemBox.getItemAt(i);
                        if ((option.saleId() + "|" + option.productId()).equals(previous)) { itemBox.setSelectedIndex(i); break; }
                    }
                }
                model.setRowCount(0);
                for (TransactionRecord t : service.getTransactionsByType("RETURN")) {
                    model.addRow(new Object[]{t.getDateTime(), t.getReferenceId(), t.getRelatedId(), t.getProductName(),
                            t.getQuantity(), currency(t.getTotal()), t.getNote().startsWith("RESTOCKED:") ? "Restocked" : "Not restocked",
                            t.getNote().replaceFirst("^(RESTOCKED|NOT_RESTOCKED):\\s*", "")});
                }
            }
        }
