package bg.restaurant.swing.model;

public class AppUser {
    private final long id;
    private final String accessCode;
    private final boolean admin;
    private final String displayName;
    private final Long employeeId;

    public AppUser(long id, String accessCode, boolean admin, String displayName, Long employeeId) {
        this.id = id;
        this.accessCode = accessCode;
        this.admin = admin;
        this.displayName = displayName;
        this.employeeId = employeeId;
    }

    public long id() {
        return id;
    }

    public String accessCode() {
        return accessCode;
    }

    public boolean isAdmin() {
        return admin;
    }

    public String displayName() {
        return displayName;
    }

    public Long employeeId() {
        return employeeId;
    }
}
