package com.tadobasolutions.controllers;

import com.tadobasolutions.dto.request.LeaveRequestDTO;
import com.tadobasolutions.dto.request.LeaveReviewRequestDTO;
import com.tadobasolutions.dto.response.CurrentlyOnLeaveDTO;
import com.tadobasolutions.dto.response.LeaveResponseDTO;
import com.tadobasolutions.entity.enums.LeaveStatus;
import com.tadobasolutions.service.LeaveService;
import com.tadobasolutions.utilities.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://tadobasolutions.com",
        "https://tadobasolutions.com",
        "http://mis.tadobasolutions.com",
        "https://mis.tadobasolutions.com"
})
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    // ── Employee applies for leave ──────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUB_ADMIN','EMPLOYEE','INTERN')")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> apply(
            @RequestBody LeaveRequestDTO dto,
            Authentication authentication) {
        String email = authentication.getName();
        LeaveResponseDTO created = leaveService.applyLeave(email, dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<LeaveResponseDTO>builder()
                        .data(created)
                        .message("Leave request submitted successfully")
                        .statusCode(HttpStatus.CREATED.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    // ── Admin approves/rejects directly ──────────────────────────────────────
    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> review(
            @PathVariable String id,
            @RequestBody LeaveReviewRequestDTO dto
    ) {
        LeaveResponseDTO updated = leaveService.review(id, dto);
        return ResponseEntity.ok(
                ApiResponse.<LeaveResponseDTO>builder()
                        .data(updated)
                        .message("Leave reviewed successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> getById(@PathVariable String id) {
        LeaveResponseDTO leave = leaveService.getLeaveById(id);
        return ResponseEntity.ok(
                ApiResponse.<LeaveResponseDTO>builder()
                        .data(leave)
                        .message("Leave fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LeaveResponseDTO>>> getAll(
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) String employeeId
    ) {
        List<LeaveResponseDTO> leaves = leaveService.getAllLeaves(status, employeeId);
        return ResponseEntity.ok(
                ApiResponse.<List<LeaveResponseDTO>>builder()
                        .data(leaves)
                        .message("Leaves fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    // ── Dashboard: employees currently on leave — visible to everyone ───────
    @GetMapping("/currently-on-leave")
    @PreAuthorize("hasAnyRole('ADMIN','SUB_ADMIN','EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<CurrentlyOnLeaveDTO>>> getCurrentlyOnLeave() {
        List<CurrentlyOnLeaveDTO> onLeave = leaveService.getCurrentlyOnLeave();
        return ResponseEntity.ok(
                ApiResponse.<List<CurrentlyOnLeaveDTO>>builder()
                        .data(onLeave)
                        .message("Currently on-leave employees fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    // ── Admin: history of leaves already reviewed ────────────────────────────
    @GetMapping("/review-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveResponseDTO>>> getReviewHistory(
            @RequestParam String approverId
    ) {
        List<LeaveResponseDTO> history = leaveService.getReviewHistory(approverId);
        return ResponseEntity.ok(
                ApiResponse.<List<LeaveResponseDTO>>builder()
                        .data(history)
                        .message("Review history fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    // ── Employee cancels their own leave (only while PENDING) ────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUB_ADMIN','EMPLOYEE','INTERN')")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable String id,
            @RequestParam String employeeId
    ) {
        leaveService.cancelLeave(id, employeeId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .data(null)
                        .message("Leave request cancelled successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }
}