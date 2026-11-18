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

final class StockStatusPieChart extends JPanel {
            private int healthy, low, out;
            StockStatusPieChart() {
                setOpaque(true); setBackground(Color.WHITE);
                setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.BORDER, 1, true),
                        BorderFactory.createEmptyBorder(10, 12, 8, 12)));
                setPreferredSize(new Dimension(360, 250));
            }
            void updateData(List<Product> products) {
                healthy = 0; low = 0; out = 0;
                for (Product product : products) {
                    if (product.getQuantity() <= 0) out++;
                    else if (product.isLowStock()) low++;
                    else healthy++;
                }
                repaint();
            }
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g.setColor(UiTheme.INK); g.setFont(new Font("SansSerif", Font.BOLD, 14)); g.drawString("Stock health", 14, 24);
                g.setColor(UiTheme.MUTED); g.setFont(new Font("SansSerif", Font.PLAIN, 12)); g.drawString("Product count by availability", 14, 42);
                int total = healthy + low + out;
                int size = Math.min(130, Math.max(82, h - 76));
                int x = 22, y = 55 + Math.max(0, (h - 70 - size) / 2);
                if (total == 0) {
                    g.setColor(new Color(226, 232, 240)); g.fillOval(x, y, size, size);
                    g.setColor(Color.WHITE); g.fillOval(x + 28, y + 28, size - 56, size - 56);
                    g.setColor(UiTheme.MUTED); g.drawString("No products", x + 22, y + size / 2 + 4);
                } else {
                    Color[] colors = {new Color(22, 163, 74), new Color(217, 119, 6), new Color(220, 38, 38)};
                    int[] values = {healthy, low, out};
                    int startAngle = 90;
                    for (int i = 0; i < values.length; i++) {
                        if (values[i] == 0) continue;
                        int extent = (int) Math.round(values[i] * 360.0 / total);
                        g.setColor(colors[i]); g.fillArc(x, y, size, size, startAngle, -extent); startAngle -= extent;
                    }
                    g.setColor(Color.WHITE); g.fillOval(x + size / 3, y + size / 3, size / 3, size / 3);
                    g.setColor(UiTheme.INK); g.setFont(new Font("SansSerif", Font.BOLD, 16));
                    String count = Integer.toString(total); g.drawString(count, x + size / 2 - g.getFontMetrics().stringWidth(count) / 2, y + size / 2 + 5);
                }
                int legendX = x + size + 22, legendY = y + 15;
                String[] names = {"Healthy stock", "Low stock", "Out of stock"};
                int[] counts = {healthy, low, out};
                Color[] colors = {new Color(22, 163, 74), new Color(217, 119, 6), new Color(220, 38, 38)};
                for (int i = 0; i < names.length; i++) {
                    g.setColor(colors[i]); g.fillRoundRect(legendX, legendY + i * 33, 11, 11, 3, 3);
                    g.setColor(UiTheme.INK); g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    g.drawString(names[i], legendX + 18, legendY + 9 + i * 33);
                    g.setColor(UiTheme.MUTED); g.drawString(Integer.toString(counts[i]), legendX + 18, legendY + 23 + i * 33);
                }
                g.dispose();
            }
        }
