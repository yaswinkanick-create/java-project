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

final class AppIcon implements Icon {
        private final IconKind kind;
        private final Color color;
        private final int size;

        AppIcon(IconKind kind, Color color, int size) {
            this.kind = kind;
            this.color = color;
            this.size = size;
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.setColor(color);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            switch (kind) {
                case BRAND, STOCK -> {
                    g.drawRoundRect(2, 5, 14, 10, 2, 2);
                    g.drawLine(2, 9, 16, 9);
                    g.drawLine(9, 5, 9, 15);
                    g.drawLine(5, 5, 9, 2);
                    g.drawLine(9, 2, 13, 5);
                }
                case OVERVIEW -> {
                    g.drawRoundRect(2, 2, 6, 6, 2, 2);
                    g.drawRoundRect(10, 2, 6, 6, 2, 2);
                    g.drawRoundRect(2, 10, 6, 6, 2, 2);
                    g.drawRoundRect(10, 10, 6, 6, 2, 2);
                }
                case PRODUCTS -> {
                    g.drawLine(9, 2, 16, 6);
                    g.drawLine(16, 6, 9, 10);
                    g.drawLine(9, 10, 2, 6);
                    g.drawLine(2, 6, 9, 2);
                    g.drawLine(2, 6, 2, 14);
                    g.drawLine(2, 14, 9, 18);
                    g.drawLine(9, 18, 16, 14);
                    g.drawLine(16, 14, 16, 6);
                    g.drawLine(9, 10, 9, 18);
                }
                case SALES -> {
                    g.drawLine(2, 3, 5, 3);
                    g.drawLine(5, 3, 7, 13);
                    g.drawLine(7, 13, 15, 13);
                    g.drawLine(15, 13, 17, 6);
                    g.drawLine(6, 6, 17, 6);
                    g.drawOval(7, 15, 2, 2);
                    g.drawOval(14, 15, 2, 2);
                }
                case RETURNS -> {
                    g.drawArc(3, 3, 12, 12, 35, 285);
                    g.drawLine(3, 4, 3, 9);
                    g.drawLine(3, 4, 8, 4);
                    g.drawLine(3, 4, 7, 1);
                }
                case REPORTS -> {
                    g.drawLine(3, 2, 3, 16);
                    g.drawLine(3, 16, 17, 16);
                    g.fillRoundRect(6, 10, 3, 5, 1, 1);
                    g.fillRoundRect(11, 6, 3, 9, 1, 1);
                    g.fillRoundRect(16, 3, 2, 12, 1, 1);
                }
                case SUPPLIERS -> {
                    g.drawOval(3, 2, 5, 5);
                    g.drawOval(11, 4, 4, 4);
                    g.drawArc(1, 8, 10, 9, 0, 180);
                    g.drawArc(9, 10, 9, 7, 0, 180);
                }
                case PURCHASING, ORDERS -> {
                    g.drawRoundRect(4, 2, 11, 15, 2, 2);
                    g.drawLine(7, 6, 12, 6);
                    g.drawLine(7, 9, 13, 9);
                    g.drawLine(7, 12, 11, 12);
                    if (kind == IconKind.PURCHASING) {
                        g.drawLine(2, 5, 2, 12);
                        g.drawLine(2, 12, 5, 12);
                    }
                }
                case RESTOCK -> {
                    g.drawRoundRect(2, 5, 9, 8, 1, 1);
                    g.drawLine(11, 8, 15, 8);
                    g.drawLine(15, 8, 17, 11);
                    g.drawLine(17, 11, 17, 13);
                    g.drawLine(2, 13, 17, 13);
                    g.drawOval(4, 13, 3, 3);
                    g.drawOval(13, 13, 3, 3);
                }
                case CUSTOMER -> {
                    g.drawOval(6, 2, 7, 7);
                    g.drawArc(2, 9, 15, 9, 0, 180);
                }
                case EXIT -> {
                    g.drawRoundRect(2, 2, 9, 14, 2, 2);
                    g.drawLine(8, 9, 17, 9);
                    g.drawLine(13, 5, 17, 9);
                    g.drawLine(13, 13, 17, 9);
                }
                case ALERT -> {
                    int[] xs = {10, 18, 2};
                    int[] ys = {2, 17, 17};
                    g.drawPolygon(xs, ys, 3);
                    g.drawLine(10, 7, 10, 12);
                    g.fillOval(9, 14, 2, 2);
                }
            }
            g.dispose();
        }
    }
