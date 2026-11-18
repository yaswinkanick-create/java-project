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

final class CsvUtil {
        private CsvUtil() { }

        public static String formatRow(List<String> values) {
            List<String> escaped = new ArrayList<>(values.size());
            for (String value : values) {
                String safe = value == null ? "" : value;
                if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
                    safe = "\"" + safe.replace("\"", "\"\"") + "\"";
                }
                escaped.add(safe);
            }
            return String.join(",", escaped);
        }

        public static String formatRow(String... values) {
            return formatRow(List.of(values));
        }

        public static List<String> parseLine(String line) {
            List<String> values = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean quoted = false;
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (ch == '"') {
                    if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = !quoted;
                    }
                } else if (ch == ',' && !quoted) {
                    values.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(ch);
                }
            }
            if (quoted) {
                throw new IllegalArgumentException("CSV row has an unmatched quotation mark.");
            }
            values.add(current.toString());
            return values;
        }
    }
