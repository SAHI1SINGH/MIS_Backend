package com.tadobasolutions.controllers;

import com.tadobasolutions.entity.StandupLog;
import com.tadobasolutions.service.StandupLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/standup")
@RequiredArgsConstructor
@CrossOrigin
public class StandupLogController {

    private final StandupLogService standupService;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/standup
    // Employee submits a new work entry — no daily limit
    // Body: { employeeId, employeeName, workType, doingToday, blockers }
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> submit(@RequestBody StandupLog log) {
        try {
            // basic validation
            if (log.getEmployeeId() == null || log.getEmployeeId().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "employeeId is required"));
            }
            if (log.getDoingToday() == null || log.getDoingToday().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "doingToday is required"));
            }
            StandupLog saved = standupService.submitStandup(log);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/standup/check/{employeeId}
    // Flutter calls on screen open — returns { submitted: true, count: 3 }
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/check/{employeeId}")
    public ResponseEntity<Map<String, Object>> checkToday(
            @PathVariable String employeeId
    ) {
        return ResponseEntity.ok(standupService.getTodayStatus(employeeId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/standup/today/{employeeId}
    // Flutter shows today's entries on the standup screen
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/today/{employeeId}")
    public ResponseEntity<List<StandupLog>> getTodayLogs(
            @PathVariable String employeeId
    ) {
        return ResponseEntity.ok(standupService.getTodayLogs(employeeId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/standup/employee/{employeeId}
    // Employee sees their full history — all dates, all entries
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<StandupLog>> getByEmployee(
            @PathVariable String employeeId
    ) {
        return ResponseEntity.ok(standupService.getByEmployee(employeeId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/standup/date/2026-06-22
    // Admin sees ALL employees' entries for a given date
    // Angular admin panel calls this with date picker
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/date/{date}")
    public ResponseEntity<List<StandupLog>> getByDate(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(standupService.getByDate(date));
    }
}