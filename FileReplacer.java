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

final class FileReplacer {
    private FileReplacer() { }

static void replace(Path temporary, Path target) throws IOException {
        IOException atomicFailure;
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return;
        } catch (IOException exception) {
            atomicFailure = exception;
        }

        IOException moveFailure;
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            return;
        } catch (IOException exception) {
            moveFailure = exception;
        }

        Path parent = target.toAbsolutePath().getParent();
        Path backup = null;
        boolean originalBackedUp = false;
        boolean replacementCompleted = false;
        boolean originalRestored = true;
        IOException copyFailure = null;
        try {
            if (Files.exists(target)) {
                backup = Files.createTempFile(parent, "inventory-backup-", ".tmp");
                Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
                originalBackedUp = true;
            }
            Files.copy(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            replacementCompleted = true;
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* replacement succeeded; leave temp for later cleanup */ }
        } catch (IOException exception) {
            copyFailure = exception;
            if (originalBackedUp && backup != null) {
                try {
                    Files.copy(backup, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException restoreFailure) {
                    originalRestored = false;
                    copyFailure.addSuppressed(restoreFailure);
                }
            }
            copyFailure.addSuppressed(moveFailure);
            copyFailure.addSuppressed(atomicFailure);
        } finally {
            if (backup != null && (replacementCompleted || originalRestored)) {
                try { Files.deleteIfExists(backup); } catch (IOException ignored) { /* leave recoverable backup */ }
            }
        }

        if (!replacementCompleted) {
            String message = "Could not save CSV file '" + target + "'. Close any program that may have this file open, then retry.";
            if (backup != null && !originalRestored) message += " The original CSV backup is preserved at: " + backup;
            if (Files.exists(temporary)) message += " The new data is preserved at: " + temporary;
            throw new IOException(message, copyFailure);
        }
    }
}
