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

final class CustomerAccount {
        final String id;
        final String name;
        final String email;
        final String saltHex;
        final String passwordHashHex;

        CustomerAccount(String id, String name, String email, String saltHex, String passwordHashHex) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.saltHex = saltHex;
            this.passwordHashHex = passwordHashHex;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        String saltHex() { return saltHex; }
        String passwordHashHex() { return passwordHashHex; }
    }
