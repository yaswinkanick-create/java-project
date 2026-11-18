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

final class RoleAuthStore {
        private static final String[] HEADER = {"role", "username", "display_name", "salt_hex", "password_hash_hex"};
        private final Path accountsFile;
        private final ArrayList<RoleCredential> credentials = new ArrayList<>();

        public RoleAuthStore(Path dataDirectory) throws IOException {
            Path directory = dataDirectory.toAbsolutePath().normalize();
            Files.createDirectories(directory);
            accountsFile = directory.resolve("role_accounts.csv");
            CustomerDataStore.ensureHeader(accountsFile, HEADER);
            load();
            ensureDefault("INVENTORY_SELLER", "seller", "Inventory Seller", "Seller@123");
            ensureDefault("SUPPLIER", "supplier", "Supplier", "Supplier@123");
        }

        public synchronized boolean authenticate(String role, String username, char[] password) {
            String roleKey = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
            String userKey = username == null ? "" : username.trim();
            RoleCredential account = credentials.stream()
                    .filter(item -> item.role.equals(roleKey) && item.username.equalsIgnoreCase(userKey))
                    .findFirst().orElse(null);
            if (account == null || password == null) {
                throw new IllegalArgumentException("Username or password is incorrect.");
            }
            byte[] actual = HexFormat.of().parseHex(CustomerDataStore.hashPassword(password,
                    HexFormat.of().parseHex(account.saltHex)));
            byte[] expected = HexFormat.of().parseHex(account.passwordHashHex);
            if (!MessageDigest.isEqual(actual, expected)) {
                throw new IllegalArgumentException("Username or password is incorrect.");
            }
            return true;
        }

        private void load() throws IOException {
            credentials.clear();
            for (List<String> row : CustomerDataStore.readRows(accountsFile, HEADER)) {
                credentials.add(new RoleCredential(row.get(0), row.get(1), row.get(2), row.get(3), row.get(4)));
            }
        }

        private void ensureDefault(String role, String username, String displayName, String defaultPassword) throws IOException {
            boolean exists = credentials.stream().anyMatch(item -> item.role.equals(role));
            if (exists) return;
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            char[] password = defaultPassword.toCharArray();
            try {
                credentials.add(new RoleCredential(role, username, displayName,
                        HexFormat.of().formatHex(salt), CustomerDataStore.hashPassword(password, salt)));
                save();
            } finally {
                Arrays.fill(password, '\0');
            }
        }

        private void save() throws IOException {
            List<String[]> rows = new ArrayList<>();
            for (RoleCredential item : credentials) {
                rows.add(new String[]{item.role, item.username, item.displayName, item.saltHex, item.passwordHashHex});
            }
            CustomerDataStore.writeRows(accountsFile, HEADER, rows);
        }

        private static final class RoleCredential {
            private final String role;
            private final String username;
            private final String displayName;
            private final String saltHex;
            private final String passwordHashHex;

            private RoleCredential(String role, String username, String displayName, String saltHex, String passwordHashHex) {
                this.role = role;
                this.username = username;
                this.displayName = displayName;
                this.saltHex = saltHex;
                this.passwordHashHex = passwordHashHex;
            }
        }
    }
