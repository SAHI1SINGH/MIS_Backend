package com.tadobasolutions.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * End-of-Day Reporting: Work & Follow-Up Report.
 * One report per department per date.
 */
@Data
@Document(collection = "standup_logs")
public class StandupLog {

    @Id
    private String id;

    // ── Header: Date + Department ────────────────────────────────────────────
    // Date: incharge -> always today (fixed by backend), admin -> optional
    @Indexed
    private LocalDate standupDate;

    // Department: incharge -> own department (auto), admin -> dropdown
    @Indexed
    private String departmentId;
    private String departmentName;

    // ── Who filled it ────────────────────────────────────────────────────────
    private String submittedById;
    private String submittedByName;
    private boolean submittedByAdmin;
    private LocalDateTime submittedAt;

    // ── Table (max 15 rows) ──────────────────────────────────────────────────
    private List<EodItem> items = new ArrayList<>();

    // ── End-of-Day Summary ───────────────────────────────────────────────────
    private String keyAchievements;     // Key achievements today
    private String challenges;          // Challenges / Issues faced
    private String supportRequired;     // Support required from management
    private String tomorrowPlan;        // Planned work for tomorrow

    // ── Admin remarks meta ───────────────────────────────────────────────────
    private String remarkedBy;
    private LocalDateTime remarkedAt;

    // ── One row of the table ─────────────────────────────────────────────────
    @Data
    public static class EodItem {
        private Integer srNo;               // S. No. (set by backend)
        private String workParticulars;     // Work Particulars / Tasks
        private String completedBy;         // Completed By
        private String resultOrFollowUp;    // Results / Pending / Follow-Up

        // Remarks — filled by admin only.
        // 0 = all 5 stars colorless, 4 = first 4 stars colored
        private Integer rating = 0;
        private String remarkText;
    }
}