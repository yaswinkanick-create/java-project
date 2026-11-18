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

final class CustomerDataStore {
        private static final String[] CUSTOMER_HEADER = {"customer_id", "name", "email", "salt_hex", "password_hash_hex"};
        private static final String[] CUSTOMER_ORDER_HEADER = {"order_id", "created_at", "customer_id", "customer_name", "product_id", "product_name", "quantity", "requested_unit_price", "status", "restock_request_id", "sale_reference"};
        private static final String[] RESTOCK_HEADER = {"request_id", "created_at", "order_id", "customer_name", "product_id", "product_name", "quantity_needed", "status"};
        private static final int HASH_ITERATIONS = 120_000;
        private final Path dataDirectory;
        private final Path customersFile;
        private final Path ordersFile;
        private final Path restockFile;
        private final ArrayList<CustomerAccount> accounts = new ArrayList<>();
        private final ArrayList<CustomerOrder> orders = new ArrayList<>();
        private final ArrayList<RestockRequest> restockRequests = new ArrayList<>();

        public CustomerDataStore(Path dataDirectory) throws IOException {
            this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
            Files.createDirectories(this.dataDirectory);
            customersFile = this.dataDirectory.resolve("customers.csv");
            ordersFile = this.dataDirectory.resolve("customer_orders.csv");
            restockFile = this.dataDirectory.resolve("restock_requests.csv");
            ensureHeader(customersFile, CUSTOMER_HEADER);
            ensureHeader(ordersFile, CUSTOMER_ORDER_HEADER);
            ensureHeader(restockFile, RESTOCK_HEADER);
            loadAccounts();
            loadOrders();
            loadRestockRequests();
        }

