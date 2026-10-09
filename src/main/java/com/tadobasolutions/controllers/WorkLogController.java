package com.tadobasolutions.controllers;

import com.tadobasolutions.dto.request.WorkLogRequestDTO;
import com.tadobasolutions.dto.response.WorkLogResponseDTO;
import com.tadobasolutions.service.WorkLogService;
import com.tadobasolutions.utilities.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/worklog")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://tadobasolutions.com",
        "https://tadobasolutions.com",
        "http://mis.tadobasolutions.com",
        "https://mis.tadobasolutions.com"
})
@RequiredArgsConstructor
public class WorkLogController {

    private final WorkLogService workLogService;

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE endpoints
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/worklog/submit
     * Employee submits their EOD work log.
     * Flutter calls this when employee taps "Submit" on EOD screen.
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<WorkLogResponseDTO>> submit(
            @RequestBody WorkLogRequestDTO dto) {

        WorkLogResponseDTO result = workLogService.submitWorkLog(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<WorkLogResponseDTO>builder()
                        .data(result)
                        .message("Work log submitted successfully")
                        .statusCode(HttpStatus.CREATED.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build());
    }

    /**
     * GET /api/worklog/my/{employeeId}
     * Employee views their full log history.
     */
    @GetMapping("/my/{employeeId}")
    public ResponseEntity<ApiResponse<List<WorkLogResponseDTO>>> getMyLogs(
            @PathVariable String employeeId) {

        List<WorkLogResponseDTO> logs = workLogService.getMyLogs(employeeId);
        return ok(logs, "Logs fetched successfully");
    }

    /**
     * GET /api/worklog/my/{employeeId}/date?date=2025-06-17
     * Employee views their own log for a specific date.
     */
    @GetMapping("/my/{employeeId}/date")
    public ResponseEntity<ApiResponse<WorkLogResponseDTO>> getMyLogForDate(
            @PathVariable String employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        WorkLogResponseDTO log = workLogService.getMyLogForDate(employeeId, date);
        return ok(log, "Log fetched");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN / SUB-ADMIN endpoints
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/worklog/admin/inbox?date=2025-06-17
     * Admin views all submitted EOD logs for a given date.
     * If no date param → defaults to today.
     */
    @GetMapping("/admin/inbox")
    public ResponseEntity<ApiResponse<List<WorkLogResponseDTO>>> getInbox(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        List<WorkLogResponseDTO> logs = workLogService.getLogsForDate(targetDate);
        return ok(logs, "EOD inbox for " + targetDate + " — " + logs.size() + " submission(s)");
    }

    /**
     * GET /api/worklog/admin/employee/{employeeId}?from=2025-06-01&to=2025-06-30
     * Admin views one employee's logs across a date range.
     */
    @GetMapping("/admin/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<WorkLogResponseDTO>>> getEmployeeLogs(
            @PathVariable String employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<WorkLogResponseDTO> logs = workLogService.getEmployeeLogs(employeeId, from, to);
        return ok(logs, "Logs for employee " + employeeId);
    }

    /**
     * GET /api/worklog/admin/team?from=2025-06-01&to=2025-06-30
     * Admin views full team logs across a date range.
     */
    @GetMapping("/admin/team")
    public ResponseEntity<ApiResponse<List<WorkLogResponseDTO>>> getTeamLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<WorkLogResponseDTO> logs = workLogService.getTeamLogs(from, to);
        return ok(logs, "Team logs from " + from + " to " + to);
    }

    /**
     * PATCH /api/worklog/admin/{logId}/note
     * Admin adds a review note on an employee's EOD log.
     * Body: { "note": "Good work today!" }
     */
    @PatchMapping("/admin/{logId}/note")
    public ResponseEntity<ApiResponse<WorkLogResponseDTO>> addNote(
            @PathVariable String logId,
            @RequestBody Map<String, String> body) {

        String note = body.get("note");
        WorkLogResponseDTO updated = workLogService.addAdminNote(logId, note);
        return ok(updated, "Note added");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(
                ApiResponse.<T>builder()
                        .data(data)
                        .message(message)
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build());
    }
}
