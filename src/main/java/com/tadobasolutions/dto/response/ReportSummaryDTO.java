package com.tadobasolutions.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ReportSummaryDTO.java — rename two fields
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportSummaryDTO {
    private long totalTasks;
    private long completed;
    private long pending;
    private long lateSubmission;
    private long overdue;
    private double avgEmployeeRating;
    private double avgAdminRating;
    private double workPercentage;
    private double adminScore;
}
