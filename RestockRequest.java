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

final class RestockRequest {
        final String id;
        final String createdAt;
        final String orderId;
        final String customerName;
        final String productId;
        final String productName;
        int quantityNeeded;
        String status;

        RestockRequest(String id, String createdAt, String orderId, String customerName,
                               String productId, String productName, int quantityNeeded, String status) {
            this.id = id; this.createdAt = createdAt; this.orderId = orderId; this.customerName = customerName;
            this.productId = productId; this.productName = productName; this.quantityNeeded = quantityNeeded; this.status = status;
        }
        public String getId() { return id; }
        public String getCreatedAt() { return createdAt; }
        public String getOrderId() { return orderId; }
        public String getCustomerName() { return customerName; }
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantityNeeded() { return quantityNeeded; }
        public String getStatus() { return status; }
    }
