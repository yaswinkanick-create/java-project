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

final class CsvDatabase {
        private static final String[] PRODUCT_HEADER = {
                "product_id", "name", "category", "unit_cost", "selling_price", "quantity", "reorder_level"
        };
        private static final String[] SUPPLIER_HEADER = {"supplier_id", "name", "phone", "email"};
        private static final String[] TRANSACTION_HEADER = {
                "row_id", "date_time", "type", "reference_id", "product_id", "product_name",
                "quantity", "unit_price", "unit_cost", "total", "party_name", "payment_method",
                "related_id", "note"
        };
        private static final String[] PURCHASE_ORDER_HEADER = {
                "order_id", "created_at", "supplier_id", "supplier_name", "product_id", "product_name",
                "ordered_quantity", "received_quantity", "unit_cost", "status", "note"
        };

        private final Path dataDirectory;
        private final Path productsFile;
        private final Path suppliersFile;
        private final Path transactionsFile;
        private final Path purchaseOrdersFile;

        public CsvDatabase(Path dataDirectory) throws IOException {
            this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
            Files.createDirectories(this.dataDirectory);
            productsFile = this.dataDirectory.resolve("products.csv");
            suppliersFile = this.dataDirectory.resolve("suppliers.csv");
            transactionsFile = this.dataDirectory.resolve("transactions.csv");
            purchaseOrdersFile = this.dataDirectory.resolve("purchase_orders.csv");
            ensureSeedFile(productsFile, PRODUCT_HEADER,
                    "P1001,Notebook,Stationery,28.00,45.00,120,20\n" +
                    "P1002,Pen,Stationery,5.00,10.00,100,15\n" +
                    "P1003,Marker,Stationery,14.00,25.00,35,10\n" +
                    "P1004,File Folder,Office Supplies,18.00,30.00,8,10\n");
            ensureSeedFile(suppliersFile, SUPPLIER_HEADER,
                    "SUP101,City Stationery Traders,9876543210,sales@example.com\n" +
                    "SUP102,Office Essentials,9123456780,orders@example.org\n");
            ensureSeedFile(transactionsFile, TRANSACTION_HEADER,
                    "INIT-1001,2026-10-18 09:00:00,ADJUSTMENT,OPEN-1001,P1001,Notebook,120,45.00,28.00,0.00,,,,Sample opening stock\n" +
                    "INIT-1002,2026-10-18 09:00:00,ADJUSTMENT,OPEN-1002,P1002,Pen,100,10.00,5.00,0.00,,,,Sample opening stock\n" +
                    "INIT-1003,2026-10-18 09:00:00,ADJUSTMENT,OPEN-1003,P1003,Marker,35,25.00,14.00,0.00,,,,Sample opening stock\n" +
                    "INIT-1004,2026-10-18 09:00:00,ADJUSTMENT,OPEN-1004,P1004,File Folder,8,30.00,18.00,0.00,,,,Sample opening stock\n");
            ensureSeedFile(purchaseOrdersFile, PURCHASE_ORDER_HEADER, "");
        }

        public Path getDataDirectory() { return dataDirectory; }

        public List<Product> loadProducts() throws IOException {
            List<Product> products = new ArrayList<>();
            List<List<String>> rows = readRows(productsFile, PRODUCT_HEADER);
            int rowNumber = 1;
            for (List<String> row : rows) {
                rowNumber++;
                try {
                    products.add(new Product(row.get(0), row.get(1), row.get(2),
                            Double.parseDouble(row.get(3)), Double.parseDouble(row.get(4)),
                            Integer.parseInt(row.get(5)), Integer.parseInt(row.get(6))));
                } catch (RuntimeException exception) {
                    throw rowError(productsFile, rowNumber, exception);
                }
            }
            return products;
        }

        public List<Supplier> loadSuppliers() throws IOException {
            List<Supplier> suppliers = new ArrayList<>();
            List<List<String>> rows = readRows(suppliersFile, SUPPLIER_HEADER);
            int rowNumber = 1;
            for (List<String> row : rows) {
                rowNumber++;
                try {
                    suppliers.add(new Supplier(row.get(0), row.get(1), row.get(2), row.get(3)));
                } catch (RuntimeException exception) {
                    throw rowError(suppliersFile, rowNumber, exception);
                }
            }
            return suppliers;
        }

