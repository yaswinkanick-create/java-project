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

abstract class AppPanel extends JPanel {
    protected final MainFrame app;
    protected final InventoryService service;
    protected final CustomerDataStore customerData;
    protected final RoleAuthStore roleAuth;
    protected final JLabel statusLabel;

    AppPanel(MainFrame app) {
        this.app = app;
        this.service = app.service;
        this.customerData = app.customerData;
        this.roleAuth = app.roleAuth;
        this.statusLabel = app.statusLabel;
    }

    protected void perform(String activity, IoAction action) { app.perform(activity, action); }
    protected void showError(String message) { app.showError(message); }
    protected void showInfo(String message) { app.showInfo(message); }
    protected JButton primary(String text, ActionListener listener) { return app.primary(text, listener); }
    protected JButton secondary(String text, ActionListener listener) { return app.secondary(text, listener); }
    protected JPanel panelWithToolbar(JPanel toolbar, JComponent content) { return app.panelWithToolbar(toolbar, content); }
    protected JPanel toolbar() { return app.toolbar(); }
    protected JTable styledTable(DefaultTableModel model) { return app.styledTable(model); }
    protected JComponent scroll(JTable table) { return app.scroll(table); }
    protected DefaultTableModel readOnlyModel(Object[] columns) { return app.readOnlyModel(columns); }
    protected String currency(double amount) { return app.currency(amount); }
    protected static double parseDouble(String value, String label) { return MainFrame.parseDouble(value, label); }
    protected static int parseInt(String value, String label) { return MainFrame.parseInt(value, label); }
    protected JPanel formPanel() { return app.formPanel(); }
    protected void addFormRow(JPanel panel, String label, JComponent field) { app.addFormRow(panel, label, field); }
    protected JPanel card(String label, JLabel value, Color marker) { return app.card(label, value, marker); }
    protected Product selectedProduct(JTable table, ProductTableModel model) { return app.selectedProduct(table, model); }
    protected Supplier selectedSupplier(JTable table, DefaultTableModel model) { return app.selectedSupplier(table, model); }
    protected void refreshAll() { app.refreshAll(); }
    protected void openProductDialog(Product product) { app.openProductDialog(product); }
    protected void openSupplierDialog(Supplier supplier) { app.openSupplierDialog(supplier); }
}
