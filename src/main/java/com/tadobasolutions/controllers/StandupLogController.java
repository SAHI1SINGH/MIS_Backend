package com.tadobasolutions.controllers;

import com.tadobasolutions.dto.request.EodRemarksRequestDTO;
import com.tadobasolutions.dto.request.EodReportRequestDTO;
import com.tadobasolutions.entity.StandupLog;
import com.tadobasolutions.exception.BadRequestException;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.service.StandupLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/standup")
@RequiredArgsConstructor
@CrossOrigin
public class StandupLogController {

    private final StandupLogService standupService;

    // ═════════════════════════════════════════════════════════════════════════
    // DEPARTMENT INCHARGE (Flutter)
    // ═════════════════════════════════════════════════════════════════════════

    // POST /api/standup
    // Body: { employeeId,
    //         items:[{ workParticulars, completedBy, resultOrFollowUp }],   (max 15)
    //         keyAchievements, challenges, supportRequired, tomorrowPlan }
    // Date = today (fixed), department = incharge's own — both set by backend
    @PostMapping
    public ResponseEntity<?> submit(@RequestBody EodReportRequestDTO dto) {
        return handle(() -> standupService.submitByIncharge(dto));
    }

    // GET /api/standup/today/{employeeId}
    // Flutter calls on screen open -> { submitted: true/false, report: {...} | null }
    @GetMapping("/today/{employeeId}")
    public ResponseEntity<?> today(@PathVariable String employeeId) {
        return handle(() -> {
            StandupLog report = standupService.getToday(employeeId).orElse(null);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("submitted", report != null);
            res.put("report", report);
            return res;
        });
    }

    // GET /api/standup/employee/{employeeId}
    // Incharge's department history — star ratings included
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> history(@PathVariable String employeeId) {
        return handle(() -> standupService.getHistory(employeeId));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ADMIN (Angular)
    // ═════════════════════════════════════════════════════════════════════════

    // POST /api/standup/admin
    // Same body as incharge + departmentId, departmentName (dropdown), optional reportDate
    @PostMapping("/admin")
    public ResponseEntity<?> submitAsAdmin(@RequestBody EodReportRequestDTO dto) {
        return handle(() -> standupService.submitByAdmin(dto));
    }

    // GET /api/standup/date/2026-10-17?departmentId=abc   (departmentId optional)
    @GetMapping("/date/{date}")
    public ResponseEntity<?> byDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String departmentId) {
        return handle(() -> standupService.getByDate(date, departmentId));
    }

    // GET /api/standup/admin/excel?from=2026-10-17&to=2026-10-17   (both optional, default today)
    // Downloads .xlsx — ALL departments in one sheet, filter on every column
    @GetMapping("/admin/excel")
    public ResponseEntity<?> downloadExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            byte[] bytes = standupService.exportExcel(from, to);

            LocalDate f = from != null ? from : LocalDate.now();
            String name = "EOD_Reports_" + f + (to != null && !to.equals(f) ? "_to_" + to : "") + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", String.valueOf(e.getMessage()), "error", String.valueOf(e.getMessage())));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", String.valueOf(e.getMessage()), "error", String.valueOf(e.getMessage())));
        }
    }

    // PATCH /api/standup/admin/{id}/remarks
    // Body: { remarkedBy, remarks:[{ srNo, rating(0-5), remarkText }] }
    // Returns full updated report — same JSON admin and employee both read
    @PatchMapping("/admin/{id}/remarks")
    public ResponseEntity<?> remarks(@PathVariable String id,
                                     @RequestBody EodRemarksRequestDTO dto) {
        return handle(() -> standupService.addRemarks(id, dto));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BOTH
    // ═════════════════════════════════════════════════════════════════════════

    // GET /api/standup/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable String id) {
        return handle(() -> standupService.getById(id));
    }

    // ── helper: { "message": "...", "error": "..." } — Flutter ApiClient reads "message" ──
    private ResponseEntity<?> handle(Supplier<Object> action) {
        try {
            return ResponseEntity.ok(action.get());
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", String.valueOf(e.getMessage()), "error", String.valueOf(e.getMessage())));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(Map.of("message", String.valueOf(e.getMessage()), "error", String.valueOf(e.getMessage())));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", String.valueOf(e.getMessage()), "error", String.valueOf(e.getMessage())));
        }
    }
}