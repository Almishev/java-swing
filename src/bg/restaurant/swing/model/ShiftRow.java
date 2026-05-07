package bg.restaurant.swing.model;

import java.time.LocalDateTime;

public class ShiftRow {
    private final long id;
    private final long employeeId;
    private final String employeeName;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    public ShiftRow(long id,
                    long employeeId,
                    String employeeName,
                    LocalDateTime startTime,
                    LocalDateTime endTime) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long id() {
        return id;
    }

    public long employeeId() {
        return employeeId;
    }

    public String employeeName() {
        return employeeName;
    }

    public LocalDateTime startTime() {
        return startTime;
    }

    public LocalDateTime endTime() {
        return endTime;
    }
}

