package com.tadobasolutions.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AttendanceSummaryDTO {
    private String employeeId;
    private String employeeName;
    private String departmentName;
    private int totalDays;
    private int presentCount;
    private int lateCount;
    private int wfhCount;
    private int onDutyOutsideCount;
    private int absentCount;
    private double attendancePercentage;
    private double totalWorkingHours;
    private double avgWorkingHours;
    private List<AttendanceResponseDTO> records;
}