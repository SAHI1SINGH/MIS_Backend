package com.tadobasolutions.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceCheckOutRequestDTO {

    private String employeeId;

    // Optional: for a past/specific date. If null → today IST
    private LocalDate date;

    // Optional remarks at check-out, e.g. "Left early for appointment"
    private String remarks;

    // ── Standalone checkout support (no app check-in exists) ──────────────
    // Flutter sends true after local_auth succeeds
    private boolean biometricVerified;

    // Required only when checking out remotely AND no app check-in exists
    private String remoteReason;
}