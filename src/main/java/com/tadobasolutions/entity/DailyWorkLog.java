package com.tadobasolutions.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One document per employee per day.
 * Employee fills this at end-of-day (EOD).
 * Admin/Sub-Admin can read it to see what everyone did
 * without any surveillance camera.
 */
@Document(collection = "daily_work_logs")
@CompoundIndex(name = "emp_date_unique", def = "{'employeeId': 1, 'date': 1}", unique = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyWorkLog {

    @Id
    private String id;

    // ── Who ──────────────────────────────────────────────────────────────────
    private String employeeId;
    private String employeeName;
    private String departmentId;
    private String departmentName;

    // ── When ─────────────────────────────────────────────────────────────────
    private LocalDate date;                  // the working day this log belongs to
    private LocalDateTime submittedAt;       // exact time employee hit "Submit"

    // ── What employee did today ───────────────────────────────────────────────
    private List<String> tasksCompleted;     // free-text list: "Finished login screen", etc.
    private String workSummary;             // one paragraph — what I worked on today
    private String challenges;             // any blocker or difficulty faced
    private String tomorrowPlan;           // what I plan to do tomorrow

    // ── Auto-filled by backend ────────────────────────────────────────────────
    private int tasksDoneCount;            // count of Task docs marked COMPLETED today
    private boolean lateSubmission;        // true if submitted after 8 PM IST

    // Admin can add a quick note on this log
    private String adminNote;

    @CreatedDate
    private LocalDateTime createdAt;
}
