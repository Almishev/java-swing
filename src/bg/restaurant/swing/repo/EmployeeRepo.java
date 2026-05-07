package bg.restaurant.swing.repo;

import bg.restaurant.swing.db.Db;
import bg.restaurant.swing.model.Employee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class EmployeeRepo {
    public List<Employee> list() throws SQLException {
        try (Connection c = Db.connect();
             PreparedStatement ps = c.prepareStatement("SELECT id, name FROM employee ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            List<Employee> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Employee(rs.getLong("id"), rs.getString("name")));
            }
            return out;
        }
    }

    public Employee create(String name) throws SQLException {
        String cleaned = name == null ? "" : name.trim();
        if (cleaned.isEmpty()) throw new SQLException("Името е празно.");

        try (Connection c = Db.connect();
             PreparedStatement ps = c.prepareStatement("INSERT INTO employee(name) VALUES (?)", new String[]{"ID"})) {
            ps.setString(1, cleaned);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return new Employee(keys.getLong(1), cleaned);
            }
        }
        // Fallback: re-read
        try (Connection c = Db.connect();
             PreparedStatement ps = c.prepareStatement("SELECT id, name FROM employee WHERE name=?")) {
            ps.setString(1, cleaned);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Employee(rs.getLong("id"), rs.getString("name"));
            }
        }
        throw new SQLException("Неуспешно добавяне на служител.");
    }

    public void delete(long id) throws SQLException {
        try (Connection c = Db.connect();
             PreparedStatement ps = c.prepareStatement("DELETE FROM employee WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}

