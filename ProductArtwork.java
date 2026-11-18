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

final class ProductArtwork extends JComponent {
            private final String category;
            private final String name;
            private final Color accent;
            ProductArtwork(String category, String name) {
                this.category = category == null ? "Product" : category;
                this.name = name == null ? "Product" : name;
                Color[] palette = {new Color(37, 99, 235), new Color(13, 148, 136), new Color(124, 58, 237), new Color(217, 119, 6)};
                this.accent = palette[Math.floorMod((this.category + this.name).hashCode(), palette.length)];
                setOpaque(false);
                setToolTipText(this.name + " product illustration");
            }
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 25));
                g.fillRoundRect(0, 0, w, h, 13, 13);
                g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(accent);
                int cx = w / 2, cy = h / 2;
                String text = (category + " " + name).toLowerCase(Locale.ROOT);
                if (text.contains("folder")) {
                    g.fillRoundRect(cx - 28, cy - 15, 56, 40, 5, 5);
                    g.fillRoundRect(cx - 22, cy - 22, 27, 12, 4, 4);
                    g.setColor(Color.WHITE); g.drawLine(cx - 17, cy - 4, cx + 17, cy - 4);
                } else if (text.contains("notebook") || text.contains("book") || text.contains("paper")) {
                    g.fillRoundRect(cx - 23, cy - 29, 46, 58, 4, 4);
                    g.setColor(Color.WHITE); g.drawLine(cx - 12, cy - 13, cx + 12, cy - 13);
                    g.drawLine(cx - 12, cy - 3, cx + 12, cy - 3); g.drawLine(cx - 12, cy + 7, cx + 8, cy + 7);
                    g.drawLine(cx - 14, cy - 25, cx - 14, cy + 25);
                } else if (text.contains("pen") || text.contains("marker") || text.contains("pencil")) {
                    g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(cx - 18, cy + 22, cx + 18, cy - 22);
                    g.setStroke(new BasicStroke(2.3f));
                    g.setColor(Color.WHITE); g.drawLine(cx - 9, cy + 12, cx + 9, cy - 10);
                    g.setColor(accent); g.drawLine(cx - 23, cy + 27, cx - 16, cy + 20);
                } else {
                    Polygon front = new Polygon(new int[]{cx - 25, cx + 1, cx + 1, cx - 25}, new int[]{cy - 9, cy, cy + 28, cy + 19}, 4);
                    Polygon side = new Polygon(new int[]{cx + 1, cx + 25, cx + 25, cx + 1}, new int[]{cy, cy - 9, cy + 19, cy + 28}, 4);
                    Polygon top = new Polygon(new int[]{cx - 25, cx, cx + 25, cx + 1}, new int[]{cy - 9, cy - 22, cy - 9, cy}, 4);
                    g.fillPolygon(front); g.fillPolygon(side); g.fillPolygon(top);
                    g.setColor(Color.WHITE); g.drawLine(cx + 1, cy, cx + 1, cy + 28);
                }
                g.dispose();
            }
        }
