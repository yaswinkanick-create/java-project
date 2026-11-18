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

final class Supplier {
        private final String id;
        private String name;
        private String phone;
        private String email;

        public Supplier(String id, String name, String phone, String email) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Supplier ID is required.");
            }
            this.id = id.trim();
            updateDetails(name, phone, email);
        }

        public void updateDetails(String name, String phone, String email) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Supplier name is required.");
            }
            this.name = name.trim();
            this.phone = phone == null ? "" : phone.trim();
            this.email = email == null ? "" : email.trim();
            if (!this.email.isEmpty() && !this.email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
                throw new IllegalArgumentException("Enter a valid supplier email address.");
            }
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }

        @Override
        public boolean equals(Object other) {
            return other instanceof Supplier supplier && id.equalsIgnoreCase(supplier.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id.toLowerCase());
        }
    }
