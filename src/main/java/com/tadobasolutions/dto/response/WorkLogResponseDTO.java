package com.tadobasolutions.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class WorkLogResponseDTO {

    private String id;
    private String employeeId;
    private String employeeName;
    private String departmentName;
    private LocalDate date;
    private LocalDateTime submittedAt;

    private List<String> tasksCompleted;
    private String workSummary;
    private String challenges;
    private String tomorrowPlan;

    private int tasksDoneCount;       // auto-filled: Task docs COMPLETED today
    private boolean lateSubmission;   // true if submitted after 8 PM
    private String adminNote;
}
