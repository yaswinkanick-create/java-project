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

final class InventoryDialogs {
    private InventoryDialogs() { }

static void openProductDialog(MainFrame app, Product existing) {
            boolean editing = existing != null;
            JDialog dialog = new JDialog(app, editing ? "Edit product" : "Add product", true);
            dialog.setLayout(new BorderLayout(0, 8));
            dialog.getContentPane().setBackground(Color.WHITE);
            JPanel form = app.formPanel();
            JTextField idField = new JTextField(editing ? existing.getId() : app.nextProductId());
            idField.setEnabled(!editing);
            JTextField nameField = new JTextField(editing ? existing.getName() : "");
            JTextField categoryField = new JTextField(editing ? existing.getCategory() : "General");
            JTextField costField = new JTextField(editing ? Double.toString(existing.getUnitCost()) : "0.00");
            JTextField priceField = new JTextField(editing ? Double.toString(existing.getSellingPrice()) : "0.00");
            JTextField quantityField = new JTextField(editing ? Integer.toString(existing.getQuantity()) : "0");
            quantityField.setEnabled(!editing);
            JTextField reorderField = new JTextField(editing ? Integer.toString(existing.getReorderLevel()) : "5");
            app.addFormRow(form, "Product ID / SKU", idField);
            app.addFormRow(form, "Product name", nameField);
            app.addFormRow(form, "Category", categoryField);
            app.addFormRow(form, "Unit cost (\u20B9)", costField);
            app.addFormRow(form, "Selling price (\u20B9)", priceField);
            app.addFormRow(form, editing ? "Current quantity (Stock Control)" : "Opening quantity", quantityField);
            app.addFormRow(form, "Reorder level", reorderField);
            JLabel hint = new JLabel("  Product ID stays fixed after creation. Use Stock Control to change quantities.");
            hint.setForeground(UiTheme.MUTED);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
            actions.setBackground(Color.WHITE);
            JButton cancel = app.secondary("Cancel", e -> dialog.dispose());
            JButton save = app.primary(editing ? "Save changes" : "Add product", e -> {
                try {
                    String id = idField.getText().trim();
                    String name = nameField.getText().trim();
                    String category = categoryField.getText().trim();
                    double cost = MainFrame.parseDouble(costField.getText(), "Unit cost");
                    double price = MainFrame.parseDouble(priceField.getText(), "Selling price");
                    int reorder = MainFrame.parseInt(reorderField.getText(), "Reorder level");
                    if (editing) {
                        app.service.updateProduct(id, name, category, cost, price, reorder);
                    } else {
                        int opening = MainFrame.parseInt(quantityField.getText(), "Opening quantity");
                        app.service.addProduct(id, name, category, cost, price, opening, reorder);
                    }
                    dialog.dispose();
                    app.statusLabel.setText(editing ? "Product updated | Saved" : "Product created | Saved");
                    app.refreshAll();
                } catch (IllegalArgumentException exception) {
                    app.showError(exception.getMessage());
                } catch (IOException exception) {
                    app.showError("Could not save product. Review the CSV files before continuing.\n" + exception.getMessage());
                }
            });
            actions.add(cancel);
            actions.add(save);
            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setBackground(Color.WHITE);
            bottom.add(hint, BorderLayout.CENTER);
            bottom.add(actions, BorderLayout.SOUTH);
            dialog.add(form, BorderLayout.CENTER);
            dialog.add(bottom, BorderLayout.SOUTH);
            dialog.getRootPane().setDefaultButton(save);
            dialog.pack();
            dialog.setMinimumSize(new Dimension(570, dialog.getHeight()));
            dialog.setLocationRelativeTo(app);
            dialog.setVisible(true);
        }

static void openSupplierDialog(MainFrame app, Supplier existing) {
            boolean editing = existing != null;
            JDialog dialog = new JDialog(app, editing ? "Edit supplier" : "Add supplier", true);
            dialog.setLayout(new BorderLayout());
            dialog.getContentPane().setBackground(Color.WHITE);
            JPanel form = app.formPanel();
            JTextField id = new JTextField(editing ? existing.getId() : app.nextSupplierId());
            id.setEnabled(!editing);
            JTextField name = new JTextField(editing ? existing.getName() : "");
            JTextField phone = new JTextField(editing ? existing.getPhone() : "");
            JTextField email = new JTextField(editing ? existing.getEmail() : "");
            app.addFormRow(form, "Supplier ID", id);
            app.addFormRow(form, "Supplier name", name);
            app.addFormRow(form, "Phone", phone);
            app.addFormRow(form, "Email", email);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
            actions.setBackground(Color.WHITE);
            actions.add(app.secondary("Cancel", e -> dialog.dispose()));
            JButton save = app.primary(editing ? "Save changes" : "Add supplier", e -> {
                try {
                    if (editing) app.service.updateSupplier(id.getText(), name.getText(), phone.getText(), email.getText());
                    else app.service.addSupplier(id.getText(), name.getText(), phone.getText(), email.getText());
                    dialog.dispose();
                    app.statusLabel.setText("Supplier record saved");
                    app.refreshAll();
                } catch (IllegalArgumentException exception) {
                    app.showError(exception.getMessage());
                } catch (IOException exception) {
                    app.showError("Could not save supplier. Review the CSV files.\n" + exception.getMessage());
                }
            });
            actions.add(save);
            dialog.add(form, BorderLayout.CENTER);
            dialog.add(actions, BorderLayout.SOUTH);
            dialog.getRootPane().setDefaultButton(save);
            dialog.pack();
            dialog.setMinimumSize(new Dimension(510, dialog.getHeight()));
            dialog.setLocationRelativeTo(app);
            dialog.setVisible(true);
        }
}
