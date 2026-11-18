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

final class TransactionRecord {
        private final String id;
        private final String dateTime;
        private final String type;
        private final String referenceId;
        private final String productId;
        private final String productName;
        private final int quantity;
        private final double unitPrice;
        private final double unitCost;
        private final double total;
        private final String partyName;
        private final String paymentMethod;
        private final String relatedId;
        private final String note;

        public TransactionRecord(String id, String dateTime, String type, String referenceId,
                                 String productId, String productName, int quantity,
                                 double unitPrice, double unitCost, double total,
                                 String partyName, String paymentMethod, String relatedId, String note) {
            this.id = required(id, "Transaction row ID");
            this.dateTime = required(dateTime, "Transaction date");
            this.type = required(type, "Transaction type").toUpperCase(java.util.Locale.ROOT);
            if (!java.util.Set.of("PURCHASE", "SALE", "RETURN", "ADJUSTMENT").contains(this.type)) {
                throw new IllegalArgumentException("Unknown transaction type: " + this.type);
            }
            this.referenceId = required(referenceId, "Transaction reference");
            this.productId = required(productId, "Product ID");
            this.productName = required(productName, "Product name");
            if ((this.type.equals("PURCHASE") || this.type.equals("SALE") || this.type.equals("RETURN")) && quantity <= 0) {
                throw new IllegalArgumentException("Transaction quantity must be greater than zero.");
            }
            if (this.type.equals("ADJUSTMENT") && quantity == 0) {
                throw new IllegalArgumentException("An adjustment must change the quantity.");
            }
            validateMoney(unitPrice, "Unit price");
            validateMoney(unitCost, "Unit cost");
            validateMoney(total, "Transaction total");
            this.quantity = quantity;
            this.unitPrice = money(unitPrice);
            this.unitCost = money(unitCost);
            this.total = money(total);
            this.partyName = text(partyName);
            this.paymentMethod = text(paymentMethod);
            this.relatedId = text(relatedId);
            this.note = text(note);
        }

        private static String text(String value) { return value == null ? "" : value; }
        private static String required(String value, String label) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
            return value.trim();
        }
        private static void validateMoney(double value, String label) {
            if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException(label + " must be non-negative and finite.");
        }
        private static double money(double value) { return Math.round(value * 100.0) / 100.0; }

        public String getId() { return id; }
        public String getDateTime() { return dateTime; }
        public String getType() { return type; }
        public String getReferenceId() { return referenceId; }
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public double getUnitPrice() { return unitPrice; }
        public double getUnitCost() { return unitCost; }
        public double getTotal() { return total; }
        public String getPartyName() { return partyName; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getRelatedId() { return relatedId; }
        public String getNote() { return note; }
    }
