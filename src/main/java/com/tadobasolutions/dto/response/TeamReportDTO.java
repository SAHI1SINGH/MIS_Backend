package com.tadobasolutions.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Admin-facing response: full team performance snapshot for a given month/year.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamReportDTO {

    // ── Team-level aggregates ────────────────────────────────────────────────
    private long   totalTasks;
    private long   totalCompleted;
    private long   totalPending;
    private long   totalLate;
    private long   totalOverdue;
    private double teamCompletionRate;   // % (0-100)
    private double teamAvgAdminRating;   // avg across all reviewed tasks
    private int    totalEmployees;

    // ── Per-employee breakdown (sorted by score desc) ────────────────────────
    private List<EmployeePerformanceSummary> employees;

    // ── Per-department breakdown ─────────────────────────────────────────────
    private List<DepartmentPerformanceSummary> departments;

    // ────────────────────────────────────────────────────────────────────────
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class EmployeePerformanceSummary {
        private String employeeId;
        private String employeeName;
        private String departmentName;
        private long   totalTasks;
        private long   completed;
        private long   pending;
        private long   late;
        private long   overdue;
        private double completionRate;    // %
        private double avgEmpRating;
        private double avgAdminRating;
        private double performanceScore;  // weighted composite 0-100
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class DepartmentPerformanceSummary {
        private String departmentId;
        private String departmentName;
        private long   totalTasks;
        private long   completed;
        private double completionRate;
    }
}