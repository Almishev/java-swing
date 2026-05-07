package bg.restaurant.swing.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

public final class UiTheme {
    public static final Color HEADER_BG = new Color(0x1e3a5f);
    public static final Color HEADER_FG = new Color(0xf8fafc);
    public static final Color CONTENT_BG = new Color(0xe8eef4);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color TEXT_PRIMARY = new Color(0x1e293b);
    public static final Color ACCENT = new Color(0x0d9488);
    public static final Color DANGER = new Color(0xdc2626);
    public static final Color MUTED_BUTTON = new Color(0x475569);
    public static final Color SELECTION_ROW = new Color(0xccfbf1);

    private UiTheme() {}

    public static void installGlobalGuards() {
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("Panel.background", CONTENT_BG);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
    }

    public static JPanel wrapInCard(JComponent inner) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xcbd5e1), 1),
                new EmptyBorder(12, 12, 12, 12)));
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    public static void styleToolbar(JPanel panel) {
        panel.setOpaque(true);
        panel.setBackground(HEADER_BG);
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));
        applyToolbarColors(panel);
    }

    private static void applyToolbarColors(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel) {
                child.setForeground(HEADER_FG);
            } else if (child instanceof JTextField) {
                JTextField tf = (JTextField) child;
                tf.setBackground(CARD_BG);
                tf.setForeground(TEXT_PRIMARY);
                tf.setCaretColor(TEXT_PRIMARY);
                tf.setOpaque(true);
            } else if (child instanceof JComboBox<?>) {
                JComboBox<?> combo = (JComboBox<?>) child;
                combo.setBackground(CARD_BG);
                combo.setForeground(TEXT_PRIMARY);
                combo.setOpaque(true);
            } else if (child instanceof JSpinner) {
                JSpinner sp = (JSpinner) child;
                sp.setBackground(CARD_BG);
                Component ed = sp.getEditor();
                if (ed instanceof JSpinner.DefaultEditor) {
                    JTextField tf = ((JSpinner.DefaultEditor) ed).getTextField();
                    tf.setBackground(CARD_BG);
                    tf.setForeground(TEXT_PRIMARY);
                    tf.setCaretColor(TEXT_PRIMARY);
                }
                sp.setOpaque(true);
            } else if (child instanceof JButton) {
                styleToolbarButton((JButton) child);
            } else if (child instanceof Container) {
                applyToolbarColors((Container) child);
            }
        }
    }

    private static void styleToolbarButton(JButton b) {
        String t = b.getText();
        Color bg;
        if ("Изход".equals(t)) {
            bg = MUTED_BUTTON;
        } else if ("Изтрий".equals(t)) {
            bg = DANGER;
        } else if ("Добави потребител".equals(t)) {
            bg = new Color(0x7c3aed);
        } else {
            bg = ACCENT;
        }
        styleFilledButton(b, bg, Color.WHITE);
    }

    public static void styleFilledButton(JButton b, Color bg, Color fg) {
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(brighten(bg, 0.08f));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(bg);
            }
        });
    }

    public static void styleSecondaryButton(JButton b) {
        b.setBackground(new Color(0xf1f5f9));
        b.setForeground(TEXT_PRIMARY);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x94a3b8), 1),
                new EmptyBorder(7, 13, 7, 13)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private static Color brighten(Color c, float amount) {
        int r = Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * amount));
        int g = Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * amount));
        int b = Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * amount));
        return new Color(r, g, b);
    }

    public static void styleTabbedPane(JTabbedPane tabs) {
        tabs.setBackground(CONTENT_BG);
        tabs.setForeground(TEXT_PRIMARY);
        tabs.setBorder(new EmptyBorder(8, 12, 12, 12));
    }

    public static void styleContentArea(JPanel panel) {
        panel.setOpaque(true);
        panel.setBackground(CONTENT_BG);
    }

    public static void styleFormRowPanel(JPanel panel) {
        panel.setOpaque(true);
        panel.setBackground(new Color(0xf8fafc));
        for (Component child : panel.getComponents()) {
            if (child instanceof JLabel) {
                child.setForeground(TEXT_PRIMARY);
            }
        }
    }

    public static void styleAccentButton(JButton b) {
        styleFilledButton(b, ACCENT, Color.WHITE);
    }

    public static void styleMutedButton(JButton b) {
        styleFilledButton(b, MUTED_BUTTON, Color.WHITE);
    }

    public static void styleDangerButton(JButton b) {
        styleFilledButton(b, DANGER, Color.WHITE);
    }

    public static void styleTextField(JTextField tf) {
        tf.setOpaque(true);
        tf.setBackground(CARD_BG);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(TEXT_PRIMARY);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xcbd5e1)),
                new EmptyBorder(6, 10, 6, 10)));
    }

    public static void styleCombo(JComboBox<?> combo) {
        combo.setOpaque(true);
        combo.setBackground(CARD_BG);
        combo.setForeground(TEXT_PRIMARY);
    }

    public static void styleSpinner(JSpinner spinner) {
        spinner.setOpaque(false);
        Component ed = spinner.getEditor();
        if (ed instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) ed).getTextField();
            styleTextField(tf);
        }
    }

    public static void styleDayPanelButton(JButton b) {
        String t = b.getText();
        if (t != null && t.startsWith("Започни")) {
            styleFilledButton(b, ACCENT, Color.WHITE);
        } else if (t != null && t.startsWith("Приключи")) {
            styleFilledButton(b, new Color(0x0891b2), Color.WHITE);
        } else if ("Обнови".equals(t)) {
            styleFilledButton(b, MUTED_BUTTON, Color.WHITE);
        } else if (t != null && t.startsWith("Редактирай")) {
            styleFilledButton(b, new Color(0xd97706), Color.WHITE);
        } else if (t != null && t.startsWith("Изтрий")) {
            styleFilledButton(b, DANGER, Color.WHITE);
        } else {
            styleFilledButton(b, ACCENT, Color.WHITE);
        }
    }

    public static void styleScrollAroundTable(JScrollPane sp) {
        sp.setBorder(BorderFactory.createLineBorder(new Color(0xcbd5e1)));
        sp.getViewport().setBackground(CARD_BG);
    }

    public static void styleDataTable(JTable table) {
        table.setBackground(CARD_BG);
        table.setForeground(TEXT_PRIMARY);
        table.setSelectionBackground(SELECTION_ROW);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setGridColor(new Color(0xe2e8f0));
        table.setShowGrid(true);
        table.setRowHeight(28);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(HEADER_BG);
        header.setForeground(HEADER_FG);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        header.setReorderingAllowed(false);
        DefaultTableCellRenderer hr = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                l.setBackground(HEADER_BG);
                l.setForeground(HEADER_FG);
                l.setOpaque(true);
                l.setHorizontalAlignment(JLabel.LEFT);
                l.setBorder(new EmptyBorder(4, 8, 4, 8));
                return l;
            }
        };
        header.setDefaultRenderer(hr);
    }
}