        public synchronized CustomerAccount register(String name, String email, char[] password) throws IOException {
            String cleanName = required(name, "Customer name");
            String cleanEmail = required(email, "Email").toLowerCase(Locale.ROOT);
            if (!cleanEmail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new IllegalArgumentException("Enter a valid email address.");
            if (password == null || password.length < 8) throw new IllegalArgumentException("Password must contain at least 8 characters.");
            if (accounts.stream().anyMatch(a -> a.getEmail().equalsIgnoreCase(cleanEmail))) {
                throw new IllegalArgumentException("An account with this email already exists. Sign in instead.");
            }
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            String saltHex = HexFormat.of().formatHex(salt);
            String hashHex = hashPassword(password, salt);
            CustomerAccount account = new CustomerAccount(newId("CUS"), cleanName, cleanEmail, saltHex, hashHex);
            accounts.add(account);
            try { saveAccounts(); }
            catch (IOException exception) { accounts.remove(account); throw exception; }
            return account;
        }

        public synchronized CustomerAccount authenticate(String email, char[] password) {
            String cleanEmail = email == null ? "" : email.trim();
            CustomerAccount account = accounts.stream().filter(a -> a.getEmail().equalsIgnoreCase(cleanEmail)).findFirst().orElse(null);
            if (account == null || password == null) throw new IllegalArgumentException("Email or password is incorrect.");
            byte[] actual = HexFormat.of().parseHex(hashPassword(password, HexFormat.of().parseHex(account.saltHex())));
            byte[] expected = HexFormat.of().parseHex(account.passwordHashHex());
            if (!MessageDigest.isEqual(actual, expected)) throw new IllegalArgumentException("Email or password is incorrect.");
            return account;
        }

        public synchronized CustomerOrder submitOrder(CustomerAccount account, InventoryService inventory,
                                                      String productId, int quantity) throws IOException {
            if (account == null) throw new IllegalArgumentException("Register or sign in before placing an order.");
            if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero.");
            Product product = inventory.findProduct(productId);
            if (product == null) throw new IllegalArgumentException("The selected product no longer exists.");
            String orderId = newId("CO");
            CustomerOrder order = new CustomerOrder(orderId, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    account.getId(), account.getName(), product.getId(), product.getName(), quantity,
                    product.getSellingPrice(), "AWAITING_STOCK", "", "");
            orders.add(order);
            RestockRequest request = null;
            if (product.getQuantity() < quantity) {
                int needed = quantity - product.getQuantity();
                request = new RestockRequest(newId("RS"), order.getCreatedAt(), orderId, account.getName(),
                        product.getId(), product.getName(), needed, "OPEN");
                order.restockRequestId = request.getId();
                restockRequests.add(request);
            }
            try {
                saveOrders();
                if (request != null) saveRestockRequests();
            } catch (IOException exception) {
                orders.remove(order);
                if (request != null) restockRequests.remove(request);
                throw exception;
            }
            fulfillPendingOrders(inventory);
            return order;
        }

        public synchronized void fulfillPendingOrders(InventoryService inventory) throws IOException {
            boolean ordersChanged = false;
            boolean requestsChanged = false;
            for (CustomerOrder order : orders) {
                if (!order.status.equals("AWAITING_STOCK")) continue;
                Product product = inventory.findProduct(order.productId);
                if (product == null || product.getQuantity() < order.quantity) continue;
                String stableSaleReference = "SAL-" + order.id;
                if (order.saleReference.isBlank()) {
                    order.saleReference = inventory.createSaleForCustomerOrder(
                            List.of(new InventoryService.CartItem(order.productId, order.quantity)), order.customerName, order.id);
                }
                order.status = "FULFILLED";
                ordersChanged = true;
                if (!order.restockRequestId.isBlank()) {
                    RestockRequest request = findRestockRequest(order.restockRequestId);
                    if (request != null && !request.status.equals("RESOLVED")) {
                        request.status = "RESOLVED";
                        requestsChanged = true;
                    }
                }
            }
            if (ordersChanged) saveOrders();
            if (requestsChanged) saveRestockRequests();
            reopenRequestsWithoutActivePurchase(inventory);
        }

        private void reopenRequestsWithoutActivePurchase(InventoryService inventory) throws IOException {
            boolean changed = false;
            for (RestockRequest request : restockRequests) {
                if (!request.status.equals("ORDERED")) continue;
                CustomerOrder order = findOrder(request.orderId);
                if (order == null || !order.status.equals("AWAITING_STOCK")) continue;
                boolean activePo = inventory.getPurchaseOrders().stream().anyMatch(po -> po.getNote().contains(request.id)
                        && Set.of("DRAFT", "ORDERED", "PARTIALLY_RECEIVED").contains(po.getStatus()));
                if (!activePo) {
                    Product product = inventory.findProduct(request.productId);
                    request.quantityNeeded = Math.max(1, order.quantity - (product == null ? 0 : product.getQuantity()));
                    request.status = "OPEN";
                    changed = true;
                }
            }
            if (changed) saveRestockRequests();
        }

        public synchronized void markRestockOrdered(String requestId) throws IOException {
            RestockRequest request = requireRestockRequest(requestId);
            if (!request.status.equals("OPEN")) throw new IllegalArgumentException("Only open restock requests can be ordered.");
            request.status = "ORDERED";
            saveRestockRequests();
        }

        public synchronized List<CustomerOrder> getOrdersForCustomer(String customerId) {
            return orders.stream().filter(o -> o.customerId.equalsIgnoreCase(customerId))
                    .sorted(Comparator.comparing(CustomerOrder::getCreatedAt).reversed()).toList();
        }

        public synchronized List<RestockRequest> getRestockRequests() {
            return restockRequests.stream().sorted(Comparator.comparing(RestockRequest::getCreatedAt).reversed()).toList();
        }

        public synchronized RestockRequest findRestockRequest(String id) {
            if (id == null) return null;
            return restockRequests.stream().filter(r -> r.id.equalsIgnoreCase(id.trim())).findFirst().orElse(null);
        }

        public synchronized CustomerOrder findOrder(String id) {
            if (id == null) return null;
            return orders.stream().filter(o -> o.id.equalsIgnoreCase(id.trim())).findFirst().orElse(null);
        }

        private void loadAccounts() throws IOException {
            for (List<String> row : readRows(customersFile, CUSTOMER_HEADER)) {
                accounts.add(new CustomerAccount(row.get(0), row.get(1), row.get(2), row.get(3), row.get(4)));
            }
            Set<String> emails = new HashSet<>();
            for (CustomerAccount account : accounts) if (!emails.add(account.getEmail().toLowerCase(Locale.ROOT))) throw new IOException("Duplicate customer email in customers.csv: " + account.getEmail());
        }
        private void loadOrders() throws IOException {
            for (List<String> row : readRows(ordersFile, CUSTOMER_ORDER_HEADER)) {
                try {
                    int quantity = Integer.parseInt(row.get(6));
                    double price = Double.parseDouble(row.get(7));
                    if (quantity <= 0 || !Double.isFinite(price) || price < 0) throw new IllegalArgumentException("Invalid quantity or price.");
                    if (!Set.of("AWAITING_STOCK", "FULFILLED", "CANCELLED").contains(row.get(8))) throw new IllegalArgumentException("Unknown customer order status.");
                    orders.add(new CustomerOrder(row.get(0), row.get(1), row.get(2), row.get(3), row.get(4), row.get(5), quantity, price, row.get(8), row.get(9), row.get(10)));
                } catch (RuntimeException exception) { throw new IOException("Invalid customer order row: " + exception.getMessage(), exception); }
            }
        }
        private void loadRestockRequests() throws IOException {
            for (List<String> row : readRows(restockFile, RESTOCK_HEADER)) {
                try {
                    int quantity = Integer.parseInt(row.get(6));
                    if (quantity <= 0 || !Set.of("OPEN", "ORDERED", "RESOLVED").contains(row.get(7))) throw new IllegalArgumentException("Invalid request quantity or status.");
                    restockRequests.add(new RestockRequest(row.get(0), row.get(1), row.get(2), row.get(3), row.get(4), row.get(5), quantity, row.get(7)));
                } catch (RuntimeException exception) { throw new IOException("Invalid restock request row: " + exception.getMessage(), exception); }
            }
        }
        private void saveAccounts() throws IOException {
            List<String[]> rows = new ArrayList<>();
            for (CustomerAccount a : accounts) rows.add(new String[]{a.id, a.name, a.email, a.saltHex, a.passwordHashHex});
            writeRows(customersFile, CUSTOMER_HEADER, rows);
        }
        private void saveOrders() throws IOException {
            List<String[]> rows = new ArrayList<>();
            for (CustomerOrder o : orders) rows.add(new String[]{o.id, o.createdAt, o.customerId, o.customerName, o.productId, o.productName,
                    Integer.toString(o.quantity), String.format(Locale.ROOT, "%.2f", o.requestedUnitPrice), o.status, o.restockRequestId, o.saleReference});
            writeRows(ordersFile, CUSTOMER_ORDER_HEADER, rows);
        }
        private void saveRestockRequests() throws IOException {
            List<String[]> rows = new ArrayList<>();
            for (RestockRequest r : restockRequests) rows.add(new String[]{r.id, r.createdAt, r.orderId, r.customerName, r.productId, r.productName,
                    Integer.toString(r.quantityNeeded), r.status});
            writeRows(restockFile, RESTOCK_HEADER, rows);
        }
        static void ensureHeader(Path file, String[] header) throws IOException {
            if (Files.exists(file) && Files.size(file) > 0) return;
            Files.createDirectories(file.getParent());
            Files.writeString(file, CsvUtil.formatRow(header) + System.lineSeparator(), StandardCharsets.UTF_8);
        }
        static List<List<String>> readRows(Path file, String[] header) throws IOException {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.isEmpty()) return List.of();
            List<String> actualHeader;
            try { actualHeader = CsvUtil.parseLine(lines.get(0)); }
            catch (RuntimeException exception) { throw new IOException("Invalid CSV header in " + file + ".", exception); }
            if (!actualHeader.equals(List.of(header))) throw new IOException("Unexpected CSV columns in " + file + ". Check the header row.");
            List<List<String>> rows = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).isBlank()) continue;
                try {
                    List<String> row = CsvUtil.parseLine(lines.get(i));
                    if (row.size() != header.length) throw new IllegalArgumentException("Expected " + header.length + " columns, found " + row.size());
                    rows.add(row);
                } catch (RuntimeException exception) { throw new IOException("Invalid CSV data at " + file + ", row " + (i + 1) + ": " + exception.getMessage(), exception); }
            }
            return rows;
        }
        static void writeRows(Path file, String[] header, List<String[]> rows) throws IOException {
            Path parent = file.getParent();
            Files.createDirectories(parent);
            Path temp = Files.createTempFile(parent, "inventory-customer-", ".tmp");
            boolean moved = false;
            boolean contentReady = false;
            try {
                try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                    writer.write(CsvUtil.formatRow(header)); writer.newLine();
                    for (String[] row : rows) { writer.write(CsvUtil.formatRow(row)); writer.newLine(); }
                }
                contentReady = true;
                FileReplacer.replace(temp, file);
                moved = true;
            } finally {
                if (!contentReady) {
                    Files.deleteIfExists(temp);
                } else if (moved) {
                    // The CSV save already succeeded; do not turn cleanup failure into a failed operation.
                    try { Files.deleteIfExists(temp); } catch (IOException ignored) { /* safe to clean up later */ }
                }
            }
        }
        static String hashPassword(char[] password, byte[] salt) {
            PBEKeySpec spec = new PBEKeySpec(password, salt, HASH_ITERATIONS, 256);
            try { return HexFormat.of().formatHex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded()); }
            catch (Exception exception) { throw new IllegalStateException("Password hashing is unavailable in this Java runtime.", exception); }
            finally { spec.clearPassword(); }
        }
        private static String required(String value, String label) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
            return value.trim();
        }
        private static String newId(String prefix) { return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT); }
        private RestockRequest requireRestockRequest(String id) {
            RestockRequest request = findRestockRequest(id);
            if (request == null) throw new IllegalArgumentException("Restock request not found: " + id);
            return request;
        }
    }
