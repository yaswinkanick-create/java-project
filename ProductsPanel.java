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

final class ProductsPanel extends AppPanel {
            private final ProductTableModel model = new ProductTableModel();
            private final JTable table = new JTable(model);
            private final TableRowSorter<ProductTableModel> sorter = new TableRowSorter<>(model);
            private final JTextField search = new JTextField(18);

            ProductsPanel(MainFrame app) {
            super(app);
                table.setFillsViewportHeight(true);
                table.setRowHeight(31);
                table.setShowVerticalLines(false);
                table.setGridColor(UiTheme.BORDER);
                table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                table.setAutoCreateRowSorter(false);
                table.setRowSorter(sorter);
                table.getTableHeader().setReorderingAllowed(false);
                JPanel actions = toolbar();
                actions.add(new JLabel("Search"));
                actions.add(search);
                actions.add(primary("+ Add product", e -> openProductDialog(null)));
                actions.add(secondary("Edit", e -> {
                    Product p = selectedProduct(table, model);
                    if (p == null) showInfo("Select a product row first."); else openProductDialog(p);
                }));
                actions.add(secondary("Delete", e -> deleteSelected()));
                actions.add(secondary("Import CSV", e -> importCsv()));
                actions.add(secondary("Export CSV", e -> exportCsv()));
                search.getDocument().addDocumentListener(new DocumentListener() {
                    private void apply() {
                        String text = search.getText().trim();
                        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
                    }
                    @Override public void insertUpdate(DocumentEvent e) { apply(); }
                    @Override public void removeUpdate(DocumentEvent e) { apply(); }
                    @Override public void changedUpdate(DocumentEvent e) { apply(); }
                });
                setLayout(new BorderLayout());
                setBackground(UiTheme.CANVAS);
                setBorder(BorderFactory.createEmptyBorder(16, 4, 4, 4));
                add(actions, BorderLayout.NORTH);
                add(scroll(table), BorderLayout.CENTER);
            }
            void refresh() { model.setProducts(service.getProducts()); }

            private void deleteSelected() {
                Product product = selectedProduct(table, model);
                if (product == null) { showInfo("Select a product row first."); return; }
                int result = JOptionPane.showConfirmDialog(app,
                        "Delete " + product.getName() + "? Products with stock or transaction history cannot be deleted.",
                        "Confirm deletion", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.OK_OPTION) perform("Product deletion", () -> service.deleteProduct(product.getId()));
            }

            private void importCsv() {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Import product CSV");
                if (chooser.showOpenDialog(app) == JFileChooser.APPROVE_OPTION) {
                    Path path = chooser.getSelectedFile().toPath();
                    int confirm = JOptionPane.showConfirmDialog(app,
                            "Import products from this CSV? Rows are merged by Product ID; matching product fields and quantity will be updated.\n\n"
                                    + "Export a backup first if you need to preserve the current values.",
                            "Confirm CSV import", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirm == JOptionPane.OK_OPTION) perform("Product CSV import", () -> service.importProducts(path));
                }
            }

            private void exportCsv() {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Export products to CSV");
                chooser.setSelectedFile(new java.io.File("products-export.csv"));
                if (chooser.showSaveDialog(app) == JFileChooser.APPROVE_OPTION) {
                    Path path = chooser.getSelectedFile().toPath();
                    try {
                        service.exportProducts(path);
                        statusLabel.setText("Product CSV export | Saved");
                        showInfo("Product CSV export completed:\n" + path.toAbsolutePath());
                    } catch (IOException exception) {
                        showError("Could not export product CSV.\n" + exception.getMessage());
                    }
                }
            }
        }
