package com.tadobasolutions.entity;

import com.tadobasolutions.entity.enums.AttendanceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "attendances")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Attendance {

    @Id
    private String id;

    private String employeeId;
    private String employeeName;
    private String departmentId;
    private String departmentName;

    // Date of attendance (the actual working day)
    private LocalDate date;

    // Timestamp when the employee checked in
    private LocalDateTime markedAt;

    // PRESENT / LATE / WORK_FROM_HOME / ON_DUTY_OUTSIDE / MANUAL
    private AttendanceType type;

    // Reason given at check-in
    private String reason;

    // Extra notes given at check-in
    private String remarks;

    // true = employee checked in after 10:05 AM IST
    private boolean lateEntry;

    // ── Check-out fields ───────────────────────────────────────────────
    private LocalDateTime checkOutTime;   // null until checked out
    private String checkOutRemarks;       // optional notes at check-out
    private boolean checkedOut;           // true once checked out
    private Double workingHours;          // computed: hours between check-in & check-out

    // Admin can add a review note
    private String adminNote;

    @CreatedDate
    private LocalDateTime createdAt;

    private boolean biometricVerified;
    private String  clientIp;
}