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

final class PurchaseOrder {
        private final String id;
        private final String createdAt;
        private final String supplierId;
        private final String supplierName;
        private final String productId;
        private final String productName;
        private final int orderedQuantity;
        private final double unitCost;
        private final String note;
        private int receivedQuantity;
        private String status;

        public PurchaseOrder(String id, String createdAt, String supplierId, String supplierName,
                             String productId, String productName, int orderedQuantity,
                             int receivedQuantity, double unitCost, String status, String note) {
            this.id = required(id, "Purchase order ID");
            this.createdAt = required(createdAt, "Purchase order date");
            this.supplierId = required(supplierId, "Supplier ID");
            this.supplierName = required(supplierName, "Supplier name");
            this.productId = required(productId, "Product ID");
            this.productName = required(productName, "Product name");
            if (orderedQuantity <= 0) throw new IllegalArgumentException("Ordered quantity must be greater than zero.");
            if (receivedQuantity < 0 || receivedQuantity > orderedQuantity) {
                throw new IllegalArgumentException("Received quantity must be between zero and ordered quantity.");
            }
            if (!Double.isFinite(unitCost) || unitCost < 0) throw new IllegalArgumentException("Unit cost must be non-negative and finite.");
            this.orderedQuantity = orderedQuantity;
            this.receivedQuantity = receivedQuantity;
            this.unitCost = money(unitCost);
            this.status = required(status, "Purchase order status").toUpperCase(java.util.Locale.ROOT);
            this.note = note == null ? "" : note.trim();
            validateStatus();
        }

        public void markOrdered() {
            if (!status.equals("DRAFT")) throw new IllegalArgumentException("Only draft purchase orders can be marked as ordered.");
            status = "ORDERED";
        }

        public void cancel() {
            if (status.equals("RECEIVED") || status.equals("CANCELLED")) {
                throw new IllegalArgumentException("A completed or already cancelled purchase order cannot be cancelled.");
            }
            status = "CANCELLED";
        }

        public void receive(int quantity) {
            if (!status.equals("ORDERED") && !status.equals("PARTIALLY_RECEIVED")) {
                throw new IllegalArgumentException("Only ordered purchase orders can receive a delivery.");
            }
            if (quantity <= 0) throw new IllegalArgumentException("Delivery quantity must be greater than zero.");
            if (quantity > getRemainingQuantity()) {
                throw new IllegalArgumentException("The delivery exceeds the outstanding quantity of " + getRemainingQuantity() + ".");
            }
            receivedQuantity += quantity;
            status = receivedQuantity == orderedQuantity ? "RECEIVED" : "PARTIALLY_RECEIVED";
        }

        public int getRemainingQuantity() { return status.equals("CANCELLED") ? 0 : orderedQuantity - receivedQuantity; }
        public String getId() { return id; }
        public String getCreatedAt() { return createdAt; }
        public String getSupplierId() { return supplierId; }
        public String getSupplierName() { return supplierName; }
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getOrderedQuantity() { return orderedQuantity; }
        public int getReceivedQuantity() { return receivedQuantity; }
        public double getUnitCost() { return unitCost; }
        public String getStatus() { return status; }
        public String getNote() { return note; }

        public PurchaseOrder copy() {
            return new PurchaseOrder(id, createdAt, supplierId, supplierName, productId, productName,
                    orderedQuantity, receivedQuantity, unitCost, status, note);
        }

        private void validateStatus() {
            boolean valid = switch (status) {
                case "DRAFT" -> receivedQuantity == 0;
                case "ORDERED" -> receivedQuantity == 0;
                case "PARTIALLY_RECEIVED" -> receivedQuantity > 0 && receivedQuantity < orderedQuantity;
                case "RECEIVED" -> receivedQuantity == orderedQuantity;
                case "CANCELLED" -> receivedQuantity < orderedQuantity;
                default -> false;
            };
            if (!valid) throw new IllegalArgumentException("Purchase order status does not match received quantity: " + status);
        }

        private static String required(String value, String label) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
            return value.trim();
        }

        private static double money(double value) { return Math.round(value * 100.0) / 100.0; }
    }
