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

final class CustomerPanel extends AppPanel {
            private final JLabel accountStatus = new JLabel("Not signed in");
            private final JTabbedPane customerTabs = new JTabbedPane();
            private final JPanel productRows = new JPanel();
            private final JLabel productCount = new JLabel("0 products");
            private final DefaultTableModel ordersModel = readOnlyModel(
                    new Object[]{"Order ID", "Date", "Product", "Qty", "Status", "Restock request", "Sale reference"});
            private final JTable ordersTable = styledTable(ordersModel);
            private CustomerAccount currentAccount;

            CustomerPanel(MainFrame app) {
            super(app);
                setLayout(new BorderLayout(0, 12));
                setBackground(UiTheme.CANVAS);

                JPanel titleArea = new JPanel(new GridLayout(0, 1, 0, 5));
                titleArea.setOpaque(false);
                JLabel title = new JLabel("Customer Marketplace");
                title.setFont(new Font("SansSerif", Font.BOLD, 23));
                title.setForeground(UiTheme.INK);
                accountStatus.setForeground(UiTheme.MUTED);
                titleArea.add(title);
                titleArea.add(accountStatus);
                add(titleArea, BorderLayout.NORTH);

                productRows.setLayout(new BoxLayout(productRows, BoxLayout.Y_AXIS));
                productRows.setBackground(UiTheme.CANVAS);
                productRows.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));
                JScrollPane productScroll = new JScrollPane(productRows);
                productScroll.getViewport().setBackground(UiTheme.CANVAS);
                productScroll.setBorder(BorderFactory.createEmptyBorder());

                JPanel catalogPanel = new JPanel(new BorderLayout(0, 9));
                catalogPanel.setOpaque(false);
                JPanel catalogHeader = new JPanel(new BorderLayout(8, 0));
                catalogHeader.setOpaque(false);
                JPanel catalogTitle = new JPanel(new GridLayout(0, 1, 0, 3));
                catalogTitle.setOpaque(false);
                JLabel catalogHeading = new JLabel("Shop products");
                catalogHeading.setFont(new Font("SansSerif", Font.BOLD, 17));
                catalogHeading.setForeground(UiTheme.INK);
                JLabel catalogHint = new JLabel("Browse each product, check availability and order directly from its row.");
                catalogHint.setForeground(UiTheme.MUTED);
                catalogTitle.add(catalogHeading);
                catalogTitle.add(catalogHint);
                catalogHeader.add(catalogTitle, BorderLayout.WEST);
                JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                right.setOpaque(false);
                productCount.setForeground(UiTheme.MUTED);
                right.add(productCount);
                right.add(Box.createHorizontalStrut(12));
                right.add(secondary("Refresh", event -> { refreshProducts(); refreshOrders(); }));
                catalogHeader.add(right, BorderLayout.EAST);
                catalogPanel.add(catalogHeader, BorderLayout.NORTH);
                catalogPanel.add(productScroll, BorderLayout.CENTER);

                JPanel history = new JPanel(new BorderLayout(0, 8));
                history.setOpaque(false);
                JLabel historyTitle = new JLabel("My orders");
                historyTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
                historyTitle.setForeground(UiTheme.INK);
                JPanel historyHeader = new JPanel(new BorderLayout(8, 0));
                historyHeader.setOpaque(false);
                historyHeader.add(historyTitle, BorderLayout.WEST);
                historyHeader.add(secondary("Refresh orders", event -> refreshOrders()), BorderLayout.EAST);
                history.add(historyHeader, BorderLayout.NORTH);
                history.add(scroll(ordersTable), BorderLayout.CENTER);
                JLabel hint = new JLabel("Out-of-stock orders create a supplier restock request; stock changes only when an order is fulfilled.");
                hint.setForeground(UiTheme.MUTED);
                history.add(hint, BorderLayout.SOUTH);

                customerTabs.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
                customerTabs.setFont(new Font("SansSerif", Font.BOLD, 13));
                customerTabs.addTab("Products", new AppIcon(IconKind.PRODUCTS, UiTheme.TEAL, 17), catalogPanel);
                customerTabs.addTab("My Orders", new AppIcon(IconKind.ORDERS, UiTheme.ACCENT, 17), history);
                customerTabs.addChangeListener(event -> {
                    if (customerTabs.getSelectedIndex() == 1) refreshOrders();
                });
                add(customerTabs, BorderLayout.CENTER);
            }
            void setCustomerAccount(CustomerAccount account) {
                currentAccount = account;
                accountStatus.setText(account == null
                        ? "Not signed in"
                        : "Signed in as " + account.getName() + " (" + account.getEmail() + ")");
                refreshOrders();
            }

            private void placeCustomerOrder(String productId, int quantity) {
                if (currentAccount == null) {
                    showInfo("Sign in before placing a customer order.");
                    return;
                }
                if (quantity <= 0) {
                    showError("Quantity must be greater than zero.");
                    return;
                }
                Product product = service.findProduct(productId);
                if (product == null) {
                    showError("This product is no longer available. Refresh the catalog and try again.");
                    refreshProducts();
                    return;
                }
                String stockMessage = product.getQuantity() >= quantity
                        ? "Place order for " + quantity + " x " + product.getName() + " at " + currency(product.getSellingPrice()) + " each?"
                        : "Only " + product.getQuantity() + " unit(s) are currently available. Place the order and create a supplier restock request for the shortage?";
                if (JOptionPane.showConfirmDialog(app, stockMessage, "Confirm customer order",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) return;

                perform("Customer order", () -> {
                    CustomerOrder order = customerData.submitOrder(currentAccount, service, productId, quantity);
                    if (order.getStatus().equals("FULFILLED")) {
                        showInfo("Customer order fulfilled.\nOrder: " + order.getId() + "\nSale reference: " + order.getSaleReference());
                    } else {
                        showInfo("Customer order saved as awaiting stock.\nOrder: " + order.getId()
                                + "\nRestock request: " + order.getRestockRequestId()
                                + "\nThe Supplier module can create a purchase order for the shortage.");
                    }
                });
            }
            void refreshProducts() {
                productRows.removeAll();
                List<Product> products = service.getProducts();
                productCount.setText(products.size() + (products.size() == 1 ? " product" : " products"));
                if (products.isEmpty()) {
                    JPanel empty = new JPanel(new BorderLayout(0, 8));
                    empty.setBackground(Color.WHITE);
                    empty.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.BORDER),
                            BorderFactory.createEmptyBorder(24, 24, 24, 24)));
                    JLabel title = new JLabel("No products are available yet");
                    title.setFont(new Font("SansSerif", Font.BOLD, 17));
                    title.setForeground(UiTheme.INK);
                    JLabel hint = new JLabel("Products added by the Inventory / Seller workspace will appear here.");
                    hint.setForeground(UiTheme.MUTED);
                    empty.add(title, BorderLayout.NORTH);
                    empty.add(hint, BorderLayout.CENTER);
                    empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
                    productRows.add(empty);
                } else {
                    for (Product product : products) {
                        ProductMarketplaceRow row = new ProductMarketplaceRow(product);
                        row.setAlignmentX(Component.LEFT_ALIGNMENT);
                        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 118));
                        productRows.add(row);
                        productRows.add(Box.createVerticalStrut(9));
                    }
                }
                productRows.revalidate();
                productRows.repaint();
            }
            void refreshOrders() {
                ordersModel.setRowCount(0);
                if (currentAccount == null) return;
                for (CustomerOrder order : customerData.getOrdersForCustomer(currentAccount.getId())) {
                    ordersModel.addRow(new Object[]{order.getId(), order.getCreatedAt(), order.getProductName(), order.getQuantity(),
                            order.getStatus(), order.getRestockRequestId(), order.getSaleReference()});
                }
            }

            private final class ProductMarketplaceRow extends JPanel {
                private ProductMarketplaceRow(Product product) {
                    setLayout(new BorderLayout(14, 0));
                    setBackground(Color.WHITE);
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(UiTheme.BORDER, 1, true),
                            BorderFactory.createEmptyBorder(10, 12, 10, 12)));

                    ProductArtwork artwork = new ProductArtwork(product.getCategory(), product.getName());
                    artwork.setPreferredSize(new Dimension(92, 88));
                    add(artwork, BorderLayout.WEST);

                    JPanel details = new JPanel(new GridLayout(0, 1, 0, 4));
                    details.setOpaque(false);
                    JLabel name = new JLabel(product.getName());
                    name.setFont(new Font("SansSerif", Font.BOLD, 16));
                    name.setForeground(UiTheme.INK);
                    JLabel category = new JLabel(product.getCategory() + "  |  SKU: " + product.getId());
                    category.setForeground(UiTheme.MUTED);
                    JLabel stock = new JLabel(product.getQuantity() <= 0 ? "OUT OF STOCK"
                            : product.isLowStock() ? "LOW STOCK - " + product.getQuantity() + " available"
                            : "IN STOCK - " + product.getQuantity() + " available");
                    stock.setFont(new Font("SansSerif", Font.BOLD, 12));
                    stock.setForeground(product.getQuantity() <= 0 ? UiTheme.DANGER
                            : product.isLowStock() ? new Color(146, 64, 14) : UiTheme.SUCCESS);
                    details.add(name);
                    details.add(category);
                    details.add(stock);
                    add(details, BorderLayout.CENTER);

                    JPanel action = new JPanel(new GridBagLayout());
                    action.setOpaque(false);
                    GridBagConstraints c = new GridBagConstraints();
                    c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.EAST; c.insets = new Insets(0, 0, 5, 0);
                    JLabel price = new JLabel(currency(product.getSellingPrice()));
                    price.setFont(new Font("SansSerif", Font.BOLD, 18));
                    price.setForeground(UiTheme.INK);
                    action.add(price, c);
                    c.gridy = 1; c.insets = new Insets(0, 0, 6, 0);
                    JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
                    quantity.setPreferredSize(new Dimension(78, 28));
                    quantity.getAccessibleContext().setAccessibleName("Quantity for " + product.getName());
                    action.add(quantity, c);
                    c.gridy = 2; c.insets = new Insets(0, 0, 0, 0);
                    JButton order = primary(product.getQuantity() <= 0 ? "Order / Restock" : "Order now",
                            event -> placeCustomerOrder(product.getId(), (Integer) quantity.getValue()));
                    // Preserve the existing workflow: an out-of-stock order can create a supplier restock request.
                    order.setToolTipText(product.getQuantity() <= 0
                            ? "Place an order request; a supplier restock request will be created for the shortage."
                            : "Place an order for this product");
                    action.add(order, c);
                    add(action, BorderLayout.EAST);
                }
            }
        }
