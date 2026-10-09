package com.tadobasolutions.dto.response;

import com.tadobasolutions.entity.enums.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrentlyOnLeaveDTO {
    private String employeeId;
    private String employeeName;
    private LeaveType leaveType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private int totalDays;
    private int daysElapsed;   // includes today
    private int daysRemaining; // including today, days left before returning
}
