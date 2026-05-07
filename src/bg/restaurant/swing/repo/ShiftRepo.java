package bg.restaurant.swing.repo;

import bg.restaurant.swing.db.Db;
import bg.restaurant.swing.model.ShiftRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class ShiftRepo {
    public long createShift(long employeeId, LocalDateTime start, LocalDateTime end) throws SQLException {
        if (end == null) throw new SQLException("Краят на смяната е задължителен.");
        if (!end.isAfter(start)) throw new SQLException("Краят трябва да е след началото.");
        try (Connection c = Db.connect()) {
            validateNoOverlap(c, employeeId, start, end, null);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO shift(employee_id, start_time, end_time) VALUES (?,?,?)",
                    new String[]{"ID"})) {
                ps.setLong(1, employeeId);
                ps.setTimestamp(2, Timestamp.valueOf(start));
                ps.setTimestamp(3, Timestamp.valueOf(end));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Неуспешен запис на смяна.");
    }

    public long startShift(long employeeId, LocalDateTime start) throws SQLException {
        try (Connection c = Db.connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO shift(employee_id, start_time, end_time) VALUES (?,?,NULL)",
                     new String[]{"ID"})) {
            ps.setLong(1, employeeId);
            ps.setTimestamp(2, Timestamp.valueOf(start));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Неуспешен запис за начало.");
    }

    public void endShift(long shiftId, LocalDateTime end) throws SQLException {
        try (Connection c = Db.connect()) {
            try (PreparedStatement load = c.prepareStatement("SELECT employee_id, start_time FROM shift WHERE id=?")) {
                load.setLong(1, shiftId);
                try (ResultSet rs = load.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Смяната не е намерена.");
                    long employeeId = rs.getLong("employee_id");
                    LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
                    if (!end.isAfter(start)) throw new SQLException("Краят трябва да е след началото.");
                    validateNoOverlap(c, employeeId, start, end, shiftId);
                }
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE shift SET end_time=? WHERE id=?")) {
                ps.setTimestamp(1, Timestamp.valueOf(end));
                ps.setLong(2, shiftId);
                ps.executeUpdate();
            }
        }
    }

    public void updateShift(long shiftId, LocalDateTime start, LocalDateTime end) throws SQLException {
        try (Connection c = Db.connect()) {
            if (end != null && !end.isAfter(start)) throw new SQLException("Краят трябва да е след началото.");
            long employeeId;
            try (PreparedStatement load = c.prepareStatement("SELECT employee_id FROM shift WHERE id=?")) {
                load.setLong(1, shiftId);
                try (ResultSet rs = load.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Смяната не е намерена.");
                    employeeId = rs.getLong("employee_id");
                }
            }
            if (end != null) {
                validateNoOverlap(c, employeeId, start, end, shiftId);
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE shift SET start_time=?, end_time=? WHERE id=?")) {
                ps.setTimestamp(1, Timestamp.valueOf(start));
                if (end == null) {
                    ps.setNull(2, java.sql.Types.TIMESTAMP);
                } else {
                    ps.setTimestamp(2, Timestamp.valueOf(end));
                }
                ps.setLong(3, shiftId);
                ps.executeUpdate();
            }
        }
    }

    public void deleteShift(long shiftId) throws SQLException {
        try (Connection c = Db.connect();
             PreparedStatement ps = c.prepareStatement("DELETE FROM shift WHERE id=?")) {
            ps.setLong(1, shiftId);
            ps.executeUpdate();
        }
    }

    public List<ShiftRow> listForDay(LocalDate day) throws SQLException {
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();
        return listBetween(from, to, null);
    }

    public List<ShiftRow> listForDay(LocalDate day, Long employeeId) throws SQLException {
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();
        return listBetween(from, to, employeeId);
    }

    public List<ShiftRow> listForMonth(YearMonth month) throws SQLException {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        return listBetween(from, to, null);
    }

    public List<ShiftRow> listForMonth(YearMonth month, Long employeeId) throws SQLException {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        return listBetween(from, to, employeeId);
    }

    private List<ShiftRow> listBetween(LocalDateTime from, LocalDateTime to, Long employeeId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT s.id, s.employee_id, e.name AS employee_name, s.start_time, s.end_time " +
                        "FROM shift s " +
                        "JOIN employee e ON e.id = s.employee_id " +
                        "WHERE s.start_time >= ? AND s.start_time < ? "
        );
        if (employeeId != null) {
            sql.append("AND s.employee_id = ? ");
        }
        sql.append("ORDER BY s.start_time DESC");
        try (Connection c = Db.connect();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            if (employeeId != null) {
                ps.setLong(3, employeeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<ShiftRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new ShiftRow(
                            rs.getLong("id"),
                            rs.getLong("employee_id"),
                            rs.getString("employee_name"),
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time") == null ? null : rs.getTimestamp("end_time").toLocalDateTime()
                    ));
                }
                return out;
            }
        }
    }

    private void validateNoOverlap(Connection c, long employeeId, LocalDateTime start, LocalDateTime end, Long excludeShiftId) throws SQLException {
        String sql =
                "SELECT start_time, end_time FROM shift " +
                "WHERE employee_id = ? " +
                "AND (? IS NULL OR id <> ?) " +
                "AND start_time < ? " +
                "AND COALESCE(end_time, TIMESTAMP '9999-12-31 23:59:59') > ? " +
                "LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            if (excludeShiftId == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, excludeShiftId);
                ps.setLong(3, excludeShiftId);
            }
            ps.setTimestamp(4, Timestamp.valueOf(end));
            ps.setTimestamp(5, Timestamp.valueOf(start));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime conflictStart = rs.getTimestamp("start_time").toLocalDateTime();
                    java.sql.Timestamp conflictEndTs = rs.getTimestamp("end_time");
                    String conflictEndText = conflictEndTs == null ? "без край" : conflictEndTs.toLocalDateTime().toString().replace('T', ' ');
                    String conflictStartText = conflictStart.toString().replace('T', ' ');
                    throw new SQLException("Смяната се припокрива със съществуваща смяна: " + conflictStartText + " - " + conflictEndText);
                }
            }
        }
    }
}

