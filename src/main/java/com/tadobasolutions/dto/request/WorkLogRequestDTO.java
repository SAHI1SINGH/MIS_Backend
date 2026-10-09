// ─────────────────────────────────────────────────────────────────────────────
// FILE: dto/request/WorkLogRequestDTO.java
// Employee sends this from the Flutter EOD screen
// ─────────────────────────────────────────────────────────────────────────────
package com.tadobasolutions.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class WorkLogRequestDTO {

    private String employeeId;

    // What did you finish today? (list of short task descriptions)
    private List<String> tasksCompleted;

    // One paragraph summary of today's work
    private String workSummary;

    // Any blocker or challenge
    private String challenges;

    // Plan for tomorrow
    private String tomorrowPlan;
}


