import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Compact dashboard warning that refreshes whenever inventory data changes. */
final class StockAlertBanner extends JPanel {
    private final JLabel icon = new JLabel();
    private final JLabel title = new JLabel();
    private final JLabel detail = new JLabel();

    StockAlertBanner(MainFrame app) {
        super(new BorderLayout(14, 0));
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel message = new JPanel(new BorderLayout(10, 0));
        message.setOpaque(false);
        icon.setIcon(new AppIcon(IconKind.STOCK, UiTheme.SUCCESS, 21));
        JPanel copy = new JPanel(new BorderLayout(0, 3));
        copy.setOpaque(false);
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        detail.setFont(new Font("SansSerif", Font.PLAIN, 12));
        copy.add(title, BorderLayout.NORTH);
        copy.add(detail, BorderLayout.CENTER);
        message.add(icon, BorderLayout.WEST);
        message.add(copy, BorderLayout.CENTER);
        add(message, BorderLayout.CENTER);

        JButton review = new JButton("Review alerts");
        UiTheme.styleSecondary(review);
        review.setMargin(new Insets(7, 12, 7, 12));
        review.addActionListener(event -> app.showStockAlerts());
        add(review, BorderLayout.EAST);

        refresh(List.of());
    }

    void refresh(List<Product> alerts) {
        int outOfStock = (int) alerts.stream().filter(product -> product.getQuantity() == 0).count();
        if (alerts.isEmpty()) {
            setBackground(new Color(236, 253, 245));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(187, 247, 208), 1, true),
                    BorderFactory.createEmptyBorder(9, 13, 9, 13)));
            icon.setIcon(new AppIcon(IconKind.STOCK, UiTheme.SUCCESS, 21));
            title.setForeground(UiTheme.SUCCESS);
            title.setText("Stock health is clear");
            detail.setForeground(new Color(55, 85, 65));
            detail.setText("All products are above their configured reorder thresholds.");
        } else {
            boolean critical = outOfStock > 0;
            Color accent = critical ? UiTheme.DANGER : UiTheme.AMBER;
            Color surface = critical ? new Color(254, 242, 242) : new Color(255, 247, 237);
            Color border = critical ? new Color(254, 202, 202) : new Color(253, 230, 138);
            setBackground(surface);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(border, 1, true),
                    BorderFactory.createEmptyBorder(9, 13, 9, 13)));
            icon.setIcon(new AppIcon(IconKind.ALERT, accent, 21));
            title.setForeground(accent);
            title.setText(critical ? "Stock threshold warning · Immediate attention"
                    : "Stock threshold warning · Reorder point reached");
            detail.setForeground(UiTheme.INK);
            detail.setText(alerts.size() + " product(s) at or below reorder level · "
                    + outOfStock + " out of stock · Updates after sales, deliveries, returns, stock edits and CSV imports.");
        }
        repaint();
    }
}
