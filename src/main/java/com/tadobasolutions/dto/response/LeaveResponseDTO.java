package com.tadobasolutions.dto.response;

import com.tadobasolutions.entity.enums.LeaveStatus;
import com.tadobasolutions.entity.enums.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaveResponseDTO {
    private String id;
    private String employeeId;
    private String employeeName;
    private LeaveType leaveType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private int totalDays;
    private String reason;
    private LeaveStatus status;
    private LocalDateTime appliedAt;
    private String approverId;
    private String approverName;
    private LocalDateTime actionAt;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

