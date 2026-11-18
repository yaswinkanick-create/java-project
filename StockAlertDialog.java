import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/** Detailed stock-threshold monitor opened from the header or dashboard banner. */
final class StockAlertDialog {
    private StockAlertDialog() { }

    static void show(MainFrame owner, List<Product> alerts) {
        JDialog dialog = new JDialog(owner, "Stock threshold monitor", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(0, 14));

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(UiTheme.CANVAS);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));

        JPanel heading = new JPanel(new BorderLayout(0, 6));
        heading.setBackground(UiTheme.SURFACE);
        heading.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(15, 17, 15, 17)));
        JLabel eyebrow = new JLabel("INVENTORY HEALTH");
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 11));
        eyebrow.setForeground(UiTheme.MUTED);
        JLabel title = new JLabel("Stock threshold monitor");
        title.setFont(new Font("SansSerif", Font.BOLD, 21));
        title.setForeground(UiTheme.INK);
        int outOfStock = (int) alerts.stream().filter(product -> product.getQuantity() == 0).count();
        JLabel description = new JLabel(alerts.isEmpty()
                ? "All products are currently above their reorder thresholds."
                : alerts.size() + " product(s) need attention · " + outOfStock + " currently out of stock.");
        description.setFont(new Font("SansSerif", Font.PLAIN, 13));
        description.setForeground(alerts.isEmpty() ? UiTheme.SUCCESS
                : outOfStock > 0 ? UiTheme.DANGER : UiTheme.AMBER);
        JPanel headingCopy = new JPanel(new BorderLayout(0, 5));
        headingCopy.setOpaque(false);
        headingCopy.add(eyebrow, BorderLayout.NORTH);
        JPanel titleAndDescription = new JPanel(new BorderLayout(0, 5));
        titleAndDescription.setOpaque(false);
        titleAndDescription.add(title, BorderLayout.NORTH);
        titleAndDescription.add(description, BorderLayout.CENTER);
        headingCopy.add(titleAndDescription, BorderLayout.CENTER);
        heading.add(headingCopy, BorderLayout.CENTER);
        root.add(heading, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"SKU", "Product", "Category", "Available", "Reorder level", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (Product product : alerts) {
            String status = product.getQuantity() == 0 ? "OUT OF STOCK" : "LOW STOCK";
            model.addRow(new Object[]{product.getId(), product.getName(), product.getCategory(),
                    product.getQuantity(), product.getReorderLevel(), status});
        }
        JTable table = owner.styledTable(model);
        table.setRowHeight(38);
        table.getColumnModel().getColumn(5).setCellRenderer(new StatusRenderer());
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(UiTheme.SURFACE);
        tablePanel.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER, 1, true));
        tablePanel.add(owner.scroll(table), BorderLayout.CENTER);
        root.add(tablePanel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        JLabel rule = new JLabel("Trigger rule: available quantity ≤ configured reorder level.");
        rule.setForeground(UiTheme.MUTED);
        rule.setFont(new Font("SansSerif", Font.PLAIN, 12));
        footer.add(rule, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton review = owner.primary("Open stock review", event -> {
            dialog.dispose();
            owner.openStockReview();
        });
        JButton close = owner.secondary("Close", event -> dialog.dispose());
        actions.add(review);
        actions.add(close);
        footer.add(actions, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        dialog.add(root, BorderLayout.CENTER);
        dialog.setSize(900, 520);
        dialog.setMinimumSize(new java.awt.Dimension(760, 430));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static final class StatusRenderer extends DefaultTableCellRenderer {
        StatusRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("SansSerif", Font.BOLD, 11));
            setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (!selected) {
                boolean out = "OUT OF STOCK".equals(value);
                setForeground(out ? UiTheme.DANGER : UiTheme.AMBER);
                setBackground(out ? new Color(254, 242, 242) : new Color(255, 247, 237));
            }
            return this;
        }
    }
}
