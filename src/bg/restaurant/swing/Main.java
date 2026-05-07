package bg.restaurant.swing;

import bg.restaurant.swing.db.Db;
import bg.restaurant.swing.model.AppUser;
import bg.restaurant.swing.ui.LoginDialog;
import bg.restaurant.swing.ui.MainFrame;
import bg.restaurant.swing.ui.UiTheme;
import bg.restaurant.swing.ui.UiUtil;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Enumeration;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        UiUtil.runOnEdt(() -> {
            try {
                Db.init();
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (SQLException e) {
                UiUtil.showError(null, e);
                return;
            } catch (Exception ignored) {
                // continue with default LAF when only look-and-feel fails
            }
            UiTheme.installGlobalGuards();
            increaseUiFont(16);
            startAppSessionLoop();
        });
    }

    private static void startAppSessionLoop() {
        AppUser loggedInUser = LoginDialog.showAndAuthenticate(null);
        if (loggedInUser == null) {
            System.exit(0);
            return;
        }
        if (!loggedInUser.isAdmin() && loggedInUser.employeeId() == null) {
            UiUtil.showError(null, new IllegalStateException("Потребителят няма свързан служител. Свържи го от администратор."));
            SwingUtilities.invokeLater(Main::startAppSessionLoop);
            return;
        }
        MainFrame f = new MainFrame(loggedInUser, () -> SwingUtilities.invokeLater(Main::startAppSessionLoop));
        f.setVisible(true);
    }

    private static void increaseUiFont(int size) {
        FontUIResource uiFont = new FontUIResource("Segoe UI", Font.PLAIN, size);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, uiFont);
            }
        }
    }
}

