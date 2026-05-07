package bg.restaurant.swing.ui;

import bg.restaurant.swing.model.AppUser;
import bg.restaurant.swing.model.Employee;
import bg.restaurant.swing.model.ShiftRow;
import bg.restaurant.swing.repo.EmployeeRepo;
import bg.restaurant.swing.repo.ShiftRepo;
import bg.restaurant.swing.repo.UserRepo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Objects;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class MainFrame extends JFrame {
    private final Runnable onLogout;
    private final AppUser currentUser;
    private final EmployeeRepo employeeRepo = new EmployeeRepo();
    private final ShiftRepo shiftRepo = new ShiftRepo();
    private final UserRepo userRepo = new UserRepo();

    private final JComboBox<Employee> employeeCombo = new JComboBox<>();
    private final JTextField employeeNameField = new JTextField(20);

    private final JSpinner daySpinner = new JSpinner(new SpinnerDateModel());
    private final JTextField manualStartField = new JTextField(16);
    private final JTextField manualEndField = new JTextField(16);
    private final DefaultTableModel dayModel = new DefaultTableModel(
            new Object[]{"ID", "Служител", "Начало", "Край", "Часове"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable dayTable = new JTable(dayModel);

    private final JComboBox<String> monthCombo = new JComboBox<>();
    private final DefaultTableModel monthModel = new DefaultTableModel(
            new Object[]{"Служител", "Общо часове (месец)"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable monthTable = new JTable(monthModel);

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
    private final Long scopedEmployeeId;
    /** Запазва последната избрана смяна — при клик върху бутон таблицата понякога губи селекция и getSelectedRow() става -1 */
    private Long cachedSelectedShiftId;

    public MainFrame(AppUser currentUser, Runnable onLogout) {
        super("Работно време (H2)");
        this.currentUser = currentUser;
        this.onLogout = Objects.requireNonNull(onLogout);
        this.scopedEmployeeId = currentUser.isAdmin() ? null : currentUser.employeeId();
        applyWindowIcon();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                performLogout();
            }
        });
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        Container cp = getContentPane();
        if (cp instanceof JComponent) {
            JComponent root = (JComponent) cp;
            root.setOpaque(true);
            root.setBackground(UiTheme.CONTENT_BG);
        }

        setLayout(new BorderLayout());
        JPanel toolbar = buildTopPanel();
        UiTheme.styleToolbar(toolbar);
        add(toolbar, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Дневник", buildDayPanel());
        tabs.addTab("Справка месец", buildMonthPanel());
        UiTheme.styleTabbedPane(tabs);
        add(tabs, BorderLayout.CENTER);

        refreshEmployees();
        refreshDayTable(selectedDay());
        refreshMonthSummary(selectedMonth());
    }

    private void performLogout() {
        dispose();
        onLogout.run();
    }

    private JPanel buildTopPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        p.add(new JLabel("Служител:"), c);

        c.gridx = 1;
        employeeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Employee) {
                    Employee e = (Employee) value;
                    setText(e.name());
                }
                if (isSelected) {
                    setBackground(UiTheme.ACCENT);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(UiTheme.CARD_BG);
                    setForeground(UiTheme.TEXT_PRIMARY);
                }
                setOpaque(true);
                return this;
            }
        });
        p.add(employeeCombo, c);
        employeeCombo.setEnabled(currentUser.isAdmin());

        c.gridx = 2;
        p.add(new JLabel("Влязъл:"), c);

        c.gridx = 3;
        String roleText = currentUser.isAdmin() ? "admin" : "user";
        JLabel currentUserLabel = new JLabel((currentUser.displayName() == null ? "Без име" : currentUser.displayName()) + " (" + roleText + ")");
        p.add(currentUserLabel, c);

        if (currentUser.isAdmin()) {
            c.gridx = 4;
            p.add(new JLabel("Нов служител:"), c);

            c.gridx = 5;
            p.add(employeeNameField, c);

            c.gridx = 6;
            JButton addEmp = new JButton("Добави");
            addEmp.addActionListener(e -> {
                try {
                    employeeRepo.create(employeeNameField.getText());
                    employeeNameField.setText("");
                    refreshEmployees();
                } catch (Exception ex) {
                    UiUtil.showError(this, ex);
                }
            });
            p.add(addEmp, c);

            c.gridx = 7;
            JButton delEmp = new JButton("Изтрий");
            delEmp.addActionListener(e -> {
                Employee emp = (Employee) employeeCombo.getSelectedItem();
                if (emp == null) return;
                int ok = JOptionPane.showConfirmDialog(this,
                        "Да изтрия ли \"" + emp.name() + "\"? (ще се изтрият и смените)",
                        "Потвърди",
                        JOptionPane.YES_NO_OPTION);
                if (ok != JOptionPane.YES_OPTION) return;
                try {
                    employeeRepo.delete(emp.id());
                    refreshEmployees();
                    refreshDayTable(selectedDay());
                    refreshMonthSummary(selectedMonth());
                } catch (Exception ex) {
                    UiUtil.showError(this, ex);
                }
            });
            p.add(delEmp, c);

            c.gridx = 8;
            JButton addUserBtn = new JButton("Добави потребител");
            addUserBtn.setToolTipText("Създава user с име и цифров код");
            addUserBtn.addActionListener(e -> createUserFromDialog());
            p.add(addUserBtn, c);
        }

        c.gridx = currentUser.isAdmin() ? 9 : 4;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.EAST;
        JButton exitBtn = new JButton("Изход");
        exitBtn.addActionListener(e -> performLogout());
        p.add(exitBtn, c);

        return p;
    }

    private void createUserFromDialog() {
        if (!currentUser.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Само администратор може да създава потребители.", "Достъп отказан", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField nameField = new JTextField(18);
        JTextField codeField = new JTextField(18);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        panel.add(new JLabel("Име:"), c);
        c.gridx = 1;
        panel.add(nameField, c);

        c.gridx = 0; c.gridy = 1;
        panel.add(new JLabel("Код (само цифри):"), c);
        c.gridx = 1;
        panel.add(codeField, c);

        int result = JOptionPane.showConfirmDialog(this, panel, "Нов потребител", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String cleanedName = nameField.getText() == null ? "" : nameField.getText().trim();
        String cleanedCode = codeField.getText() == null ? "" : codeField.getText().trim();
        if (cleanedName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Името е празно.", "Инфо", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (cleanedCode.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Кодът е празен.", "Инфо", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!isDigits(cleanedCode)) {
            JOptionPane.showMessageDialog(this, "Кодът трябва да съдържа само цифри.", "Инфо", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            // Newly created users are always non-admin by requirement.
            userRepo.createUser(cleanedCode, cleanedName, false);
            JOptionPane.showMessageDialog(this, "Потребителят е създаден (роля: user).", "Успех", JOptionPane.INFORMATION_MESSAGE);
            refreshEmployees();
        } catch (Exception ex) {
            UiUtil.showError(this, ex);
        }
    }

    private boolean isDigits(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
        }
        return true;
    }

    private JPanel buildDayPanel() {
        JPanel p = new JPanel(new BorderLayout());
        UiTheme.styleContentArea(p);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        UiTheme.styleFormRowPanel(top);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        UiTheme.styleFormRowPanel(row1);
        row1.add(new JLabel("Дата:"));

        daySpinner.setEditor(new JSpinner.DateEditor(daySpinner, "yyyy-MM-dd"));
        daySpinner.addChangeListener(e -> refreshDayTable(selectedDay()));
        UiTheme.styleSpinner(daySpinner);
        JComponent editor = daySpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField dateField = ((JSpinner.DefaultEditor) editor).getTextField();
            dateField.setColumns(14);
            dateField.setHorizontalAlignment(JTextField.CENTER);
            dateField.setFont(dateField.getFont().deriveFont(Font.PLAIN, 18f));
        }
        int dateRowHeight = Math.max(daySpinner.getPreferredSize().height, 38);
        daySpinner.setPreferredSize(new Dimension(280, dateRowHeight));
        daySpinner.setMinimumSize(new Dimension(260, dateRowHeight));
        row1.add(daySpinner);

        row1.add(new JLabel("Начало (час):"));
        manualStartField.setText(tf.format(LocalTime.now()));
        UiTheme.styleTextField(manualStartField);
        row1.add(manualStartField);

        row1.add(new JLabel("Край (час):"));
        manualEndField.setText(tf.format(LocalTime.now()));
        UiTheme.styleTextField(manualEndField);
        row1.add(manualEndField);

        JButton saveBtn = new JButton("Запази");
        saveBtn.addActionListener(e -> {
            Employee emp = (Employee) employeeCombo.getSelectedItem();
            if (emp == null) {
                JOptionPane.showMessageDialog(this, "Няма избран служител.", "Инфо", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            try {
                LocalDateTime startManual = parseDateTimeFromSelectedDate(manualStartField.getText(), selectedDay(), "начало");
                LocalDateTime endManual = parseDateTimeFromSelectedDate(manualEndField.getText(), selectedDay(), "край");
                endManual = normalizeEndTimeForOvernight(startManual, endManual);

                Long selectedShiftId = resolveSelectedShiftIdFromCurrentSelection();
                if (selectedShiftId != null) {
                    shiftRepo.updateShift(selectedShiftId, startManual, endManual);
                } else {
                    shiftRepo.createShift(emp.id(), startManual, endManual);
                }
                refreshDayTable(selectedDay());
                refreshMonthSummary(selectedMonth());
                dayTable.clearSelection();
                cachedSelectedShiftId = null;
            } catch (Exception ex) {
                UiUtil.showError(this, ex);
            }
        });
        row1.add(saveBtn);

        /*
        Старата функционалност е запазена за бъдеща употреба.

        JButton startBtn = new JButton("Започни (сега)");
        startBtn.addActionListener(e -> {
            Employee emp = (Employee) employeeCombo.getSelectedItem();
            if (emp == null) {
                JOptionPane.showMessageDialog(this, "Няма избран служител.", "Инфо", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            try {
                shiftRepo.startShift(emp.id(), LocalDateTime.now());
                refreshDayTable(selectedDay());
                refreshMonthSummary(selectedMonth());
            } catch (Exception ex) {
                UiUtil.showError(this, ex);
            }
        });
        row1.add(startBtn);

        JButton startManualBtn = new JButton("Започни (ръчно)");
        startManualBtn.addActionListener(e -> {
            Employee emp = (Employee) employeeCombo.getSelectedItem();
            if (emp == null) {
                JOptionPane.showMessageDialog(this, "Няма избран служител.", "Инфо", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            try {
                LocalDateTime startManual = parseDateTimeFromSelectedDate(manualStartField.getText(), selectedDay(), "начало");
                shiftRepo.startShift(emp.id(), startManual);
                refreshDayTable(selectedDay());
                refreshMonthSummary(selectedMonth());
            } catch (Exception ex) {
                UiUtil.showError(this, ex);
            }
        });
        row1.add(startManualBtn);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        UiTheme.styleFormRowPanel(row2);

        JButton endBtn = new JButton("Приключи избран (сега)");
        endBtn.addActionListener(e -> {
            Long id = resolveSelectedShiftIdOrWarn();
            if (id == null) return;
            try {
                shiftRepo.endShift(id, LocalDateTime.now());
                refreshDayTable(selectedDay());
                refreshMonthSummary(selectedMonth());
            } catch (Exception ex) {
                UiUtil.showError(this, ex);
            }
        });
        row2.add(endBtn);

        JButton endManualBtn = new JButton("Приключи избран (ръчно)");
        endManualBtn.addActionListener(e -> {
            Long id = resolveSelectedShiftIdOrWarn();
            if (id == null) return;
            try {
                LocalDateTime endManual = parseDateTimeFromSelectedDate(manualEndField.getText(), selectedDay(), "край");
                LocalDateTime selectedStart = findStartTimeForShiftId(id);
                endManual = normalizeEndTimeForOvernight(selectedStart, endManual);
                shiftRepo.endShift(id, endManual);
                refreshDayTable(selectedDay());
                refreshMonthSummary(selectedMonth());
            } catch (Exception ex) {
                UiUtil.showError(this, ex);
            }
        });
        row2.add(endManualBtn);

        JButton refreshBtn = new JButton("Обнови");
        refreshBtn.addActionListener(e -> {
            refreshDayTable(selectedDay());
            refreshMonthSummary(selectedMonth());
        });
        row2.add(refreshBtn);

        JButton editBtn = new JButton("Редактирай избран");
        editBtn.addActionListener(e -> {
            Long id = resolveSelectedShiftIdOrWarn();
            if (id == null) return;
            try {
                LocalDateTime startManual = parseDateTimeFromSelectedDate(manualStartField.getText(), selectedDay(), "начало");
                LocalDateTime endManual = parseOptionalDateTimeFromSelectedDate(manualEndField.getText(), selectedDay(), "край");
                endManual = normalizeEndTimeForOvernight(startManual, endManual);
                shiftRepo.updateShift(id, startManual, endManual);
                refreshDayTable(selectedDay());
                refreshMonthSummary(selectedMonth());
            } catch (Exception ex) {
                UiUtil.showError(this, ex);
            }
        });
        row2.add(editBtn);

        JButton deleteBtn = new JButton("Изтрий избран");
        deleteBtn.addActionListener(e -> {
            Long id = resolveSelectedShiftIdOrWarn();
            if (id == null) return;
            int ok = JOptionPane.showConfirmDialog(
                    this,
                    "Да изтрия ли избраната смяна?",
                    "Потвърди",
                    JOptionPane.YES_NO_OPTION
            );
            if (ok != JOptionPane.YES_OPTION) return;
            try {
                shiftRepo.deleteShift(id);
                refreshDayTable(selectedDay());
                refreshMonthSummary(selectedMonth());
            } catch (Exception ex) {
                UiUtil.showError(this, ex);
            }
        });
        row2.add(deleteBtn);

        styleActionButtons(row1);
        styleActionButtons(row2);
        */
        UiTheme.styleDayPanelButton(saveBtn);

        top.add(row1);

        p.add(top, BorderLayout.NORTH);

        dayTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dayTable.getColumnModel().getColumn(0).setMinWidth(60);
        dayTable.getColumnModel().getColumn(0).setMaxWidth(80);
        dayTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = dayTable.getSelectedRow();
            if (row < 0) return;
            cachedSelectedShiftId = parseRowShiftId(dayModel.getValueAt(row, 0));
            Object startObj = dayModel.getValueAt(row, 2);
            Object endObj = dayModel.getValueAt(row, 3);
            manualStartField.setText(extractTimeText(startObj));
            manualEndField.setText(extractTimeText(endObj));
        });
        UiTheme.styleDataTable(dayTable);
        JScrollPane dayScroll = new JScrollPane(dayTable);
        UiTheme.styleScrollAroundTable(dayScroll);
        p.add(dayScroll, BorderLayout.CENTER);

        return p;
    }

    private JPanel buildMonthPanel() {
        JPanel p = new JPanel(new BorderLayout());
        UiTheme.styleContentArea(p);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        UiTheme.styleFormRowPanel(top);
        top.add(new JLabel("Месец:"));
        fillMonthCombo();
        UiTheme.styleCombo(monthCombo);
        monthCombo.addActionListener(e -> refreshMonthSummary(selectedMonth()));
        top.add(monthCombo);

        JButton refresh = new JButton("Обнови");
        refresh.addActionListener(e -> refreshMonthSummary(selectedMonth()));
        UiTheme.styleDayPanelButton(refresh);
        top.add(refresh);

        p.add(top, BorderLayout.NORTH);
        UiTheme.styleDataTable(monthTable);
        JScrollPane monthScroll = new JScrollPane(monthTable);
        UiTheme.styleScrollAroundTable(monthScroll);
        p.add(monthScroll, BorderLayout.CENTER);

        return p;
    }

    private void styleActionButtons(Container row) {
        for (Component comp : row.getComponents()) {
            if (comp instanceof JButton) {
                UiTheme.styleDayPanelButton((JButton) comp);
            }
        }
    }

    private void fillMonthCombo() {
        monthCombo.removeAllItems();
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 18; i++) {
            YearMonth m = now.minusMonths(i);
            monthCombo.addItem(m.toString()); // yyyy-MM
        }
        monthCombo.setSelectedIndex(0);
    }

    private LocalDate selectedDay() {
        java.util.Date d = (java.util.Date) daySpinner.getValue();
        return new java.sql.Date(d.getTime()).toLocalDate();
    }

    private YearMonth selectedMonth() {
        String s = (String) monthCombo.getSelectedItem();
        if (s == null || s.trim().isEmpty()) return YearMonth.now();
        return YearMonth.parse(s);
    }

    private void refreshEmployees() {
        try {
            List<Employee> list = employeeRepo.list();
            Employee selected = (Employee) employeeCombo.getSelectedItem();
            employeeCombo.removeAllItems();
            for (Employee e : list) {
                if (scopedEmployeeId == null || e.id() == scopedEmployeeId) {
                    employeeCombo.addItem(e);
                }
            }
            if (selected != null) {
                for (int i = 0; i < employeeCombo.getItemCount(); i++) {
                    Employee it = employeeCombo.getItemAt(i);
                    if (it.id() == selected.id()) {
                        employeeCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            UiUtil.showError(this, ex);
        }
    }

    private void refreshDayTable(LocalDate day) {
        try {
            List<ShiftRow> rows = shiftRepo.listForDay(day, scopedEmployeeId);
            dayModel.setRowCount(0);
            for (ShiftRow r : rows) {
                String start = r.startTime() == null ? "" : dtf.format(r.startTime());
                String end = r.endTime() == null ? "" : dtf.format(r.endTime());
                String hours = formatHours(r.startTime(), r.endTime());
                dayModel.addRow(new Object[]{r.id(), r.employeeName(), start, end, hours});
            }
            if (cachedSelectedShiftId != null) {
                int r = findRowByShiftId(cachedSelectedShiftId.longValue());
                if (r >= 0) {
                    final int selRow = r;
                    SwingUtilities.invokeLater(() -> {
                        if (selRow < 0 || selRow >= dayTable.getRowCount()) return;
                        dayTable.setRowSelectionInterval(selRow, selRow);
                        Rectangle cell = dayTable.getCellRect(selRow, 0, true);
                        if (cell != null) dayTable.scrollRectToVisible(cell);
                    });
                } else {
                    cachedSelectedShiftId = null;
                }
            }
        } catch (Exception ex) {
            UiUtil.showError(this, ex);
        }
    }

    private static Long parseRowShiftId(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Long) return (Long) raw;
        if (raw instanceof Integer) return ((Integer) raw).longValue();
        if (raw instanceof Number) return ((Number) raw).longValue();
        try {
            return Long.parseLong(raw.toString().trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int findRowByShiftId(long shiftId) {
        for (int r = 0; r < dayModel.getRowCount(); r++) {
            Long id = parseRowShiftId(dayModel.getValueAt(r, 0));
            if (id != null && id.longValue() == shiftId) return r;
        }
        return -1;
    }

    private Long resolveSelectedShiftIdOrWarn() {
        int row = dayTable.getSelectedRow();
        if (row >= 0) {
            Long id = parseRowShiftId(dayModel.getValueAt(row, 0));
            if (id != null) {
                cachedSelectedShiftId = id;
                return id;
            }
        }
        if (cachedSelectedShiftId != null) {
            int r = findRowByShiftId(cachedSelectedShiftId.longValue());
            if (r >= 0) {
                dayTable.setRowSelectionInterval(r, r);
                return cachedSelectedShiftId;
            }
        }
        JOptionPane.showMessageDialog(this, "Избери смяна от таблицата.", "Инфо", JOptionPane.INFORMATION_MESSAGE);
        return null;
    }

    private Long resolveSelectedShiftId() {
        int row = dayTable.getSelectedRow();
        if (row >= 0) {
            Long id = parseRowShiftId(dayModel.getValueAt(row, 0));
            if (id != null) {
                cachedSelectedShiftId = id;
                return id;
            }
        }
        if (cachedSelectedShiftId != null) {
            int r = findRowByShiftId(cachedSelectedShiftId.longValue());
            if (r >= 0) {
                dayTable.setRowSelectionInterval(r, r);
                return cachedSelectedShiftId;
            }
        }
        return null;
    }

    /**
     * За опростения режим с бутон "Запази":
     * редактираме само ако потребителят е избрал ред в момента.
     * Иначе създаваме нов запис (напр. втора смяна в същия ден).
     */
    private Long resolveSelectedShiftIdFromCurrentSelection() {
        int row = dayTable.getSelectedRow();
        if (row < 0) return null;
        Long id = parseRowShiftId(dayModel.getValueAt(row, 0));
        if (id != null) cachedSelectedShiftId = id;
        return id;
    }

    private void refreshMonthSummary(YearMonth month) {
        try {
            List<ShiftRow> rows = shiftRepo.listForMonth(month, scopedEmployeeId);
            monthModel.setRowCount(0);

            java.util.Map<String, Double> totals = new java.util.TreeMap<>();
            for (ShiftRow r : rows) {
                if (r.endTime() == null) continue;
                double h = computeWorkedMinutes(r.startTime(), r.endTime()) / 60.0;
                totals.merge(r.employeeName(), h, Double::sum);
            }

            for (java.util.Map.Entry<String, Double> e : totals.entrySet()) {
                monthModel.addRow(new Object[]{e.getKey(), String.format("%.2f", e.getValue())});
            }
        } catch (Exception ex) {
            UiUtil.showError(this, ex);
        }
    }

    private String formatHours(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return "";
        long minutes = computeWorkedMinutes(start, end);
        long h = minutes / 60;
        long m = minutes % 60;
        return h + "ч " + m + "м";
    }

    private long computeWorkedMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        LocalDateTime normalizedEnd = normalizeEndTimeForOvernight(start, end);
        return Duration.between(start, normalizedEnd).toMinutes();
    }

    private LocalDateTime normalizeEndTimeForOvernight(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return end;
        if (end.isBefore(start)) return end.plusDays(1);
        return end;
    }

    private LocalDateTime findStartTimeForShiftId(long shiftId) {
        int row = findRowByShiftId(shiftId);
        if (row < 0) return null;
        Object startObj = dayModel.getValueAt(row, 2);
        if (startObj == null) return null;
        String text = String.valueOf(startObj).trim();
        if (text.isEmpty()) return null;
        try {
            return LocalDateTime.parse(text, dtf);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDateTime parseDateTimeFromSelectedDate(String value, LocalDate date, String fieldName) {
        String text = value == null ? "" : value.trim();
        try {
            LocalTime time = LocalTime.parse(text, tf);
            return LocalDateTime.of(date, time);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Невалиден формат за " + fieldName + ". Ползвай: HH:mm");
        }
    }

    private LocalDateTime parseOptionalDateTimeFromSelectedDate(String value, LocalDate date, String fieldName) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) return null;
        return parseDateTimeFromSelectedDate(text, date, fieldName);
    }

    private String extractTimeText(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return "";
        try {
            LocalDateTime dt = LocalDateTime.parse(text, dtf);
            return tf.format(dt.toLocalTime());
        } catch (DateTimeParseException ex) {
            return text;
        }
    }

    private void applyWindowIcon() {
        File iconFile = new File("hours.ico");
        if (!iconFile.exists()) return;
        Image icon = Toolkit.getDefaultToolkit().getImage(iconFile.getAbsolutePath());
        if (icon != null) {
            setIconImage(icon);
        }
    }
}

