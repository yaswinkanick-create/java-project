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

final class InventoryService {
        private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        private final CsvDatabase database;
        private ArrayList<Product> products;
        private ArrayList<Supplier> suppliers;
        private ArrayList<TransactionRecord> transactions;
        private ArrayList<PurchaseOrder> purchaseOrders;

        public InventoryService(CsvDatabase database) throws IOException {
            this.database = database;
            products = new ArrayList<>(database.loadProducts());
            suppliers = new ArrayList<>(database.loadSuppliers());
            transactions = new ArrayList<>(database.loadTransactions());
            purchaseOrders = new ArrayList<>(database.loadPurchaseOrders());
            ensureUniqueLoadedIds();
        }

        public record CartItem(String productId, int quantity) { }

        public record ReturnOption(String saleId, String productId, String productName,
                                   String customer, int remainingQuantity, double netUnitPrice) {
            @Override public String toString() {
                return saleId + "  |  " + productName + "  |  remaining " + remainingQuantity;
            }
        }

        public List<Product> getProducts() {
            List<Product> result = new ArrayList<>(products);
            result.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
            return List.copyOf(result);
        }

        public List<Supplier> getSuppliers() {
            List<Supplier> result = new ArrayList<>(suppliers);
            result.sort(Comparator.comparing(Supplier::getName, String.CASE_INSENSITIVE_ORDER));
            return List.copyOf(result);
        }

        public List<TransactionRecord> getTransactions() {
            List<TransactionRecord> result = new ArrayList<>(transactions);
            result.sort(Comparator.comparing(TransactionRecord::getDateTime).reversed());
            return List.copyOf(result);
        }

        public List<TransactionRecord> getTransactionsByType(String type) {
            return getTransactions().stream().filter(t -> t.getType().equalsIgnoreCase(type)).toList();
        }

        public List<PurchaseOrder> getPurchaseOrders() {
            List<PurchaseOrder> result = new ArrayList<>(purchaseOrders);
            result.sort(Comparator.comparing(PurchaseOrder::getCreatedAt).reversed());
            return List.copyOf(result);
        }

        public PurchaseOrder findPurchaseOrder(String id) {
            if (id == null) return null;
            return purchaseOrders.stream().filter(order -> order.getId().equalsIgnoreCase(id.trim())).findFirst().orElse(null);
        }

        public Product findProduct(String id) {
            if (id == null) return null;
            return products.stream().filter(p -> p.getId().equalsIgnoreCase(id.trim())).findFirst().orElse(null);
        }

        public Supplier findSupplier(String id) {
            if (id == null) return null;
            return suppliers.stream().filter(s -> s.getId().equalsIgnoreCase(id.trim())).findFirst().orElse(null);
        }

        public void addProduct(String id, String name, String category, double unitCost,
                               double sellingPrice, int openingQuantity, int reorderLevel) throws IOException {
            Product newProduct = new Product(id, name, category, unitCost, sellingPrice, openingQuantity, reorderLevel);
            if (findProduct(newProduct.getId()) != null) {
                throw new IllegalArgumentException("That product ID already exists.");
            }
            commit(() -> {
                products.add(newProduct);
                if (openingQuantity > 0) {
                    transactions.add(record("ADJUSTMENT", newId("OPEN"), newProduct, openingQuantity,
                            newProduct.getSellingPrice(), newProduct.getUnitCost(), 0,
                            "Opening balance", "", "", "Opening stock"));
                }
            });
        }

        public void updateProduct(String id, String name, String category, double unitCost,
                                  double sellingPrice, int reorderLevel) throws IOException {
            Product product = requireProduct(id);
            // Validate details before changing existing state.
            new Product(product.getId(), name, category, unitCost, sellingPrice, product.getQuantity(), reorderLevel);
            commit(() -> product.updateDetails(name, category, unitCost, sellingPrice, reorderLevel));
        }

        public void deleteProduct(String id) throws IOException {
            Product product = requireProduct(id);
            if (product.getQuantity() != 0) {
                throw new IllegalArgumentException("Only products with zero stock and no purchase-order or transaction history can be deleted.");
            }
            boolean hasHistory = transactions.stream().anyMatch(t -> t.getProductId().equalsIgnoreCase(product.getId()));
            boolean referencedByOrder = purchaseOrders.stream().anyMatch(order -> order.getProductId().equalsIgnoreCase(product.getId()));
            if (hasHistory || referencedByOrder) {
                throw new IllegalArgumentException("This product has purchase-order or transaction history and cannot be deleted. You can edit its details instead.");
            }
            commit(() -> products.remove(product));
        }

