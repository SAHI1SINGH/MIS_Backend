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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    // EMPLOYEE — identity always comes from the JWT, never from the request
    // ═════════════════════════════════════════════════════════════════════════

    // POST /api/standup — final submit, locks the report
    @PostMapping
    public ResponseEntity<?> submit(@RequestBody EodReportRequestDTO dto) {
        return handle(() -> {
            dto.setEmployeeId(currentUserId());       // ignore whatever the app sent
            return standupService.submitByEmployee(dto);
        });
    }

    // PUT /api/standup/draft — auto-save
    @PutMapping("/draft")
    public ResponseEntity<?> saveDraft(@RequestBody EodReportRequestDTO dto) {
        return handle(() -> {
            dto.setEmployeeId(currentUserId());
            return standupService.saveDraft(dto);
        });
    }

    // GET /api/standup/today/{employeeId}
    // Non-admin can only ask for themselves.
    @GetMapping("/today/{employeeId}")
    public ResponseEntity<?> today(@PathVariable String employeeId) {
        return handle(() -> {
            requireSelfOrAdmin(employeeId);
            StandupLog report = standupService.getToday(employeeId).orElse(null);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("exists", report != null);
            res.put("submitted", report != null && !report.isDraft());
            res.put("report", report);
            return res;
        });
    }

    // GET /api/standup/employee/{employeeId} — own submitted history
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> history(@PathVariable String employeeId) {
        return handle(() -> {
            requireSelfOrAdmin(employeeId);
            return standupService.getHistory(employeeId);
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ADMIN (also restricted to ADMIN in SecurityConfig)
    // ═════════════════════════════════════════════════════════════════════════

    @PostMapping("/admin")
    public ResponseEntity<?> submitAsAdmin(@RequestBody EodReportRequestDTO dto) {
        return handle(() -> {
            requireAdmin();
            return standupService.submitByAdmin(dto);
        });
    }

    // GET /api/standup/date/2026-10-17?departmentId=abc
    @GetMapping("/date/{date}")
    public ResponseEntity<?> byDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String departmentId) {
        return handle(() -> {
            requireAdmin();
            return standupService.getByDate(date, departmentId);
        });
    }

    // GET /api/standup/admin/excel?from=2026-10-17&to=2026-10-17
    @GetMapping("/admin/excel")
    public ResponseEntity<?> downloadExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            requireAdmin();
            byte[] bytes = standupService.exportExcel(from, to);

            LocalDate f = from != null ? from : LocalDate.now();
            String name = "EOD_Reports_" + f + (to != null && !to.equals(f) ? "_to_" + to : "") + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (AccessDeniedException e) {
            return forbidden(e);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(err(e));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(err(e));
        }
    }

    // PATCH /api/standup/admin/{id}/remarks
    @PatchMapping("/admin/{id}/remarks")
    public ResponseEntity<?> remarks(@PathVariable String id,
                                     @RequestBody EodRemarksRequestDTO dto) {
        return handle(() -> {
            requireAdmin();
            return standupService.addRemarks(id, dto);
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BOTH — owner or admin only
    // ═════════════════════════════════════════════════════════════════════════

    // GET /api/standup/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable String id) {
        return handle(() -> {
            StandupLog report = standupService.getById(id);
            if (!isAdmin() && !currentUserId().equals(report.getSubmittedById())) {
                throw new AccessDeniedException("You can only view your own report");
            }
            return report;
        });
    }

    private String currentUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a.getName() == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return standupService.resolveEmployeeId(a.getName());   // email -> Employee id
    }

    private boolean isAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream()
                .anyMatch(g -> "ROLE_ADMIN".equals(g.getAuthority()));
    }

    private void requireAdmin() {
        if (!isAdmin()) throw new AccessDeniedException("Admin only");
    }

    private void requireSelfOrAdmin(String employeeId) {
        if (!isAdmin() && !currentUserId().equals(employeeId)) {
            throw new AccessDeniedException("You can only access your own reports");
        }
    }

    // ── helper: { "message": "...", "error": "..." } — Flutter ApiClient reads "message" ──
    private Map<String, String> err(Exception e) {
        return Map.of("message", String.valueOf(e.getMessage()), "error", String.valueOf(e.getMessage()));
    }

    private ResponseEntity<?> forbidden(Exception e) {
        return ResponseEntity.status(403).body(err(e));
    }

    private ResponseEntity<?> handle(Supplier<Object> action) {
        try {
            return ResponseEntity.ok(action.get());
        } catch (AccessDeniedException e) {
            return forbidden(e);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(err(e));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(err(e));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(err(e));
        }
    }
}