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

final class UiTheme {
        // Neutral surfaces, strong slate typography, and a restrained blue/teal accent system.
        public static final Color INK = new Color(23, 43, 77);          // #172B4D
        public static final Color MUTED = new Color(100, 116, 139);     // #64748B
        public static final Color ACCENT = new Color(37, 99, 235);      // #2563EB
        public static final Color ACCENT_HOVER = new Color(29, 78, 216);// #1D4ED8
        public static final Color CANVAS = new Color(244, 247, 251);    // #F4F7FB
        public static final Color SURFACE = new Color(255, 255, 255);   // #FFFFFF
        public static final Color BORDER = new Color(226, 232, 240);    // #E2E8F0
        public static final Color TABLE_HEADER = new Color(241, 245, 249); // #F1F5F9
        public static final Color SELECTION = new Color(219, 234, 254); // #DBEAFE
        public static final Color HEADER = new Color(20, 33, 61);       // #14213D
        public static final Color TEAL = new Color(15, 118, 110);       // #0F766E
        public static final Color AMBER = new Color(180, 83, 9);        // #B45309
        public static final Color PURPLE = new Color(109, 40, 217);     // #6D28D9
        public static final Color SUCCESS = new Color(21, 128, 61);    // #15803D
        public static final Color DANGER = new Color(185, 28, 28);      // #B91C1C

        private UiTheme() { }

        public static void apply() {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {
                // Keep the JDK's default look and feel as a safe fallback.
            }

            UIManager.put("control", SURFACE);
            UIManager.put("text", INK);
            UIManager.put("nimbusBase", ACCENT);
            UIManager.put("nimbusBlueGrey", TABLE_HEADER);
            UIManager.put("nimbusFocus", ACCENT);
            UIManager.put("nimbusDisabledText", MUTED);
            UIManager.put("nimbusSelectionBackground", SELECTION);

            Font body = new Font("SansSerif", Font.PLAIN, 13);
            Font heading = new Font("SansSerif", Font.BOLD, 13);
            UIManager.put("Label.font", body);
            UIManager.put("Button.font", heading);
            UIManager.put("TextField.font", body);
            UIManager.put("PasswordField.font", body);
            UIManager.put("TextArea.font", body);
            UIManager.put("ComboBox.font", body);
            UIManager.put("CheckBox.font", body);
            UIManager.put("RadioButton.font", body);
            UIManager.put("List.font", body);
            UIManager.put("ToolTip.font", new Font("SansSerif", Font.PLAIN, 12));
            UIManager.put("Table.font", body);
            UIManager.put("Table.rowHeight", 35);
            UIManager.put("TableHeader.font", heading);
            UIManager.put("TabbedPane.font", heading);

            UIManager.put("Label.foreground", INK);
            UIManager.put("Panel.background", CANVAS);
            UIManager.put("TextField.background", SURFACE);
            UIManager.put("TextField.foreground", INK);
            UIManager.put("TextField.caretForeground", ACCENT);
            UIManager.put("TextField.selectionBackground", SELECTION);
            UIManager.put("TextField.selectionForeground", INK);
            UIManager.put("PasswordField.background", SURFACE);
            UIManager.put("PasswordField.foreground", INK);
            UIManager.put("PasswordField.caretForeground", ACCENT);
            UIManager.put("PasswordField.selectionBackground", SELECTION);
            UIManager.put("PasswordField.selectionForeground", INK);
            UIManager.put("TextArea.background", SURFACE);
            UIManager.put("TextArea.foreground", INK);
            UIManager.put("TextArea.caretForeground", ACCENT);
            UIManager.put("ComboBox.background", SURFACE);
            UIManager.put("ComboBox.foreground", INK);
            UIManager.put("Table.background", SURFACE);
            UIManager.put("Table.foreground", INK);
            UIManager.put("Table.selectionBackground", SELECTION);
            UIManager.put("Table.selectionForeground", INK);
            UIManager.put("TableHeader.background", TABLE_HEADER);
            UIManager.put("TableHeader.foreground", INK);
            UIManager.put("TabbedPane.background", CANVAS);
            UIManager.put("TabbedPane.foreground", INK);
            UIManager.put("TabbedPane.selected", SURFACE);
            UIManager.put("TabbedPane.contentAreaColor", SURFACE);
            UIManager.put("CheckBox.foreground", INK);
            UIManager.put("RadioButton.foreground", INK);
            UIManager.put("OptionPane.messageForeground", INK);
            UIManager.put("ToolTip.background", new Color(255, 255, 255));
            UIManager.put("ToolTip.foreground", INK);
            // Keep text-field insets compact. Large margins combined with padded borders
            // can leave too little vertical room for the font and clip typed text.
            UIManager.put("TextField.margin", new Insets(2, 8, 2, 8));
            UIManager.put("PasswordField.margin", new Insets(2, 8, 2, 8));
        }