        public List<TransactionRecord> loadTransactions() throws IOException {
            List<TransactionRecord> records = new ArrayList<>();
            List<List<String>> rows = readRows(transactionsFile, TRANSACTION_HEADER);
            int rowNumber = 1;
            for (List<String> row : rows) {
                rowNumber++;
                try {
                    records.add(new TransactionRecord(row.get(0), row.get(1), row.get(2), row.get(3),
                            row.get(4), row.get(5), Integer.parseInt(row.get(6)),
                            Double.parseDouble(row.get(7)), Double.parseDouble(row.get(8)),
                            Double.parseDouble(row.get(9)), row.get(10), row.get(11), row.get(12), row.get(13)));
                } catch (RuntimeException exception) {
                    throw rowError(transactionsFile, rowNumber, exception);
                }
            }
            return records;
        }

        public void saveProducts(List<Product> products) throws IOException {
            List<String[]> rows = new ArrayList<>();
            for (Product p : products) {
                rows.add(new String[]{p.getId(), p.getName(), p.getCategory(), money(p.getUnitCost()),
                        money(p.getSellingPrice()), Integer.toString(p.getQuantity()), Integer.toString(p.getReorderLevel())});
            }
            writeRows(productsFile, PRODUCT_HEADER, rows);
        }

        public void saveSuppliers(List<Supplier> suppliers) throws IOException {
            List<String[]> rows = new ArrayList<>();
            for (Supplier s : suppliers) {
                rows.add(new String[]{s.getId(), s.getName(), s.getPhone(), s.getEmail()});
            }
            writeRows(suppliersFile, SUPPLIER_HEADER, rows);
        }


        public List<PurchaseOrder> loadPurchaseOrders() throws IOException {
            List<PurchaseOrder> orders = new ArrayList<>();
            List<List<String>> rows = readRows(purchaseOrdersFile, PURCHASE_ORDER_HEADER);
            int rowNumber = 1;
            for (List<String> row : rows) {
                rowNumber++;
                try {
                    orders.add(new PurchaseOrder(row.get(0), row.get(1), row.get(2), row.get(3),
                            row.get(4), row.get(5), Integer.parseInt(row.get(6)), Integer.parseInt(row.get(7)),
                            Double.parseDouble(row.get(8)), row.get(9), row.get(10)));
                } catch (RuntimeException exception) {
                    throw rowError(purchaseOrdersFile, rowNumber, exception);
                }
            }
            return orders;
        }

        public void savePurchaseOrders(List<PurchaseOrder> orders) throws IOException {
            List<String[]> rows = new ArrayList<>();
            for (PurchaseOrder order : orders) {
                rows.add(new String[]{order.getId(), order.getCreatedAt(), order.getSupplierId(), order.getSupplierName(),
                        order.getProductId(), order.getProductName(), Integer.toString(order.getOrderedQuantity()),
                        Integer.toString(order.getReceivedQuantity()), money(order.getUnitCost()), order.getStatus(), order.getNote()});
            }
            writeRows(purchaseOrdersFile, PURCHASE_ORDER_HEADER, rows);
        }

        public void saveTransactions(List<TransactionRecord> records) throws IOException {
            List<String[]> rows = new ArrayList<>();
            for (TransactionRecord t : records) {
                rows.add(new String[]{t.getId(), t.getDateTime(), t.getType(), t.getReferenceId(),
                        t.getProductId(), t.getProductName(), Integer.toString(t.getQuantity()),
                        money(t.getUnitPrice()), money(t.getUnitCost()), money(t.getTotal()),
                        t.getPartyName(), t.getPaymentMethod(), t.getRelatedId(), t.getNote()});
            }
            writeRows(transactionsFile, TRANSACTION_HEADER, rows);
        }

