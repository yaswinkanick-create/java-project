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

final class SuppliersPanel extends AppPanel {
            private final DefaultTableModel model = readOnlyModel(new Object[]{"Supplier ID", "Supplier name", "Phone", "Email"});
            private final JTable table = styledTable(model);

            SuppliersPanel(MainFrame app) {
            super(app);
                JPanel actions = toolbar();
                actions.add(primary("+ Add supplier", e -> openSupplierDialog(null)));
                actions.add(secondary("Edit", e -> {
                    Supplier supplier = selectedSupplier(table, model);
                    if (supplier == null) showInfo("Select a supplier row first."); else openSupplierDialog(supplier);
                }));
                actions.add(secondary("Delete", e -> {
                    Supplier supplier = selectedSupplier(table, model);
                    if (supplier == null) { showInfo("Select a supplier row first."); return; }
                    int result = JOptionPane.showConfirmDialog(app,
                            "Delete supplier " + supplier.getName() + "? Suppliers referenced by purchases cannot be deleted.",
                            "Confirm deletion", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (result == JOptionPane.OK_OPTION) perform("Supplier deletion", () -> service.deleteSupplier(supplier.getId()));
                }));
                setLayout(new BorderLayout(0, 12));
                setBackground(UiTheme.CANVAS);
                setBorder(BorderFactory.createEmptyBorder(16, 4, 4, 4));
                add(actions, BorderLayout.NORTH);
                add(scroll(table), BorderLayout.CENTER);
            }
            void refresh() {
                model.setRowCount(0);
                for (Supplier supplier : service.getSuppliers()) {
                    model.addRow(new Object[]{supplier.getId(), supplier.getName(), supplier.getPhone(), supplier.getEmail()});
                }
            }
        }
