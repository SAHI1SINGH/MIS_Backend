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
 * One report per EMPLOYEE per date (draft until submitted, then locked).
 */
@Data
@Document(collection = "standup_logs")
public class StandupLog {

    @Id
    private String id;

    // ── Header: Date + Department ────────────────────────────────────────────
    @Indexed
    private LocalDate standupDate;

    @Indexed
    private String departmentId;
    private String departmentName;

    // ── Who filled it ────────────────────────────────────────────────────────
    @Indexed
    private String submittedById;
    private String submittedByName;
    private boolean submittedByAdmin;
    private LocalDateTime submittedAt;

    // ── Draft / lock state ───────────────────────────────────────────────────
    // true  = auto-saved draft (editable)
    // false = submitted & locked. Old documents have no such field -> false.
    private boolean draft;
    private LocalDateTime draftSavedAt;

    // ── Table (max 15 rows) ──────────────────────────────────────────────────
    private List<EodItem> items = new ArrayList<>();

    // ── End-of-Day Summary ───────────────────────────────────────────────────
    private String keyAchievements;
    private String challenges;
    private String supportRequired;
    private String tomorrowPlan;

    // ── Admin remarks meta ───────────────────────────────────────────────────
    private String remarkedBy;
    private LocalDateTime remarkedAt;
    private boolean autoSubmitted;
    // ── One row of the table ─────────────────────────────────────────────────
    @Data
    public static class EodItem {
        private Integer srNo;
        private String workParticulars;
        private String completedBy;
        private String resultOrFollowUp;

        // Remarks — filled by admin only. 0 = all 5 stars colorless
        private Integer rating = 0;
        private String remarkText;
    }
}