package bg.restaurant.swing.ui;

import bg.restaurant.swing.model.AppUser;
import bg.restaurant.swing.repo.UserRepo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;

public final class LoginDialog extends JDialog {
    private final UserRepo userRepo = new UserRepo();

    private final JPasswordField codeField = new JPasswordField(14);
    private AppUser authenticatedUser;

    private LoginDialog(Window parent) {
        super(parent, "Вход", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Вход");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 32f));
        titleLabel.setForeground(UiTheme.HEADER_FG);

        JPanel headerStrip = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerStrip.setOpaque(true);
        headerStrip.setBackground(UiTheme.HEADER_BG);
        headerStrip.setBorder(new EmptyBorder(32, 16, 32, 16));
        headerStrip.add(titleLabel);

        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setOpaque(true);
        centerWrap.setBackground(UiTheme.CONTENT_BG);

        JPanel card = new JPanel(new GridBagLayout());
        card.setOpaque(true);
        card.setBackground(UiTheme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xcbd5e1), 1),
                new EmptyBorder(28, 36, 28, 36)));

        JLabel codeCap = new JLabel("Код за достъп:");
        codeCap.setForeground(UiTheme.TEXT_PRIMARY);

        ((PlainDocument) codeField.getDocument()).setDocumentFilter(new DigitsOnlyFilter());
        codeField.setFont(codeField.getFont().deriveFont(Font.PLAIN, 20f));
        codeField.setOpaque(true);
        codeField.setCaretColor(UiTheme.TEXT_PRIMARY);
        codeField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xcbd5e1)),
                new EmptyBorder(8, 12, 8, 12)));
        codeField.setBackground(UiTheme.CARD_BG);
        codeField.setForeground(UiTheme.TEXT_PRIMARY);

        JButton loginBtn = new JButton("Влез");
        loginBtn.addActionListener(e -> tryLogin());
        UiTheme.styleAccentButton(loginBtn);

        JButton cancelBtn = new JButton("Отказ");
        cancelBtn.addActionListener(e -> dispose());
        UiTheme.styleSecondaryButton(cancelBtn);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.setOpaque(false);
        buttons.add(cancelBtn);
        buttons.add(loginBtn);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        card.add(codeCap, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        card.add(codeField, c);

        c.gridy = 2;
        c.insets = new Insets(20, 6, 0, 6);
        c.anchor = GridBagConstraints.CENTER;
        card.add(buttons, c);

        GridBagConstraints wrap = new GridBagConstraints();
        wrap.gridx = 0;
        wrap.gridy = 0;
        wrap.weightx = 1;
        wrap.weighty = 1;
        centerWrap.add(card, wrap);

        add(headerStrip, BorderLayout.NORTH);
        add(centerWrap, BorderLayout.CENTER);

        getRootPane().setDefaultButton(loginBtn);

        Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setBounds(screenBounds);
        setLocationRelativeTo(parent);
    }

    public static AppUser showAndAuthenticate(Window parent) {
        LoginDialog dialog = new LoginDialog(parent);
        dialog.setVisible(true);
        return dialog.authenticatedUser;
    }

    private void tryLogin() {
        String value = new String(codeField.getPassword()).trim();
        if (value.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Въведи цифров код.", "Инфо", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            AppUser user = userRepo.findByCode(value);
            if (user != null) {
                authenticatedUser = user;
                dispose();
                return;
            }
            JOptionPane.showMessageDialog(this, "Грешен код за достъп.", "Грешка", JOptionPane.ERROR_MESSAGE);
            codeField.setText("");
            codeField.requestFocusInWindow();
        } catch (Exception ex) {
            UiUtil.showError(this, ex);
        }
    }

    private static final class DigitsOnlyFilter extends javax.swing.text.DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string == null) return;
            if (isDigits(string)) super.insertString(fb, offset, string, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null) {
                super.replace(fb, offset, length, null, attrs);
                return;
            }
            if (isDigits(text)) super.replace(fb, offset, length, text, attrs);
        }

        private static boolean isDigits(String value) {
            for (int i = 0; i < value.length(); i++) {
                if (!Character.isDigit(value.charAt(i))) return false;
            }
            return true;
        }
    }
}
