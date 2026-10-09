package com.tadobasolutions.entity;

import com.tadobasolutions.entity.enums.LeaveStatus;
import com.tadobasolutions.entity.enums.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "leaves")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Leave {

    @Id
    private String id;
    private String employeeId;
    private String employeeName;
    private LeaveType leaveType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private int totalDays;
    private String reason;
    private LeaveStatus status = LeaveStatus.PENDING;
    private LocalDateTime appliedAt;
    private String approverId;
    private String approverName;
    private LocalDateTime actionAt;
    private String remarks;
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
