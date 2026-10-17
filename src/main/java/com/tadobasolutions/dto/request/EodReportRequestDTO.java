package com.tadobasolutions.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Body for submitting an End-of-Day report.
 *
 * INCHARGE  (POST /api/standup/eod)
 *   send: employeeId, items, summary fields
 *   departmentId / departmentName / reportDate are IGNORED —
 *   backend takes department from the Employee record and date = today (IST).
 *
 * ADMIN     (POST /api/standup/eod/admin)
 *   send: departmentId + departmentName (from dropdown), items, summary fields
 *   optional: employeeId (admin's own id, for "submitted by"), reportDate (defaults to today)
 */
@Data
public class EodReportRequestDTO {

    private String employeeId;

    // admin only: shown as "submitted by" when the admin is not found in Employee collection
    private String submittedByName;

    // admin only
    private String departmentId;
    private String departmentName;
    private LocalDate reportDate;

    // table rows (max 15)
    private List<Item> items;

    // End-of-Day Summary
    private String keyAchievements;
    private String challenges;
    private String supportRequired;
    private String tomorrowPlan;

    @Data
    public static class Item {
        private String workParticulars;
        private String completedBy;
        private String resultOrFollowUp;
    }
}