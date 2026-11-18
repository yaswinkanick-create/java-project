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

final class ProductTableModel extends AbstractTableModel {
            private final String[] columns = {"Product ID", "Name", "Category", "Unit cost", "Selling price", "On hand", "Reorder at", "Status"};
            private List<Product> data = List.of();
            void setProducts(List<Product> products) { data = products; fireTableDataChanged(); }
            Product getProduct(int row) { return data.get(row); }
            @Override public int getRowCount() { return data.size(); }
            @Override public int getColumnCount() { return columns.length; }
            @Override public String getColumnName(int column) { return columns[column]; }
            @Override public Class<?> getColumnClass(int column) {
                return (column == 5 || column == 6) ? Integer.class : String.class;
            }
            private String formatCurrency(double amount) { return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).format(amount); }
    @Override public Object getValueAt(int row, int column) {
                Product p = data.get(row);
                return switch (column) {
                    case 0 -> p.getId(); case 1 -> p.getName(); case 2 -> p.getCategory();
                    case 3 -> formatCurrency(p.getUnitCost()); case 4 -> formatCurrency(p.getSellingPrice());
                    case 5 -> p.getQuantity(); case 6 -> p.getReorderLevel();
                    case 7 -> p.isLowStock() ? "Low stock" : "In stock";
                    default -> "";
                };
            }
        }
