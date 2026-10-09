package com.tadobasolutions.dto.response;

import com.tadobasolutions.entity.enums.AttendanceType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AttendanceResponseDTO {
    private String id;
    private String employeeId;
    private String employeeName;
    private String departmentId;
    private String departmentName;
    private LocalDate date;
    private LocalDateTime markedAt;     // check-in time
    private AttendanceType type;
    private String reason;
    private String remarks;
    private boolean lateEntry;

    private LocalDateTime checkOutTime;
    private String checkOutRemarks;
    private boolean checkedOut;
    private Double workingHours;

    private String adminNote;
    private LocalDateTime createdAt;

    private boolean biometricVerified;
    private boolean isOfficeNetwork;
}