package com.tadobasolutions.dto.request;

import com.tadobasolutions.entity.enums.AttendanceType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AttendanceRequestDTO {

    private String employeeId;

    // Optional: for a past/specific date (admin correction). If null → today IST
    private LocalDate date;

    private AttendanceType type;   // PRESENT / LATE / WFH / ON_DUTY_OUTSIDE / MANUAL

    private String reason;         // Short reason (required if type != PRESENT)
    private String remarks;        // Optional free-text remarks (check-in remarks)

    // ── Admin-only: allows setting a check-out at the same time as a manual entry ──
    private LocalDateTime checkOutTime;
    private String checkOutRemarks;

    private boolean biometricVerified;   // Flutter sends true after local_auth succeeds
    private String  remoteReason;
}