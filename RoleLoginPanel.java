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

final class RoleLoginPanel extends AppPanel {
            private final UserRole role;
            private final JTextField nameField = new JTextField(22);
            private final JTextField usernameField = new JTextField(22);
            private final JPasswordField passwordField = new JPasswordField(22);
            private final JLabel nameLabel = new JLabel("Full name");
            private final JLabel usernameLabel;
            private final JPanel form = new JPanel(new GridLayout(0, 2, 14, 10));
            private final JButton submitButton;
            private final JButton toggleButton;
            private boolean registering;

            RoleLoginPanel(MainFrame app, UserRole role) {
            super(app);
                this.role = role;
                setLayout(new GridBagLayout());
                setBackground(UiTheme.CANVAS);
                setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

                JPanel card = new JPanel(new BorderLayout(0, 12));
                card.setBackground(Color.WHITE);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UiTheme.BORDER, 1, true),
                        BorderFactory.createEmptyBorder(20, 28, 20, 28)));
                card.setPreferredSize(new Dimension(690, 510));

                Color roleAccent = switch (role) {
                    case CUSTOMER -> UiTheme.ACCENT;
                    case INVENTORY_SELLER -> UiTheme.TEAL;
                    case SUPPLIER -> UiTheme.AMBER;
                };

                JPanel titlePanel = new JPanel(new GridLayout(0, 1, 0, 5));
                titlePanel.setOpaque(true);
                titlePanel.setBackground(roleAccent);
                titlePanel.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
                JLabel eyebrow = new JLabel("SECURE WORKSPACE ACCESS");
                eyebrow.setFont(new Font("SansSerif", Font.BOLD, 12));
                eyebrow.setForeground(Color.WHITE);
                JLabel title = new JLabel(role.title + " sign in");
                title.setFont(new Font("SansSerif", Font.BOLD, 28));
                title.setForeground(Color.WHITE);
                JLabel subtitle = new JLabel("Enter your " + (role == UserRole.CUSTOMER ? "customer account" : role.title + " account") + " credentials.");
                subtitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
                subtitle.setForeground(Color.WHITE);
                titlePanel.add(eyebrow);
                titlePanel.add(title);
                titlePanel.add(subtitle);
                card.add(titlePanel, BorderLayout.NORTH);

                form.setOpaque(false);
                form.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
                Font fieldFont = new Font("SansSerif", Font.PLAIN, 16);
                Font labelFont = new Font("SansSerif", Font.BOLD, 15);
                UiTheme.styleInput(nameField);
                UiTheme.styleInput(usernameField);
                UiTheme.styleInput(passwordField);
                nameField.setFont(fieldFont);
                usernameField.setFont(fieldFont);
                passwordField.setFont(fieldFont);
                for (JTextField field : new JTextField[]{nameField, usernameField, passwordField}) {
                    field.setPreferredSize(new Dimension(310, 44));
                    field.setMinimumSize(new Dimension(220, 44));
                }
                usernameLabel = new JLabel(role == UserRole.CUSTOMER ? "Email address" : "Username");
                usernameLabel.setFont(labelFont);
                JLabel passwordLabel = new JLabel("Password");
                passwordLabel.setFont(labelFont);
                nameLabel.setFont(labelFont);
                form.add(usernameLabel); form.add(usernameField);
                form.add(passwordLabel); form.add(passwordField);
                card.add(form, BorderLayout.CENTER);

                JPanel bottom = new JPanel(new BorderLayout(0, 8));
                bottom.setOpaque(false);
                JLabel help = new JLabel(role == UserRole.CUSTOMER
                        ? "New customer? Choose Create account."
                        : "Use the credentials assigned to your role.");
                help.setFont(new Font("SansSerif", Font.PLAIN, 14));
                help.setForeground(UiTheme.MUTED);
                bottom.add(help, BorderLayout.NORTH);

                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
                actions.setOpaque(false);
                submitButton = new JButton("Sign in");
                UiTheme.styleRoleButton(submitButton, role);
                submitButton.addActionListener(event -> submit());
                toggleButton = secondary(role == UserRole.CUSTOMER ? "Create account" : "Back to roles", event -> toggleOrBack());
                toggleButton.setVisible(role == UserRole.CUSTOMER);
                JButton backButton = secondary("Back", event -> returnToRoleSelection());
                for (JButton actionButton : new JButton[]{backButton, toggleButton, submitButton}) {
                    actionButton.setFont(new Font("SansSerif", Font.BOLD, 14));
                    actionButton.setMargin(new Insets(11, 18, 11, 18));
                    actionButton.setPreferredSize(new Dimension(actionButton == backButton ? 100
                            : actionButton == submitButton ? 245 : 165, 46));
                }
                actions.add(backButton);
                actions.add(toggleButton);
                actions.add(submitButton);
                bottom.add(actions, BorderLayout.SOUTH);
                card.add(bottom, BorderLayout.SOUTH);

                GridBagConstraints constraints = new GridBagConstraints();
                constraints.gridx = 0; constraints.gridy = 0;
                constraints.weightx = 1; constraints.weighty = 1;
                constraints.fill = GridBagConstraints.NONE;
                add(card, constraints);
            }

            private void toggleOrBack() {
                if (role != UserRole.CUSTOMER) {
                    returnToRoleSelection();
                    return;
                }
                registering = !registering;
                if (registering) {
                    form.add(nameLabel, 0);
                    form.add(nameField, 1);
                } else {
                    form.remove(nameLabel);
                    form.remove(nameField);
                }
                submitButton.setText(registering ? "Create account & continue" : "Sign in");
                toggleButton.setText(registering ? "Back to sign in" : "Create account");
                revalidate();
                repaint();
                if (registering) nameField.requestFocusInWindow(); else usernameField.requestFocusInWindow();
            }

            private void submit() {
                char[] password = passwordField.getPassword();
                try {
                    if (role == UserRole.CUSTOMER) {
                        CustomerAccount account;
                        if (registering) {
                            account = customerData.register(nameField.getText(), usernameField.getText(), password);
                        } else {
                            account = customerData.authenticate(usernameField.getText(), password);
                        }
                        passwordField.setText("");
                        app.showWorkspace(UserRole.CUSTOMER, account);
                    } else {
                        roleAuth.authenticate(role.key, usernameField.getText(), password);
                        passwordField.setText("");
                        app.showWorkspace(role, null);
                    }
                } catch (IllegalArgumentException exception) {
                    showError(exception.getMessage());
                } catch (IOException exception) {
                    showError("Could not save the account or authenticate.\n" + exception.getMessage());
                } finally {
                    Arrays.fill(password, '\0');
                }
            }

            private void returnToRoleSelection() {
                app.returnToRoleSelection();
                statusLabel.setText("Select a role to continue");
            }
        }
