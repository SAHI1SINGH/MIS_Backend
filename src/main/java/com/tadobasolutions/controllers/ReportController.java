package com.tadobasolutions.controllers;

import com.tadobasolutions.dto.response.ReportSummaryDTO;
import com.tadobasolutions.dto.response.TeamReportDTO;
import com.tadobasolutions.service.TaskService;
import com.tadobasolutions.utilities.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://tadobasolutions.com",
        "https://tadobasolutions.com",
        "http://mis.tadobasolutions.com",
        "https://mis.tadobasolutions.com"
}, exposedHeaders = {"Content-Disposition"})
@RequiredArgsConstructor
public class ReportController {

    private final TaskService taskService;

    // ── Existing: individual employee monthly summary ─────────────────────────
    @GetMapping("/employee-summary")
    public ResponseEntity<ApiResponse<ReportSummaryDTO>> getMonthlySummary(
            @RequestParam String employeeId,
            @RequestParam int month,
            @RequestParam int year) {

        ReportSummaryDTO summary = taskService.getMonthlyReportSummary(employeeId, month, year);

        return ResponseEntity.ok(
                ApiResponse.<ReportSummaryDTO>builder()
                        .data(summary)
                        .message("Monthly report summary fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    // ── Existing: individual employee Excel download ───────────────────────────
    @GetMapping("/employee-excel")
    public ResponseEntity<byte[]> downloadExcel(
            @RequestParam String employeeId,
            @RequestParam int month,
            @RequestParam int year) {

        byte[] excelData   = taskService.exportMonthlyReportExcel(employeeId, month, year);
        String employeeName = taskService.getEmployeeNameById(employeeId);
        String cleanName   = employeeName.trim().toLowerCase().replace(" ", "_");
        String monthName   = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        String filename    = "employee_report_" + cleanName + "_" + monthName + "_" + year + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }

    // ── NEW: admin team-wide performance report (JSON) ────────────────────────
    /**
     * GET /api/report/team-summary?month=5&year=2025
     * Returns full team performance data for the Flutter admin report screen.
     * Requires ADMIN role.
     */
    @GetMapping("/team-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TeamReportDTO>> getTeamSummary(
            @RequestParam int month,
            @RequestParam int year) {

        TeamReportDTO report = taskService.getTeamReport(month, year);

        return ResponseEntity.ok(
                ApiResponse.<TeamReportDTO>builder()
                        .data(report)
                        .message("Team performance report fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    // ── NEW: admin team Excel export ──────────────────────────────────────────
    /**
     * GET /api/report/team-excel?month=5&year=2025
     * Downloads a 3-sheet Excel: Summary | Employee Breakdown | Department Breakdown
     * Requires ADMIN role.
     */
    @GetMapping("/team-excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadTeamExcel(
            @RequestParam int month,
            @RequestParam int year) {

        byte[] excelData = taskService.exportTeamReportExcel(month, year);
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        String filename  = "team_report_" + monthName + "_" + year + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }
}