        public void addSupplier(String id, String name, String phone, String email) throws IOException {
            Supplier newSupplier = new Supplier(id, name, phone, email);
            if (findSupplier(newSupplier.getId()) != null) {
                throw new IllegalArgumentException("That supplier ID already exists.");
            }
            commit(() -> suppliers.add(newSupplier));
        }

        public void updateSupplier(String id, String name, String phone, String email) throws IOException {
            Supplier supplier = requireSupplier(id);
            new Supplier(supplier.getId(), name, phone, email);
            commit(() -> supplier.updateDetails(name, phone, email));
        }

        public void deleteSupplier(String id) throws IOException {
            Supplier supplier = requireSupplier(id);
            boolean usedInPurchases = transactions.stream().anyMatch(t -> t.getType().equals("PURCHASE")
                    && t.getRelatedId().equalsIgnoreCase(supplier.getId()));
            boolean referencedByOrder = purchaseOrders.stream().anyMatch(order -> order.getSupplierId().equalsIgnoreCase(supplier.getId()));
            if (usedInPurchases || referencedByOrder) {
                throw new IllegalArgumentException("This supplier is referenced by purchase-order or purchase history and cannot be deleted.");
            }
            commit(() -> suppliers.remove(supplier));
        }

        public String createPurchaseOrder(String productId, String supplierId, int orderedQuantity,
                                         double unitCost, String note) throws IOException {
            Product product = requireProduct(productId);
            Supplier supplier = requireSupplier(supplierId);
            if (orderedQuantity <= 0) throw new IllegalArgumentException("Ordered quantity must be greater than zero.");
            if (!Double.isFinite(unitCost) || unitCost < 0) throw new IllegalArgumentException("Unit cost must be a valid non-negative number.");
            String orderId = newId("PO");
            PurchaseOrder order = new PurchaseOrder(orderId, LocalDateTime.now().format(DATE_FORMAT),
                    supplier.getId(), supplier.getName(), product.getId(), product.getName(),
                    orderedQuantity, 0, unitCost, "DRAFT", cleanNote(note));
            commit(() -> purchaseOrders.add(order));
            return orderId;
        }

        public void markPurchaseOrderOrdered(String orderId) throws IOException {
            PurchaseOrder order = requirePurchaseOrder(orderId);
            if (!order.getStatus().equals("DRAFT")) {
                throw new IllegalArgumentException("Only draft purchase orders can be marked as ordered.");
            }
            commit(order::markOrdered);
        }

        public void cancelPurchaseOrder(String orderId) throws IOException {
            PurchaseOrder order = requirePurchaseOrder(orderId);
            if (order.getStatus().equals("RECEIVED") || order.getStatus().equals("CANCELLED")) {
                throw new IllegalArgumentException("A completed or already cancelled purchase order cannot be cancelled.");
            }
            commit(order::cancel);
        }

        public String receivePurchaseOrderDelivery(String orderId, int quantity, String deliveryNote) throws IOException {
            PurchaseOrder order = requirePurchaseOrder(orderId);
            Product product = requireProduct(order.getProductId());
            Supplier supplier = requireSupplier(order.getSupplierId());
            if (!order.getStatus().equals("ORDERED") && !order.getStatus().equals("PARTIALLY_RECEIVED")) {
                throw new IllegalArgumentException("Mark this purchase order as ordered before receiving a delivery.");
            }
            if (quantity <= 0) throw new IllegalArgumentException("Delivery quantity must be greater than zero.");
            if (quantity > order.getRemainingQuantity()) {
                throw new IllegalArgumentException("Only " + order.getRemainingQuantity() + " unit(s) remain outstanding on this purchase order.");
            }
            if ((long) product.getQuantity() + quantity > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("The resulting stock quantity is too large.");
            }
            String receiptId = newId("REC");
            String note = "Purchase order " + order.getId();
            if (!cleanNote(deliveryNote).isEmpty()) note += " | " + cleanNote(deliveryNote);
            final String transactionNote = note;
            commit(() -> {
                order.receive(quantity);
                product.changeQuantity(quantity);
                product.updateDetails(product.getName(), product.getCategory(), order.getUnitCost(),
                        product.getSellingPrice(), product.getReorderLevel());
                transactions.add(record("PURCHASE", receiptId, product, quantity, order.getUnitCost(),
                        order.getUnitCost(), money(quantity * order.getUnitCost()), supplier.getName(),
                        "", supplier.getId(), transactionNote));
            });
            return receiptId;
        }

