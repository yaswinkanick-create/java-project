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

final class CustomerOrder {
        final String id;
        final String createdAt;
        final String customerId;
        final String customerName;
        final String productId;
        final String productName;
        final int quantity;
        final double requestedUnitPrice;
        String status;
        String restockRequestId;
        String saleReference;

        CustomerOrder(String id, String createdAt, String customerId, String customerName,
                              String productId, String productName, int quantity, double requestedUnitPrice,
                              String status, String restockRequestId, String saleReference) {
            this.id = id; this.createdAt = createdAt; this.customerId = customerId; this.customerName = customerName;
            this.productId = productId; this.productName = productName; this.quantity = quantity;
            this.requestedUnitPrice = requestedUnitPrice; this.status = status;
            this.restockRequestId = restockRequestId == null ? "" : restockRequestId;
            this.saleReference = saleReference == null ? "" : saleReference;
        }
        public String getId() { return id; }
        public String getCreatedAt() { return createdAt; }
        public String getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public double getRequestedUnitPrice() { return requestedUnitPrice; }
        public String getStatus() { return status; }
        public String getRestockRequestId() { return restockRequestId; }
        public String getSaleReference() { return saleReference; }
    }
