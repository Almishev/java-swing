package bg.restaurant.swing.ui;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class UiUtil {
    private static final DateTimeFormatter LOG_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private UiUtil() {}

    public static void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    public static void showError(Component parent, Throwable e) {
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        String logPath = appendErrorLog(e);
        String uiText = logPath == null ? msg : (msg + "\n\nЛог: " + logPath);
        JOptionPane.showMessageDialog(parent, uiText, "Грешка", JOptionPane.ERROR_MESSAGE);
    }

    private static String appendErrorLog(Throwable e) {
        try {
            Path logFile = resolveLogFile();
            Files.createDirectories(logFile.getParent());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String entry = "[" + LOG_TS.format(LocalDateTime.now()) + "]\n"
                    + sw + "\n";
            Files.writeString(logFile, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return logFile.toAbsolutePath().toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Path resolveLogFile() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("windows")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                return Path.of(localAppData, "WorkTimeBG", "logs", "app.log");
            }
            return Path.of(System.getProperty("user.home"), "AppData", "Local", "WorkTimeBG", "logs", "app.log");
        }
        if (os.contains("mac")) {
            return Path.of(System.getProperty("user.home"), "Library", "Application Support", "WorkTimeBG", "logs", "app.log");
        }
        return Path.of(System.getProperty("user.home"), ".local", "share", "WorkTimeBG", "logs", "app.log");
    }
}