        public String createSale(List<CartItem> cart, String customer, String paymentMethod,
                                 double discountPercent) throws IOException {
            return createSaleInternal(cart, customer, paymentMethod, discountPercent, null);
        }

        /** Idempotent sale entry point for a customer order; retrying the same order cannot sell it twice. */
        public String createSaleForCustomerOrder(List<CartItem> cart, String customer, String orderId) throws IOException {
            String stableReference = "SAL-" + orderId;
            boolean alreadyRecorded = transactions.stream().anyMatch(t -> t.getType().equals("SALE")
                    && t.getReferenceId().equalsIgnoreCase(stableReference));
            if (alreadyRecorded) return stableReference;
            return createSaleInternal(cart, customer, "Customer Order", 0, stableReference);
        }

        private String createSaleInternal(List<CartItem> cart, String customer, String paymentMethod,
                                          double discountPercent, String stableSaleId) throws IOException {
            if (cart == null || cart.isEmpty()) throw new IllegalArgumentException("Add at least one item to the bill.");
            if (!Double.isFinite(discountPercent) || discountPercent < 0 || discountPercent > 100) {
                throw new IllegalArgumentException("Discount must be between 0 and 100 percent.");
            }
            if (paymentMethod == null || paymentMethod.isBlank()) throw new IllegalArgumentException("Choose a payment method.");
            String customerName = customer == null || customer.isBlank() ? "Walk-in customer" : customer.trim();
            Set<String> seen = new HashSet<>();
            double grossTotal = 0;
            for (CartItem item : cart) {
                if (item.quantity() <= 0) throw new IllegalArgumentException("Cart quantities must be greater than zero.");
                if (!seen.add(item.productId().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("A product appears more than once in the cart. Remove the duplicate line.");
                }
                Product product = requireProduct(item.productId());
                if (item.quantity() > product.getQuantity()) {
                    throw new IllegalArgumentException("Insufficient stock for " + product.getName() + ". Available: " + product.getQuantity() + ".");
                }
                grossTotal += product.getSellingPrice() * item.quantity();
            }
            final double grossAmount = grossTotal;
            final double netTotal = money(grossAmount * (1.0 - discountPercent / 100.0));
            final String saleId = stableSaleId == null ? newId("SAL") : stableSaleId;
            final String discountNote = "Discount " + money(discountPercent) + "%";
            commit(() -> {
                double assignedNet = 0;
                for (int i = 0; i < cart.size(); i++) {
                    CartItem item = cart.get(i);
                    Product product = requireProduct(item.productId());
                    double grossLine = money(product.getSellingPrice() * item.quantity());
                    double netLine;
                    if (i == cart.size() - 1) {
                        netLine = money(netTotal - assignedNet);
                    } else if (grossAmount == 0) {
                        netLine = 0;
                    } else {
                        netLine = money(netTotal * grossLine / grossAmount);
                        assignedNet = money(assignedNet + netLine);
                    }
                    product.changeQuantity(-item.quantity());
                    transactions.add(record("SALE", saleId, product, item.quantity(), product.getSellingPrice(),
                            product.getUnitCost(), netLine, customerName, paymentMethod, "", discountNote));
                }
            });
            return saleId;
        }

        public List<ReturnOption> getReturnOptions() {
            List<ReturnOption> options = new ArrayList<>();
            for (TransactionRecord sale : transactions) {
                if (!sale.getType().equals("SALE")) continue;
                int returned = transactions.stream().filter(t -> t.getType().equals("RETURN")
                        && t.getRelatedId().equals(sale.getReferenceId())
                        && t.getProductId().equalsIgnoreCase(sale.getProductId()))
                        .mapToInt(TransactionRecord::getQuantity).sum();
                int remaining = sale.getQuantity() - returned;
                if (remaining > 0) {
                    double effectiveUnit = sale.getQuantity() == 0 ? 0 : sale.getTotal() / sale.getQuantity();
                    options.add(new ReturnOption(sale.getReferenceId(), sale.getProductId(), sale.getProductName(),
                            sale.getPartyName(), remaining, money(effectiveUnit)));
                }
            }
            options.sort(Comparator.comparing(ReturnOption::saleId).reversed().thenComparing(ReturnOption::productName));
            return List.copyOf(options);
        }

        public String processReturn(String saleId, String productId, int quantity,
                                    boolean restock, String reason) throws IOException {
            if (quantity <= 0) throw new IllegalArgumentException("Return quantity must be greater than zero.");
            TransactionRecord original = transactions.stream().filter(t -> t.getType().equals("SALE")
                    && t.getReferenceId().equalsIgnoreCase(saleId)
                    && t.getProductId().equalsIgnoreCase(productId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("The selected sale item was not found."));
            ReturnOption option = getReturnOptions().stream().filter(o -> o.saleId().equalsIgnoreCase(saleId)
                    && o.productId().equalsIgnoreCase(productId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("This sale item has no quantity left to return."));
            if (quantity > option.remainingQuantity()) {
                throw new IllegalArgumentException("Only " + option.remainingQuantity() + " unit(s) remain eligible for return.");
            }
            Product product = requireProduct(productId);
            if (restock && (long) product.getQuantity() + quantity > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("The resulting stock quantity is too large.");
            }
            String returnId = newId("RET");
            double refund = money((original.getTotal() / original.getQuantity()) * quantity);
            String note = (restock ? "RESTOCKED: " : "NOT_RESTOCKED: ") + cleanNote(reason);
            commit(() -> {
                if (restock) product.changeQuantity(quantity);
                transactions.add(record("RETURN", returnId, product, quantity, original.getUnitPrice(),
                        original.getUnitCost(), refund, original.getPartyName(), original.getPaymentMethod(),
                        saleId, note));
            });
            return returnId;
        }

        public void adjustStock(String productId, int countedQuantity, String reason) throws IOException {
            Product product = requireProduct(productId);
            if (countedQuantity < 0) throw new IllegalArgumentException("Counted quantity cannot be negative.");
            int delta = countedQuantity - product.getQuantity();
            if (delta == 0) throw new IllegalArgumentException("The counted quantity matches the current quantity; no adjustment is needed.");
            String adjustmentId = newId("ADJ");
            commit(() -> {
                product.setQuantity(countedQuantity);
                transactions.add(record("ADJUSTMENT", adjustmentId, product, delta, product.getSellingPrice(),
                        product.getUnitCost(), 0, "", "", "", cleanNote(reason)));
            });
        }

        public void importProducts(Path path) throws IOException {
            List<Product> imported = database.readProductsFrom(path);
            if (imported.isEmpty()) throw new IllegalArgumentException("The selected CSV contains no product rows.");
            Set<String> importedIds = new HashSet<>();
            for (Product p : imported) {
                if (!importedIds.add(p.getId().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("The import file contains duplicate product ID: " + p.getId());
                }
            }
            commit(() -> {
                for (Product incoming : imported) {
                    Product existing = findProduct(incoming.getId());
                    if (existing == null) {
                        products.add(incoming);
                        if (incoming.getQuantity() > 0) {
                            transactions.add(record("ADJUSTMENT", newId("IMP"), incoming, incoming.getQuantity(),
                                    incoming.getSellingPrice(), incoming.getUnitCost(), 0,
                                    "", "", "", "Opening quantity from CSV import"));
                        }
                    } else {
                        int delta = incoming.getQuantity() - existing.getQuantity();
                        existing.updateDetails(incoming.getName(), incoming.getCategory(), incoming.getUnitCost(),
                                incoming.getSellingPrice(), incoming.getReorderLevel());
                        existing.setQuantity(incoming.getQuantity());
                        if (delta != 0) {
                            transactions.add(record("ADJUSTMENT", newId("IMP"), existing, delta,
                                    existing.getSellingPrice(), existing.getUnitCost(), 0,
                                    "", "", "", "Quantity updated by CSV import"));
                        }
                    }
                }
            });
        }

        public void exportProducts(Path path) throws IOException {
            database.exportProductsTo(path, getProducts());
        }

        public int getProductCount() { return products.size(); }
        public int getTotalUnits() { return products.stream().mapToInt(Product::getQuantity).sum(); }
        public int getLowStockCount() { return (int) products.stream().filter(Product::isLowStock).count(); }

        /** Products at or below their configured reorder threshold, most urgent first. */
        public List<Product> getLowStockProducts() {
            return products.stream().filter(Product::isLowStock)
                    .sorted(Comparator.comparingInt(Product::getQuantity)
                            .thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
        public double getStockValue() { return money(products.stream().mapToDouble(p -> p.getUnitCost() * p.getQuantity()).sum()); }
        public double getSalesTotal() { return sumType("SALE"); }
        public double getRefundTotal() { return sumType("RETURN"); }
        public double getNetSales() { return money(getSalesTotal() - getRefundTotal()); }
        public double getPurchaseTotal() { return sumType("PURCHASE"); }

        public double getGrossProfitEstimate() {
            double salesCost = transactions.stream().filter(t -> t.getType().equals("SALE"))
                    .mapToDouble(t -> t.getQuantity() * t.getUnitCost()).sum();
            double recoveredCost = transactions.stream().filter(t -> t.getType().equals("RETURN")
                            && t.getNote().startsWith("RESTOCKED:"))
                    .mapToDouble(t -> t.getQuantity() * t.getUnitCost()).sum();
            return money(getNetSales() - (salesCost - recoveredCost));
        }

        public Path getDataDirectory() { return database.getDataDirectory(); }

        private double sumType(String type) {
            return money(transactions.stream().filter(t -> t.getType().equals(type)).mapToDouble(TransactionRecord::getTotal).sum());
        }

        private Product requireProduct(String id) {
            Product product = findProduct(id);
            if (product == null) throw new IllegalArgumentException("Product not found: " + id);
            return product;
        }

        private Supplier requireSupplier(String id) {
            Supplier supplier = findSupplier(id);
            if (supplier == null) throw new IllegalArgumentException("Supplier not found: " + id);
            return supplier;
        }

        private PurchaseOrder requirePurchaseOrder(String id) {
            PurchaseOrder order = findPurchaseOrder(id);
            if (order == null) throw new IllegalArgumentException("Purchase order not found: " + id);
            return order;
        }

        private TransactionRecord record(String type, String referenceId, Product product, int quantity,
                                         double unitPrice, double unitCost, double total, String partyName,
                                         String paymentMethod, String relatedId, String note) {
            return new TransactionRecord(UUID.randomUUID().toString(), LocalDateTime.now().format(DATE_FORMAT),
                    type, referenceId, product.getId(), product.getName(), quantity, unitPrice, unitCost,
                    total, partyName, paymentMethod, relatedId, note);
        }

        private String newId(String prefix) {
            return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        }

        private static String cleanNote(String note) {
            return note == null ? "" : note.trim();
        }

        private static double money(double value) { return Math.round(value * 100.0) / 100.0; }

        private void commit(Runnable operation) throws IOException {
            ArrayList<Product> oldProducts = copyProducts(products);
            ArrayList<Supplier> oldSuppliers = copySuppliers(suppliers);
            ArrayList<TransactionRecord> oldTransactions = new ArrayList<>(transactions);
            ArrayList<PurchaseOrder> oldPurchaseOrders = copyPurchaseOrders(purchaseOrders);
            try {
                operation.run();
                database.saveProducts(products);
                database.saveSuppliers(suppliers);
                database.saveTransactions(transactions);
                database.savePurchaseOrders(purchaseOrders);
            } catch (RuntimeException | IOException exception) {
                products = oldProducts;
                suppliers = oldSuppliers;
                transactions = oldTransactions;
                purchaseOrders = oldPurchaseOrders;
                throw exception;
            }
        }

        private static ArrayList<Product> copyProducts(List<Product> source) {
            ArrayList<Product> copy = new ArrayList<>();
            for (Product p : source) {
                copy.add(new Product(p.getId(), p.getName(), p.getCategory(), p.getUnitCost(),
                        p.getSellingPrice(), p.getQuantity(), p.getReorderLevel()));
            }
            return copy;
        }

        private static ArrayList<Supplier> copySuppliers(List<Supplier> source) {
            ArrayList<Supplier> copy = new ArrayList<>();
            for (Supplier s : source) copy.add(new Supplier(s.getId(), s.getName(), s.getPhone(), s.getEmail()));
            return copy;
        }

        private static ArrayList<PurchaseOrder> copyPurchaseOrders(List<PurchaseOrder> source) {
            ArrayList<PurchaseOrder> copy = new ArrayList<>();
            for (PurchaseOrder order : source) copy.add(order.copy());
            return copy;
        }

        private void ensureUniqueLoadedIds() throws IOException {
            Set<String> ids = new HashSet<>();
            for (Product p : products) {
                if (!ids.add(p.getId().toLowerCase(Locale.ROOT))) throw new IOException("Duplicate product ID in products.csv: " + p.getId());
            }
            ids.clear();
            for (Supplier s : suppliers) {
                if (!ids.add(s.getId().toLowerCase(Locale.ROOT))) throw new IOException("Duplicate supplier ID in suppliers.csv: " + s.getId());
            }
            ids.clear();
            for (PurchaseOrder order : purchaseOrders) {
                if (!ids.add(order.getId().toLowerCase(Locale.ROOT))) throw new IOException("Duplicate purchase order ID in purchase_orders.csv: " + order.getId());
            }
        }
    }
