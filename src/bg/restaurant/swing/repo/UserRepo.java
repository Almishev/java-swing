package bg.restaurant.swing.repo;

import bg.restaurant.swing.db.Db;
import bg.restaurant.swing.model.AppUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class UserRepo {
    public AppUser findByCode(String code) throws SQLException {
        try (Connection c = Db.connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, access_code, is_admin, display_name, employee_id FROM app_user WHERE access_code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                long userId = rs.getLong("id");
                boolean isAdmin = rs.getBoolean("is_admin");
                String displayName = rs.getString("display_name");
                Object employeeValue = rs.getObject("employee_id");
                Long employeeId = employeeValue == null ? null : rs.getLong("employee_id");
                if (!isAdmin && employeeId == null) {
                    employeeId = ensureEmployeeLink(c, userId, displayName);
                }
                return new AppUser(
                        userId,
                        rs.getString("access_code"),
                        isAdmin,
                        displayName,
                        employeeId
                );
            }
        }
    }

    public void createUser(String code, String displayName, boolean isAdmin) throws SQLException {
        String cleaned = code == null ? "" : code.trim();
        String cleanedName = displayName == null ? "" : displayName.trim();
        if (cleaned.isEmpty()) throw new SQLException("Кодът е празен.");
        if (!isNumeric(cleaned)) throw new SQLException("Кодът трябва да е само от цифри.");
        if (cleanedName.isEmpty()) throw new SQLException("Името е празно.");

        try (Connection c = Db.connect();
             PreparedStatement existsByCode = c.prepareStatement("SELECT 1 FROM app_user WHERE access_code = ?");
             PreparedStatement findEmployee = c.prepareStatement("SELECT id FROM employee WHERE name=?");
             PreparedStatement createEmployee = c.prepareStatement("INSERT INTO employee(name) VALUES (?)", new String[]{"ID"});
             PreparedStatement insertUser = c.prepareStatement(
                     "INSERT INTO app_user(access_code, is_admin, display_name, employee_id) VALUES (?, ?, ?, ?)")) {
            c.setAutoCommit(false);
            try {
                existsByCode.setString(1, cleaned);
                try (ResultSet rs = existsByCode.executeQuery()) {
                    if (rs.next()) {
                        throw new SQLException("Потребител с този код вече съществува.");
                    }
                }

                Long employeeId = null;
                if (!isAdmin) {
                    findEmployee.setString(1, cleanedName);
                    try (ResultSet rs = findEmployee.executeQuery()) {
                        if (rs.next()) {
                            employeeId = rs.getLong("id");
                        }
                    }
                    if (employeeId == null) {
                        createEmployee.setString(1, cleanedName);
                        createEmployee.executeUpdate();
                        try (ResultSet keys = createEmployee.getGeneratedKeys()) {
                            if (keys.next()) {
                                employeeId = keys.getLong(1);
                            }
                        }
                    }
                }

                insertUser.setString(1, cleaned);
                insertUser.setBoolean(2, isAdmin);
                insertUser.setString(3, cleanedName);
                if (employeeId == null) {
                    insertUser.setObject(4, null);
                } else {
                    insertUser.setLong(4, employeeId);
                }
                insertUser.executeUpdate();
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                if (ex instanceof SQLException) {
                    String msg = ex.getMessage();
                    if (msg != null && msg.toLowerCase().contains("idx_app_user_code")) {
                        throw new SQLException("Потребител с този код вече съществува.");
                    }
                    throw (SQLException) ex;
                }
                throw new SQLException("Неуспешно създаване на потребител.", ex);
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private static boolean isNumeric(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
        }
        return true;
    }

    private Long ensureEmployeeLink(Connection c, long userId, String displayName) throws SQLException {
        String cleanedName = displayName == null ? "" : displayName.trim();
        if (cleanedName.isEmpty()) return null;

        Long employeeId = null;
        try (PreparedStatement findEmployee = c.prepareStatement("SELECT id FROM employee WHERE name=?")) {
            findEmployee.setString(1, cleanedName);
            try (ResultSet employeeRs = findEmployee.executeQuery()) {
                if (employeeRs.next()) {
                    employeeId = employeeRs.getLong("id");
                }
            }
        }

        if (employeeId == null) {
            try (PreparedStatement createEmployee = c.prepareStatement("INSERT INTO employee(name) VALUES (?)", new String[]{"ID"})) {
                createEmployee.setString(1, cleanedName);
                createEmployee.executeUpdate();
                try (ResultSet keys = createEmployee.getGeneratedKeys()) {
                    if (keys.next()) {
                        employeeId = keys.getLong(1);
                    }
                }
            }
        }

        if (employeeId != null) {
            try (PreparedStatement linkUser = c.prepareStatement("UPDATE app_user SET employee_id=? WHERE id=?")) {
                linkUser.setLong(1, employeeId);
                linkUser.setLong(2, userId);
                linkUser.executeUpdate();
            }
        }
        return employeeId;
    }
}