        /** Applies one balanced border + margin so input glyphs are never clipped. */
        public static void styleInput(JTextField field) {
            field.setFont(new Font("SansSerif", Font.PLAIN, 15));
            field.setForeground(INK);
            field.setBackground(SURFACE);
            field.setCaretColor(ACCENT);
            field.setSelectionColor(SELECTION);
            field.setSelectedTextColor(INK);
            field.setMargin(new Insets(1, 10, 1, 10));
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1, true),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            Dimension preferred = field.getPreferredSize();
            field.setPreferredSize(new Dimension(Math.max(190, preferred.width),
                    Math.max(42, preferred.height)));
            field.setMinimumSize(new Dimension(90, 42));
        }

        public static void stylePrimary(JButton button) {
            styleButton(button, ACCENT, ACCENT_HOVER, new Color(30, 64, 175), Color.WHITE,
                    TABLE_HEADER, ACCENT);
        }

        /** Applies a distinct but coordinated accent to each role selection button. */
        public static void styleRoleButton(JButton button, UserRole role) {
            if (role == UserRole.CUSTOMER) {
                styleButton(button, ACCENT, ACCENT_HOVER, new Color(30, 64, 175),
                        Color.WHITE, TABLE_HEADER, ACCENT);
            } else if (role == UserRole.INVENTORY_SELLER) {
                styleButton(button, TEAL, new Color(17, 94, 89), new Color(19, 78, 74),
                        Color.WHITE, TABLE_HEADER, TEAL);
            } else {
                styleButton(button, AMBER, new Color(146, 64, 14), new Color(120, 53, 15),
                        Color.WHITE, TABLE_HEADER, AMBER);
            }
        }

        public static void styleSecondary(JButton button) {
            styleButton(button, SURFACE, TABLE_HEADER, new Color(226, 232, 240),
                    INK, TABLE_HEADER, BORDER);
        }

        private static void styleButton(JButton button, Color base, Color hover, Color pressed,
                                        Color foreground, Color disabledBackground, Color normalBorder) {
            button.setUI(new BasicButtonUI());
            button.setBackground(base);
            button.setForeground(foreground);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setBorderPainted(true);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(normalBorder, 1, true),
                    BorderFactory.createEmptyBorder(8, 14, 8, 14)));
            button.setFocusPainted(true);
            button.setRolloverEnabled(true);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.getAccessibleContext().setAccessibleName(button.getText());

            javax.swing.border.Border normal = button.getBorder();
            javax.swing.border.Border focused = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT, 2, true),
                    BorderFactory.createEmptyBorder(7, 13, 7, 13));
            button.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent event) {
                    if (button.isEnabled() && !button.getModel().isPressed()) button.setBackground(hover);
                }
                @Override public void mouseExited(MouseEvent event) {
                    if (button.isEnabled()) button.setBackground(base);
                }
                @Override public void mousePressed(MouseEvent event) {
                    if (button.isEnabled()) button.setBackground(pressed);
                }
                @Override public void mouseReleased(MouseEvent event) {
                    if (button.isEnabled()) button.setBackground(button.contains(event.getPoint()) ? hover : base);
                }
            });
            button.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent event) { button.setBorder(focused); }
                @Override public void focusLost(FocusEvent event) { button.setBorder(normal); }
            });
            button.addPropertyChangeListener("enabled", event -> {
                if (!button.isEnabled()) {
                    button.setBackground(disabledBackground);
                    button.setForeground(MUTED);
                    button.setCursor(Cursor.getDefaultCursor());
                } else {
                    button.setBackground(base);
                    button.setForeground(foreground);
                    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            });
        }
    }
