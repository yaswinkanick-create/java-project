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

final class SalesTrendChart extends JPanel {
            private final double[] sales = new double[7];
            private final double[] purchases = new double[7];
            private final String[] labels = new String[7];
            SalesTrendChart() {
                setOpaque(true); setBackground(Color.WHITE);
                setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.BORDER, 1, true),
                        BorderFactory.createEmptyBorder(10, 12, 8, 12)));
                setPreferredSize(new Dimension(420, 250));
                updateData(List.of());
            }
            void updateData(List<TransactionRecord> transactions) {
                Arrays.fill(sales, 0); Arrays.fill(purchases, 0);
                LocalDate today = LocalDate.now();
                for (int i = 0; i < 7; i++) labels[i] = today.minusDays(6 - i).format(DateTimeFormatter.ofPattern("dd MMM"));
                for (TransactionRecord record : transactions) {
                    if (!record.getType().equals("SALE") && !record.getType().equals("PURCHASE")) continue;
                    try {
                        LocalDate date = LocalDate.parse(record.getDateTime().substring(0, 10));
                        long index = java.time.temporal.ChronoUnit.DAYS.between(today.minusDays(6), date);
                        if (index < 0 || index > 6) continue;
                        if (record.getType().equals("SALE")) sales[(int) index] += record.getTotal();
                        else purchases[(int) index] += record.getTotal();
                    } catch (RuntimeException ignored) { /* Skip legacy rows with an unrecognized date. */ }
                }
                repaint();
            }
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g.setColor(UiTheme.INK); g.setFont(new Font("SansSerif", Font.BOLD, 14));
                g.drawString("Sales vs purchases", 14, 24);
                g.setFont(new Font("SansSerif", Font.PLAIN, 12)); g.setColor(UiTheme.MUTED);
                g.drawString("Last 7 days", 14, 42);
                g.setColor(UiTheme.ACCENT); g.fillRoundRect(w - 154, 15, 10, 10, 3, 3);
                g.setColor(UiTheme.MUTED); g.drawString("Sales", w - 139, 24);
                g.setColor(new Color(13, 148, 136)); g.fillRoundRect(w - 88, 15, 10, 10, 3, 3);
                g.setColor(UiTheme.MUTED); g.drawString("Purchases", w - 73, 24);
                int left = 58, right = 14, top = 58, bottom = 29;
                int plotW = Math.max(1, w - left - right), plotH = Math.max(1, h - top - bottom);
                if (Arrays.stream(sales).sum() == 0 && Arrays.stream(purchases).sum() == 0) {
                    g.setColor(new Color(248, 250, 252));
                    g.fillRoundRect(left, top, plotW, plotH, 10, 10);
                    g.setColor(UiTheme.MUTED);
                    g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    String message = "No sales or purchases in the last 7 days";
                    g.drawString(message, left + Math.max(8, (plotW - g.getFontMetrics().stringWidth(message)) / 2),
                            top + plotH / 2);
                    g.dispose();
                    return;
                }
                double max = 1;
                for (int i = 0; i < 7; i++) max = Math.max(max, Math.max(sales[i], purchases[i]));
                for (int grid = 0; grid <= 4; grid++) {
                    int y = top + plotH - grid * plotH / 4;
                    g.setColor(new Color(226, 232, 240)); g.drawLine(left, y, left + plotW, y);
                    g.setColor(UiTheme.MUTED); g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    g.drawString(String.format(Locale.ROOT, "%.0f", max * grid / 4), 2, y + 3);
                }
                int groupW = Math.max(8, plotW / 7), barW = Math.max(3, groupW / 3);
                for (int i = 0; i < 7; i++) {
                    int x = left + i * groupW + Math.max(1, (groupW - barW * 2 - 3) / 2);
                    int sh = (int) Math.round(sales[i] / max * (plotH - 2));
                    int ph = (int) Math.round(purchases[i] / max * (plotH - 2));
                    g.setColor(UiTheme.ACCENT); g.fillRoundRect(x, top + plotH - sh, barW, sh, 4, 4);
                    g.setColor(new Color(13, 148, 136)); g.fillRoundRect(x + barW + 3, top + plotH - ph, barW, ph, 4, 4);
                    g.setColor(UiTheme.MUTED); g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    String label = labels[i];
                    int labelX = left + i * groupW + Math.max(0, (groupW - g.getFontMetrics().stringWidth(label)) / 2);
                    g.drawString(label, labelX, h - 9);
                }
                g.dispose();
            }
        }
