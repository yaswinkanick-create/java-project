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

final class RoleSelectionPanel {
    private RoleSelectionPanel() { }

static JPanel build(MainFrame app) {
            JPanel page = new JPanel(new BorderLayout(0, 20));
            page.setBackground(UiTheme.CANVAS);
            page.setBorder(BorderFactory.createEmptyBorder(26, 26, 30, 26));

            JPanel heading = new JPanel(new GridLayout(0, 1, 0, 7));
            heading.setOpaque(false);
            JLabel title = new JLabel("Choose your workspace");
            title.setFont(new Font("SansSerif", Font.BOLD, 30));
            title.setForeground(UiTheme.INK);
            JLabel subtitle = new JLabel("Select your role. The next screen will ask for the credentials for that role.");
            subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
            subtitle.setForeground(UiTheme.MUTED);
            heading.add(title);
            heading.add(subtitle);
            page.add(heading, BorderLayout.NORTH);

            JPanel choices = new JPanel(new GridLayout(1, 3, 18, 0));
            choices.setOpaque(false);
            for (UserRole role : UserRole.values()) {
                Color roleAccent = role == UserRole.CUSTOMER ? UiTheme.ACCENT
                        : role == UserRole.INVENTORY_SELLER ? UiTheme.TEAL : UiTheme.AMBER;
                Color roleSurface = role == UserRole.CUSTOMER ? new Color(239, 246, 255)
                        : role == UserRole.INVENTORY_SELLER ? new Color(240, 253, 250)
                        : new Color(255, 247, 237);
                JPanel card = new JPanel(new BorderLayout(0, 16));
                card.setBackground(roleSurface);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(roleAccent.getRed(), roleAccent.getGreen(), roleAccent.getBlue(), 105), 1, true),
                        BorderFactory.createEmptyBorder(18, 18, 18, 18)));

                RoleArtwork artwork = new RoleArtwork(role);
                artwork.setPreferredSize(new Dimension(220, 154));
                card.add(artwork, BorderLayout.NORTH);

                JPanel copy = new JPanel(new GridLayout(0, 1, 0, 9));
                copy.setOpaque(false);
                JLabel roleTitle = new JLabel(role.title);
                roleTitle.setFont(new Font("SansSerif", Font.BOLD, 21));
                roleTitle.setForeground(UiTheme.INK);
                JLabel description = new JLabel("<html><div style='width:220px;line-height:1.5'>" + role.description + "</div></html>");
                description.setFont(new Font("SansSerif", Font.PLAIN, 14));
                description.setForeground(UiTheme.MUTED);
                copy.add(roleTitle);
                copy.add(description);

                JButton select = new JButton("Continue as " + role.title);
                UiTheme.styleRoleButton(select, role);
                select.addActionListener(event -> app.showLogin(role));
                select.setIcon(new AppIcon(role == UserRole.CUSTOMER ? IconKind.CUSTOMER
                        : role == UserRole.INVENTORY_SELLER ? IconKind.STOCK : IconKind.SUPPLIERS,
                        Color.WHITE, 16));
                select.setIconTextGap(8);
                select.setPreferredSize(new Dimension(200, 48));
                card.add(copy, BorderLayout.CENTER);
                card.add(select, BorderLayout.SOUTH);
                choices.add(card);
            }
            page.add(choices, BorderLayout.CENTER);
            JLabel note = new JLabel("Customer, Inventory / Seller, and Supplier workspaces are separated by role.");
            note.setFont(new Font("SansSerif", Font.PLAIN, 13));
            note.setForeground(UiTheme.MUTED);
            page.add(note, BorderLayout.SOUTH);
            return page;
        }

private static final class RoleArtwork extends JComponent {
            private final UserRole role;
            private final Color tint;
            private final Color accent;

            private RoleArtwork(UserRole role) {
                this.role = role;
                if (role == UserRole.CUSTOMER) {
                    tint = new Color(224, 236, 255);
                    accent = UiTheme.ACCENT;
                } else if (role == UserRole.INVENTORY_SELLER) {
                    tint = new Color(221, 245, 239);
                    accent = UiTheme.TEAL;
                } else {
                    tint = new Color(255, 239, 220);
                    accent = UiTheme.AMBER;
                }
                setOpaque(false);
                setToolTipText(role.title + " illustration");
            }

            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g.setColor(tint);
                g.fillRoundRect(0, 0, w, h, 20, 20);
                g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
                g.fillOval(w - 108, -25, 128, 128);
                g.fillOval(12, h - 58, 74, 74);
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(accent);
                int cx = w / 2, cy = h / 2 + 2;
                if (role == UserRole.CUSTOMER) {
                    g.fillOval(cx - 17, cy - 34, 34, 34);
                    g.fillRoundRect(cx - 34, cy + 2, 68, 43, 22, 22);
                    g.setColor(Color.WHITE);
                    g.drawRoundRect(cx + 21, cy + 12, 34, 29, 5, 5);
                    g.drawArc(cx + 27, cy + 3, 22, 22, 0, 180);
                    g.setColor(accent);
                    g.drawLine(cx + 32, cy + 22, cx + 45, cy + 22);
                } else if (role == UserRole.INVENTORY_SELLER) {
                    int x = cx - 43, y = cy - 26;
                    Polygon front = new Polygon(new int[]{x, x + 42, x + 42, x}, new int[]{y, y + 15, y + 55, y + 40}, 4);
                    Polygon side = new Polygon(new int[]{x + 42, x + 78, x + 78, x + 42}, new int[]{y + 15, y, y + 40, y + 55}, 4);
                    Polygon top = new Polygon(new int[]{x, x + 36, x + 78, x + 42}, new int[]{y, y - 16, y, y + 15}, 4);
                    g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 45)); g.fillPolygon(front); g.fillPolygon(side); g.fillPolygon(top);
                    g.setColor(accent); g.drawPolygon(front); g.drawPolygon(side); g.drawPolygon(top);
                    g.drawLine(x + 42, y + 15, x + 42, y + 55);
                    g.drawLine(x + 18, y + 7, x + 18, y + 46);
                } else {
                    int x = cx - 55, y = cy - 13;
                    g.drawRoundRect(x, y + 9, 66, 34, 6, 6);
                    g.drawRoundRect(x + 66, y + 20, 43, 23, 5, 5);
                    g.drawLine(x + 77, y + 20, x + 87, y + 7);
                    g.drawLine(x + 87, y + 7, x + 103, y + 7);
                    g.setColor(tint); g.fillRoundRect(x + 73, y + 23, 29, 12, 3, 3);
                    g.setColor(accent); g.drawRoundRect(x + 73, y + 23, 29, 12, 3, 3);
                    g.fillOval(x + 12, y + 35, 20, 20); g.fillOval(x + 75, y + 35, 20, 20);
                    g.setColor(Color.WHITE); g.fillOval(x + 18, y + 41, 8, 8); g.fillOval(x + 81, y + 41, 8, 8);
                    g.setColor(accent); g.drawRoundRect(x - 4, y - 11, 29, 19, 4, 4); g.drawRoundRect(x + 29, y - 11, 29, 19, 4, 4);
                }
                g.dispose();
            }
        }
}
