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

enum UserRole {
        CUSTOMER("CUSTOMER", "Customer", "Browse the catalog, place orders, and view order status."),
        INVENTORY_SELLER("INVENTORY_SELLER", "Inventory / Seller", "Manage products, sales, stock control, and reports."),
        SUPPLIER("SUPPLIER", "Supplier", "Manage supplier records, purchasing, deliveries, and restock requests.");

        final String key;
        final String title;
        final String description;

        UserRole(String key, String title, String description) {
            this.key = key;
            this.title = title;
            this.description = description;
        }
    }
