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

final class MainFrame extends JFrame {
        final InventoryService service;
        final CustomerDataStore customerData;
        final RoleAuthStore roleAuth;
        final CardLayout contentLayout = new CardLayout();
        final JPanel contentCards = new JPanel(contentLayout);
        private final JTabbedPane inventoryTabs = new JTabbedPane();
        final JTabbedPane supplierTabs = new JTabbedPane();
        private final JButton signOutButton = new JButton("Change role / Sign out");
        private final JButton stockAlertButton = new JButton("Stock healthy");
        private final Map<String, String> previousStockStates = new HashMap<>();
        private boolean stockAlertBaselineReady;
        RoleLoginPanel loginPanel;
        UserRole currentRole;
        final CustomerPanel customerPanel;
        final RestockRequestsPanel restockRequestsPanel;
        final DashboardPanel dashboardPanel;
        final ProductsPanel productsPanel;
        final SuppliersPanel suppliersPanel;
        final PurchasingPanel purchasingPanel;
        final SalesPanel salesPanel;
        final ReturnsPanel returnsPanel;
        final StockPanel stockPanel;
        final ReportsPanel reportsPanel;
        final JLabel statusLabel = new JLabel("Ready");

        public MainFrame(InventoryService service, CustomerDataStore customerData, RoleAuthStore roleAuth) {
            super("Inventory Manager");
            this.service = service;
            this.customerData = customerData;
            this.roleAuth = roleAuth;
            this.customerPanel = new CustomerPanel(this);
            this.restockRequestsPanel = new RestockRequestsPanel(this);
            this.dashboardPanel = new DashboardPanel(this);
            this.productsPanel = new ProductsPanel(this);
            this.suppliersPanel = new SuppliersPanel(this);
            this.purchasingPanel = new PurchasingPanel(this);
            this.salesPanel = new SalesPanel(this);
            this.returnsPanel = new ReturnsPanel(this);
            this.stockPanel = new StockPanel(this);
            this.reportsPanel = new ReportsPanel(this);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setMinimumSize(new Dimension(1050, 690));
            setSize(1280, 820);
            setLocationRelativeTo(null);
            getContentPane().setBackground(UiTheme.CANVAS);
            contentCards.setBackground(UiTheme.CANVAS);
            setLayout(new BorderLayout(0, 0));
            add(buildHeader(), BorderLayout.NORTH);
            buildTabs();
            add(contentCards, BorderLayout.CENTER);
            add(buildStatusBar(), BorderLayout.SOUTH);
            refreshAll();
            contentLayout.show(contentCards, "ROLE_SELECT");
            statusLabel.setText("Select a role to continue");
        }

