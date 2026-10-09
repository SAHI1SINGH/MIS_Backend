package com.tadobasolutions.controllers;

import com.tadobasolutions.dto.request.AttendanceCheckOutRequestDTO;
import com.tadobasolutions.dto.request.AttendanceRequestDTO;
import com.tadobasolutions.dto.response.AttendanceResponseDTO;
import com.tadobasolutions.dto.response.AttendanceSummaryDTO;
import com.tadobasolutions.service.AttendanceService;
import com.tadobasolutions.utilities.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://tadobasolutions.com",
        "https://tadobasolutions.com",
        "http://mis.tadobasolutions.com",
        "https://mis.tadobasolutions.com"
})
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    // ── Employee: network check (Flutter pre-flight before biometric scan) ────
    //
    // Flutter calls this FIRST before showing the fingerprint dialog.
    // Response: { "isOfficeNetwork": true/false, "ip": "x.x.x.x" }
    // - isOfficeNetwork=true  → fingerprint only, no reason popup
    // - isOfficeNetwork=false → fingerprint + reason popup

    @GetMapping("/network-check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> networkCheck(
            HttpServletRequest request) {
        boolean isOffice = attendanceService.isOfficeNetwork(request);
        String  ip       = getClientIp(request);
        log.info("[NetworkCheck] ip={} isOfficeNetwork={}", ip, isOffice); // ← add this
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .data(Map.of("isOfficeNetwork", isOffice, "ip", ip))
                .message("Network check complete")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build());
    }

    // ── Employee: check in ────────────────────────────────────────────────────
    //
    // Now accepts HttpServletRequest so the service can read the server-side IP.
    // For biometric attendance, Flutter sends:
    //   { "type": "BIOMETRIC",        "biometricVerified": true }                  ← office WiFi
    //   { "type": "BIOMETRIC_REMOTE", "biometricVerified": true, "remoteReason": "WFH" } ← off WiFi

    @PostMapping("/mark")
    public ResponseEntity<ApiResponse<AttendanceResponseDTO>> mark(
            @RequestBody AttendanceRequestDTO dto,
            HttpServletRequest request) {
        AttendanceResponseDTO result = attendanceService.markAttendance(dto, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<AttendanceResponseDTO>builder()
                        .data(result)
                        .message("Checked in successfully")
                        .statusCode(HttpStatus.CREATED.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build());
    }

    // ── Employee: check out ───────────────────────────────────────────────────

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<AttendanceResponseDTO>> checkOut(
            @RequestBody AttendanceCheckOutRequestDTO dto,
            HttpServletRequest request) {
        AttendanceResponseDTO result = attendanceService.checkOutAttendance(dto, request);
        return ResponseEntity.ok(ApiResponse.<AttendanceResponseDTO>builder()
                .data(result)
                .message("Checked out successfully")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build());
    }

    // ── Employee: check if already checked in today ───────────────────────────

    @GetMapping("/check/{employeeId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkToday(
            @PathVariable String employeeId) {
        boolean marked = attendanceService.hasMarkedToday(employeeId);
        return ResponseEntity.ok(ApiResponse.<Map<String, Boolean>>builder()
                .data(Map.of("marked", marked))
                .message("Check complete")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build());
    }

    // ── Employee: check if already checked out today ──────────────────────────

    @GetMapping("/checkout-status/{employeeId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkOutStatus(
            @PathVariable String employeeId) {
        boolean checkedOut = attendanceService.hasCheckedOutToday(employeeId);
        return ResponseEntity.ok(ApiResponse.<Map<String, Boolean>>builder()
                .data(Map.of("checkedOut", checkedOut))
                .message("Check complete")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build());
    }

    // ── Employee: my history (month/year) ─────────────────────────────────────

    @GetMapping("/my/{employeeId}")
    public ResponseEntity<ApiResponse<List<AttendanceResponseDTO>>> myHistory(
            @PathVariable String employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        List<AttendanceResponseDTO> list =
                attendanceService.getMyAttendance(employeeId, month, year);
        return ResponseEntity.ok(ApiResponse.<List<AttendanceResponseDTO>>builder()
                .data(list)
                .message("Attendance records fetched")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build());
    }

    // ── Employee: monthly summary ──────────────────────────────────────────────

    @GetMapping("/summary/{employeeId}")
    public ResponseEntity<ApiResponse<AttendanceSummaryDTO>> summary(
            @PathVariable String employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        AttendanceSummaryDTO summary =
                attendanceService.getMySummary(employeeId, month, year);
        return ResponseEntity.ok(ApiResponse.<AttendanceSummaryDTO>builder()
                .data(summary)
                .message("Summary fetched")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build());
    }

    // ── Admin: today's attendance ──────────────────────────────────────────────

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<AttendanceResponseDTO>>> today() {
        List<AttendanceResponseDTO> list = attendanceService.getTodayAttendance();
        return ResponseEntity.ok(ApiResponse.<List<AttendanceResponseDTO>>builder()
                .data(list)
                .message("Today's attendance fetched")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build());
    }

    // ── Admin: full month ──────────────────────────────────────────────────────

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<AttendanceResponseDTO>>> monthly(
            @RequestParam int month,
            @RequestParam int year) {
        List<AttendanceResponseDTO> list =
                attendanceService.getMonthlyAttendance(month, year);
        return ResponseEntity.ok(ApiResponse.<List<AttendanceResponseDTO>>builder()
                .data(list)
                .message("Monthly attendance fetched")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build());
    }

    // ── Admin: specific employee ───────────────────────────────────────────────

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<AttendanceResponseDTO>>> byEmployee(
            @PathVariable String employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        List<AttendanceResponseDTO> list =
                attendanceService.getEmployeeAttendance(employeeId, month, year);
        return ResponseEntity.ok(ApiResponse.<List<AttendanceResponseDTO>>builder()
                .data(list)
                .message("Employee attendance fetched")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build());
    }

    // ── Admin: add note ────────────────────────────────────────────────────────

    @PutMapping("/{id}/note")
    public ResponseEntity<ApiResponse<AttendanceResponseDTO>> addNote(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        AttendanceResponseDTO updated =
                attendanceService.addAdminNote(id, body.get("adminNote"));
        return ResponseEntity.ok(ApiResponse.<AttendanceResponseDTO>builder()
                .data(updated)
                .message("Note added")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build());
    }

    // ── Admin: manual / backdated mark ────────────────────────────────────────

    @PostMapping("/admin/mark")
    public ResponseEntity<ApiResponse<AttendanceResponseDTO>> adminMark(
            @RequestBody AttendanceRequestDTO dto) {
        AttendanceResponseDTO result = attendanceService.adminMarkAttendance(dto);
        return ResponseEntity.ok(ApiResponse.<AttendanceResponseDTO>builder()
                .data(result)
                .message("Attendance marked by admin")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build());
    }


    // ── Admin: download Excel ──────────────────────────────────────────────────

    @GetMapping("/download/excel")
    public void downloadExcel(
            @RequestParam int month,
            @RequestParam int year,
            HttpServletResponse response) throws Exception {
        attendanceService.downloadMonthlyExcel(month, year, response);
    }

    @GetMapping("/download/excel/employee/{employeeId}")
    public void downloadEmployeeExcel(
            @PathVariable String employeeId,
            @RequestParam int month,
            @RequestParam int year,
            HttpServletResponse response) throws Exception {
        attendanceService.downloadEmployeeExcel(employeeId, month, year, response);
    }

    // ── Private helper ─────────────────────────────────────────────────────────
    //
    // Reads the real client IP, respecting reverse-proxy X-Forwarded-For header.
    // Used only for the network-check response body (informational).
    // The service always re-reads the IP from HttpServletRequest independently.

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}