        /** Read a product CSV for import. It must use the same columns as products.csv. */
        public List<Product> readProductsFrom(Path path) throws IOException {
            List<List<String>> rows = readRows(path, PRODUCT_HEADER);
            List<Product> products = new ArrayList<>();
            int rowNumber = 1;
            for (List<String> row : rows) {
                rowNumber++;
                try {
                    products.add(new Product(row.get(0), row.get(1), row.get(2),
                            Double.parseDouble(row.get(3)), Double.parseDouble(row.get(4)),
                            Integer.parseInt(row.get(5)), Integer.parseInt(row.get(6))));
                } catch (RuntimeException exception) {
                    throw rowError(path, rowNumber, exception);
                }
            }
            return products;
        }

        public void exportProductsTo(Path path, List<Product> products) throws IOException {
            List<String[]> rows = new ArrayList<>();
            for (Product p : products) {
                rows.add(new String[]{p.getId(), p.getName(), p.getCategory(), money(p.getUnitCost()),
                        money(p.getSellingPrice()), Integer.toString(p.getQuantity()), Integer.toString(p.getReorderLevel())});
            }
            writeRows(path.toAbsolutePath().normalize(), PRODUCT_HEADER, rows);
        }

        private void ensureSeedFile(Path file, String[] header, String seedRows) throws IOException {
            if (Files.exists(file) && Files.size(file) > 0) return;
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            StringBuilder content = new StringBuilder(CsvUtil.formatRow(header)).append(System.lineSeparator());
            if (seedRows != null && !seedRows.isEmpty()) {
                content.append(seedRows.replace("\n", System.lineSeparator()));
            }
            Files.writeString(file, content.toString(), StandardCharsets.UTF_8);
        }

        private List<List<String>> readRows(Path file, String[] expectedHeader) throws IOException {
            int expectedColumns = expectedHeader.length;
            List<List<String>> rows = new ArrayList<>();
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String firstLine = reader.readLine();
                if (firstLine == null) return rows;
                List<String> actualHeader;
                try {
                    actualHeader = CsvUtil.parseLine(firstLine);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Invalid CSV header in " + file + ".", e);
                }
                List<String> expected = java.util.Arrays.stream(expectedHeader).map(String::trim).toList();
                List<String> normalizedActual = actualHeader.stream().map(String::trim).toList();
                if (!normalizedActual.equals(expected)) {
                    throw new IOException("Unexpected CSV header in " + file + ". Expected: " + String.join(",", expected));
                }
                String line;
                int rowNumber = 1;
                while ((line = reader.readLine()) != null) {
                    rowNumber++;
                    if (line.isBlank()) continue;
                    try {
                        List<String> row = CsvUtil.parseLine(line);
                        if (row.size() != expectedColumns) {
                            throw new IllegalArgumentException("Expected " + expectedColumns + " columns but found " + row.size());
                        }
                        rows.add(row);
                    } catch (IllegalArgumentException exception) {
                        throw new IOException("Invalid CSV data at " + file + ", row " + rowNumber + ": " + exception.getMessage(), exception);
                    }
                }
            }
            return rows;
        }

        private void writeRows(Path target, String[] header, List<String[]> rows) throws IOException {
            Path parent = target.getParent();
            if (parent != null) Files.createDirectories(parent);
            Path temporary = Files.createTempFile(parent == null ? Path.of(".") : parent, "inventory-", ".tmp");
            boolean moved = false;
            boolean contentReady = false;
            try {
                try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                    writer.write(CsvUtil.formatRow(header));
                    writer.newLine();
                    for (String[] row : rows) {
                        writer.write(CsvUtil.formatRow(row));
                        writer.newLine();
                    }
                }
                contentReady = true;
                FileReplacer.replace(temporary, target);
                moved = true;
            } finally {
                if (!contentReady) {
                    Files.deleteIfExists(temporary);
                } else if (moved) {
                    // The CSV save already succeeded; a temp-file cleanup issue must not report a false save failure.
                    try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* safe to clean up later */ }
                }
            }
        }

        private IOException rowError(Path file, int rowNumber, RuntimeException cause) {
            return new IOException("Invalid data in " + file + ", row " + rowNumber + ": " + cause.getMessage(), cause);
        }

        private static String money(double amount) {
            return String.format(java.util.Locale.ROOT, "%.2f", amount);
        }
    }