        private JComponent buildHeader() {
            JPanel header = new JPanel(new BorderLayout(12, 0));
            header.setBackground(UiTheme.HEADER);
            header.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 3, 0, new Color(96, 165, 250)),
                    BorderFactory.createEmptyBorder(18, 24, 18, 24)));
            JPanel brand = new JPanel(new GridLayout(2, 1, 0, 3));
            brand.setOpaque(false);
            JLabel title = new JLabel("Inventory Manager",
                    new AppIcon(IconKind.BRAND, Color.WHITE, 22), SwingConstants.LEFT);
            title.setIconTextGap(10);
            title.setFont(new Font("SansSerif", Font.BOLD, 23));
            title.setForeground(Color.WHITE);
            JLabel subtitle = new JLabel("Purchasing | Sales | Stock control | Reporting");
            subtitle.setForeground(new Color(210, 224, 242));
            subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
            brand.add(title);
            brand.add(subtitle);
            UiTheme.styleSecondary(signOutButton);
            signOutButton.setIcon(new AppIcon(IconKind.EXIT, UiTheme.INK, 16));
            signOutButton.setIconTextGap(8);
            signOutButton.addActionListener(event -> signOut());
            signOutButton.setVisible(false);
            JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            headerActions.setOpaque(false);
            configureStockAlertButton();
            headerActions.add(stockAlertButton);
            headerActions.add(signOutButton);
            header.add(brand, BorderLayout.WEST);
            header.add(headerActions, BorderLayout.EAST);
            return header;
        }

        private JComponent buildStatusBar() {
            JPanel status = new JPanel(new BorderLayout());
            status.setBackground(UiTheme.SURFACE);
            status.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                    BorderFactory.createEmptyBorder(7, 18, 7, 18)));
            statusLabel.setForeground(UiTheme.ACCENT);
            statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            JLabel storage = new JLabel("CSV data: " + service.getDataDirectory());
            storage.setForeground(UiTheme.MUTED);
            storage.setFont(new Font("SansSerif", Font.PLAIN, 12));
            status.add(statusLabel, BorderLayout.WEST);
            status.add(storage, BorderLayout.EAST);
            return status;
        }

        private void buildTabs() {
            // Each role lands directly in its own workspace after authentication.
            inventoryTabs.addTab("Overview", new AppIcon(IconKind.OVERVIEW, UiTheme.ACCENT, 17), dashboardPanel);
            inventoryTabs.addTab("Products", new AppIcon(IconKind.PRODUCTS, UiTheme.TEAL, 17), productsPanel);
            inventoryTabs.addTab("Sales & Billing", new AppIcon(IconKind.SALES, UiTheme.ACCENT, 17), salesPanel);
            inventoryTabs.addTab("Returns", new AppIcon(IconKind.RETURNS, UiTheme.AMBER, 17), returnsPanel);
            inventoryTabs.addTab("Stock Control", new AppIcon(IconKind.STOCK, UiTheme.TEAL, 17), stockPanel);
            inventoryTabs.addTab("Reports", new AppIcon(IconKind.REPORTS, UiTheme.PURPLE, 17), reportsPanel);
            supplierTabs.addTab("Suppliers", new AppIcon(IconKind.SUPPLIERS, UiTheme.TEAL, 17), suppliersPanel);
            supplierTabs.addTab("Purchasing", new AppIcon(IconKind.PURCHASING, UiTheme.ACCENT, 17), purchasingPanel);
            supplierTabs.addTab("Restock Requests", new AppIcon(IconKind.RESTOCK, UiTheme.AMBER, 17), restockRequestsPanel);
            inventoryTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
            supplierTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
            inventoryTabs.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            supplierTabs.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            customerPanel.setBorder(BorderFactory.createEmptyBorder(16, 8, 8, 8));

            contentCards.add(RoleSelectionPanel.build(this), "ROLE_SELECT");
            contentCards.add(customerPanel, "CUSTOMER");
            contentCards.add(inventoryTabs, "INVENTORY_SELLER");
            contentCards.add(supplierTabs, "SUPPLIER");
            contentCards.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        }



        /** Lightweight vector artwork keeps the single-file project self-contained. */


        void showLogin(UserRole role) {
            if (loginPanel != null) contentCards.remove(loginPanel);
            loginPanel = new RoleLoginPanel(this, role);
            contentCards.add(loginPanel, "LOGIN");
            contentCards.revalidate();
            contentCards.repaint();
            contentLayout.show(contentCards, "LOGIN");
            statusLabel.setText("Sign in to " + role.title);
        }

        void showWorkspace(UserRole role, CustomerAccount account) {
            currentRole = role;
            customerPanel.setCustomerAccount(role == UserRole.CUSTOMER ? account : null);
            refreshAll();
            String card = switch (role) {
                case CUSTOMER -> "CUSTOMER";
                case INVENTORY_SELLER -> "INVENTORY_SELLER";
                case SUPPLIER -> "SUPPLIER";
            };
            signOutButton.setVisible(true);
            stockAlertButton.setVisible(role != UserRole.CUSTOMER);
            statusLabel.setText("Signed in to " + role.title);
            contentLayout.show(contentCards, card);
        }

        void signOut() {
            customerPanel.setCustomerAccount(null);
            currentRole = null;
            if (loginPanel != null) {
                contentCards.remove(loginPanel);
                loginPanel = null;
            }
            signOutButton.setVisible(false);
            stockAlertButton.setVisible(false);
            statusLabel.setText("Select a role to continue");
            contentLayout.show(contentCards, "ROLE_SELECT");
        }



        void refreshAll() {
            customerPanel.refreshProducts();
            customerPanel.refreshOrders();
            dashboardPanel.refresh();
            productsPanel.refresh();
            suppliersPanel.refresh();
            purchasingPanel.refresh();
            restockRequestsPanel.refresh();
            salesPanel.refreshProducts();
            returnsPanel.refresh();
            stockPanel.refresh();
            reportsPanel.refresh();
            refreshStockAlertState();
        }

        private void configureStockAlertButton() {
            stockAlertButton.setIcon(new AppIcon(IconKind.ALERT, UiTheme.TEAL, 16));
            stockAlertButton.setIconTextGap(7);
            stockAlertButton.setFont(new Font("SansSerif", Font.BOLD, 12));
            stockAlertButton.setFocusPainted(true);
            stockAlertButton.setOpaque(true);
            stockAlertButton.setContentAreaFilled(true);
            stockAlertButton.setBorderPainted(true);
            stockAlertButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            stockAlertButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UiTheme.BORDER, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            stockAlertButton.addActionListener(event -> showStockAlerts());
            stockAlertButton.setVisible(false);
        }

        /** Re-evaluates thresholds after every saved operation and updates the live indicator. */
        private void refreshStockAlertState() {
            List<Product> alerts = service.getLowStockProducts();
            Map<String, String> nextStates = new HashMap<>();
            List<String> newlyTriggered = new ArrayList<>();
            List<String> newlyOutOfStock = new ArrayList<>();
            for (Product product : service.getProducts()) {
                String key = product.getId().toLowerCase(Locale.ROOT);
                String state = product.getQuantity() == 0 ? "OUT"
                        : product.isLowStock() ? "LOW" : "OK";
                String previous = previousStockStates.get(key);
                if (stockAlertBaselineReady && (previous == null || "OK".equals(previous))
                        && !"OK".equals(state)) {
                    newlyTriggered.add(product.getName());
                } else if (stockAlertBaselineReady && "LOW".equals(previous) && "OUT".equals(state)) {
                    newlyTriggered.add(product.getName());
                }
                if (stockAlertBaselineReady && "OUT".equals(state) && !"OUT".equals(previous)) {
                    newlyOutOfStock.add(product.getName());
                }
                nextStates.put(key, state);
            }
            previousStockStates.clear();
            previousStockStates.putAll(nextStates);
            stockAlertBaselineReady = true;

            int outOfStock = (int) alerts.stream().filter(product -> product.getQuantity() == 0).count();
            if (alerts.isEmpty()) {
                stockAlertButton.setText("Stock healthy");
                stockAlertButton.setIcon(new AppIcon(IconKind.ALERT, UiTheme.SUCCESS, 16));
                stockAlertButton.setBackground(new Color(236, 253, 245));
                stockAlertButton.setForeground(UiTheme.SUCCESS);
                stockAlertButton.setToolTipText("All products are above their reorder thresholds.");
            } else {
                stockAlertButton.setText("Stock alerts · " + alerts.size());
                Color accent = outOfStock > 0 ? UiTheme.DANGER : UiTheme.AMBER;
                Color surface = outOfStock > 0 ? new Color(254, 242, 242) : new Color(255, 247, 237);
                stockAlertButton.setIcon(new AppIcon(IconKind.ALERT, accent, 16));
                stockAlertButton.setBackground(surface);
                stockAlertButton.setForeground(accent);
                stockAlertButton.setToolTipText(alerts.size() + " product(s) at or below reorder level; "
                        + outOfStock + " out of stock. Click to review.");
            }
            if (!newlyTriggered.isEmpty()) {
                List<String> alertNames = newlyOutOfStock.isEmpty() ? newlyTriggered : newlyOutOfStock;
                String names = String.join(", ", alertNames.stream().limit(3).toList());
                if (alertNames.size() > 3) names += " and " + (alertNames.size() - 3) + " more";
                statusLabel.setText(newlyOutOfStock.isEmpty()
                        ? "Threshold warning: " + names + " reached reorder level."
                        : "Out-of-stock alert: " + names + " need immediate replenishment.");
            }
            stockAlertButton.repaint();
        }

        void showStockAlerts() {
            StockAlertDialog.show(this, service.getLowStockProducts());
        }

        void openStockReview() {
            if (currentRole == UserRole.INVENTORY_SELLER) {
                inventoryTabs.setSelectedComponent(stockPanel);
            } else if (currentRole == UserRole.SUPPLIER) {
                supplierTabs.setSelectedComponent(restockRequestsPanel);
            }
        }

        void perform(String activity, IoAction action) {
            try {
                action.run();
                customerData.fulfillPendingOrders(service);
                statusLabel.setText(activity + " | Saved");
                refreshAll();
            } catch (IllegalArgumentException exception) {
                showError(exception.getMessage());
            } catch (IOException exception) {
                showError("Could not save the operation. Review the CSV files in the data folder before continuing.\n\n"
                        + exception.getMessage());
            } catch (RuntimeException exception) {
                showError("The operation could not be completed: " + exception.getMessage());
            }
        }

        void showError(String message) {
            JOptionPane.showMessageDialog(this, message == null ? "An unexpected error occurred." : message,
                    "Check the operation", JOptionPane.ERROR_MESSAGE);
        }

        void showInfo(String message) {
            JOptionPane.showMessageDialog(this, message, "Inventory Manager", JOptionPane.INFORMATION_MESSAGE);
        }

        JButton primary(String text, ActionListener listener) {
            JButton button = new JButton(text);
            UiTheme.stylePrimary(button);
            button.addActionListener(listener);
            return button;
        }

        JButton secondary(String text, ActionListener listener) {
            JButton button = new JButton(text);
            UiTheme.styleSecondary(button);
            button.addActionListener(listener);
            return button;
        }

        JPanel panelWithToolbar(JPanel toolbar, JComponent content) {
            JPanel panel = new JPanel(new BorderLayout(0, 12));
            panel.setBackground(UiTheme.CANVAS);
            panel.setBorder(BorderFactory.createEmptyBorder(16, 4, 4, 4));
            toolbar.setOpaque(false);
            panel.add(toolbar, BorderLayout.NORTH);
            panel.add(content, BorderLayout.CENTER);
            return panel;
        }

        JPanel toolbar() {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            panel.setOpaque(false);
            return panel;
        }

        JTable styledTable(DefaultTableModel model) {
            JTable table = new JTable(model);
            table.setFillsViewportHeight(true);
            table.setRowHeight(34);
            table.setShowVerticalLines(false);
            table.setFont(new Font("SansSerif", Font.PLAIN, 13));
            table.setForeground(UiTheme.INK);
            table.setBackground(Color.WHITE);
            table.setSelectionBackground(UiTheme.SELECTION);
            table.setSelectionForeground(UiTheme.INK);
            table.setGridColor(UiTheme.BORDER);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoCreateRowSorter(true);
            table.getTableHeader().setReorderingAllowed(false);
            table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
            table.getTableHeader().setForeground(UiTheme.INK);
            table.getTableHeader().setBackground(UiTheme.TABLE_HEADER);
            table.getTableHeader().setPreferredSize(new Dimension(10, 38));
            table.setIntercellSpacing(new Dimension(0, 1));
            return table;
        }

        JComponent scroll(JTable table) {
            JScrollPane pane = new JScrollPane(table);
            pane.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
            pane.getViewport().setBackground(Color.WHITE);
            return pane;
        }

        DefaultTableModel readOnlyModel(Object[] columns) {
            return new DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
        }

        String currency(double amount) {
            return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).format(amount);
        }

        static double parseDouble(String value, String label) {
            try {
                double parsed = Double.parseDouble(value.trim());
                if (!Double.isFinite(parsed)) throw new NumberFormatException();
                return parsed;
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException(label + " must be a valid number.");
            }
        }

        static int parseInt(String value, String label) {
            try {
                return Integer.parseInt(value.trim());
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException(label + " must be a whole number.");
            }
        }

        JPanel formPanel() {
            JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
            form.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
            form.setBackground(Color.WHITE);
            return form;
        }

        void addFormRow(JPanel panel, String label, JComponent field) {
            if (field instanceof JTextField textField) UiTheme.styleInput(textField);
            JLabel text = new JLabel(label);
            text.setForeground(UiTheme.INK);
            panel.add(text);
            panel.add(field);
        }

        JPanel card(String label, JLabel value, Color marker) {
            JPanel card = new JPanel(new BorderLayout(0, 9));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, marker),
                    BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.BORDER),
                            BorderFactory.createEmptyBorder(15, 16, 15, 16))));
            JLabel title = new JLabel(label);
            title.setForeground(UiTheme.MUTED);
            title.setFont(new Font("SansSerif", Font.PLAIN, 12));
            value.setForeground(UiTheme.INK);
            value.setFont(new Font("SansSerif", Font.BOLD, 23));
            card.add(title, BorderLayout.NORTH);
            card.add(value, BorderLayout.CENTER);
            return card;
        }

        Product selectedProduct(JTable table, ProductTableModel model) {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) return null;
            return model.getProduct(table.convertRowIndexToModel(viewRow));
        }

        Supplier selectedSupplier(JTable table, DefaultTableModel model) {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) return null;
            String id = String.valueOf(model.getValueAt(table.convertRowIndexToModel(viewRow), 0));
            return service.findSupplier(id);
        }

        String nextProductId() {
            int candidate = 1001;
            while (service.findProduct("P" + candidate) != null) candidate++;
            return "P" + candidate;
        }

        String nextSupplierId() {
            int candidate = 101;
            while (service.findSupplier("SUP" + candidate) != null) candidate++;
            return "SUP" + candidate;
        }







        /** Category-themed product illustration drawn with Java2D; no image files or libraries required. */






        /** Bar chart comparing sales and purchases over the most recent seven calendar days. */


        /** Donut chart showing the number of SKUs in each stock-health state. */
        void openProductDialog(Product product) { InventoryDialogs.openProductDialog(this, product); }
        void openSupplierDialog(Supplier supplier) { InventoryDialogs.openSupplierDialog(this, supplier); }
        void returnToRoleSelection() {
            contentLayout.show(contentCards, "ROLE_SELECT");
            statusLabel.setText("Select a role to continue");
        }

    }
