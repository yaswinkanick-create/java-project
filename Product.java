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

final class Product {
        private final String id;
        private String name;
        private String category;
        private double unitCost;
        private double sellingPrice;
        private int quantity;
        private int reorderLevel;

        public Product(String id, String name, String category, double unitCost,
                       double sellingPrice, int quantity, int reorderLevel) {
            this.id = requireText(id, "Product ID").trim();
            updateDetails(name, category, unitCost, sellingPrice, reorderLevel);
            setQuantity(quantity);
        }

        public void updateDetails(String name, String category, double unitCost,
                                  double sellingPrice, int reorderLevel) {
            this.name = requireText(name, "Product name").trim();
            this.category = requireText(category, "Category").trim();
            requireNonNegativeMoney(unitCost, "Unit cost");
            requireNonNegativeMoney(sellingPrice, "Selling price");
            if (reorderLevel < 0) {
                throw new IllegalArgumentException("Reorder level cannot be negative.");
            }
            this.unitCost = money(unitCost);
            this.sellingPrice = money(sellingPrice);
            this.reorderLevel = reorderLevel;
        }

        public void setQuantity(int quantity) {
            if (quantity < 0) {
                throw new IllegalArgumentException("Quantity cannot be negative.");
            }
            this.quantity = quantity;
        }

        public void changeQuantity(int delta) {
            long next = (long) quantity + delta;
            if (next < 0 || next > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("The resulting quantity is invalid.");
            }
            quantity = (int) next;
        }

        public boolean isLowStock() {
            return quantity <= reorderLevel;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public double getUnitCost() { return unitCost; }
        public double getSellingPrice() { return sellingPrice; }
        public int getQuantity() { return quantity; }
        public int getReorderLevel() { return reorderLevel; }

        private static String requireText(String value, String label) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(label + " is required.");
            }
            return value;
        }

        private static void requireNonNegativeMoney(double amount, String label) {
            if (!Double.isFinite(amount) || amount < 0) {
                throw new IllegalArgumentException(label + " must be a valid non-negative number.");
            }
        }

        private static double money(double amount) {
            return Math.round(amount * 100.0) / 100.0;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Product product && id.equalsIgnoreCase(product.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id.toLowerCase());
        }
    }
