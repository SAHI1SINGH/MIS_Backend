package com.tadobasolutions.service.Impl;

import com.tadobasolutions.dto.request.LeaveRequestDTO;
import com.tadobasolutions.dto.request.LeaveReviewRequestDTO;
import com.tadobasolutions.dto.response.CurrentlyOnLeaveDTO;
import com.tadobasolutions.dto.response.LeaveResponseDTO;
import com.tadobasolutions.entity.Admin;
import com.tadobasolutions.entity.Employee;
import com.tadobasolutions.entity.Intern;
import com.tadobasolutions.entity.Leave;
import com.tadobasolutions.entity.enums.LeaveStatus;
import com.tadobasolutions.exception.BadRequestException;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.repository.AdminRepository;
import com.tadobasolutions.repository.EmployeeRepository;
import com.tadobasolutions.repository.InternRepository;
import com.tadobasolutions.repository.LeaveRepository;
import com.tadobasolutions.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRepository            leaveRepository;
    private final EmployeeRepository         employeeRepository;
    private final InternRepository           internRepository;   // NEW
    private final AdminRepository            adminRepository;
    private final FcmNotificationService     fcmService;
    private final LeaveReminderSchedulerImpl leaveReminderScheduler;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    // Applicant can be an Employee/Sub-Admin or an Intern
    private record Applicant(String id, String name) {}

    private Applicant resolveApplicant(String email) {
        Optional<Employee> emp = employeeRepository.findByEmail(email);
        if (emp.isPresent()) {
            return new Applicant(emp.get().getId(), emp.get().getName());
        }
        Intern intern = internRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return new Applicant(intern.getId(), intern.getName());
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private LeaveResponseDTO mapToDTO(Leave leave) {
        return new LeaveResponseDTO(
                leave.getId(),
                leave.getEmployeeId(),
                leave.getEmployeeName(),
                leave.getLeaveType(),
                leave.getFromDate(),
                leave.getToDate(),
                leave.getTotalDays(),
                leave.getReason(),
                leave.getStatus(),
                leave.getAppliedAt(),
                leave.getApproverId(),
                leave.getApproverName(),
                leave.getActionAt(),
                leave.getRemarks(),
                leave.getCreatedAt(),
                leave.getUpdatedAt()
        );
    }

    // ─── Apply ────────────────────────────────────────────────────────────────

    @Override
    public LeaveResponseDTO applyLeave(String email, LeaveRequestDTO dto) {

        Applicant applicant = resolveApplicant(email);   // CHANGED

        if (dto.getFromDate() == null || dto.getToDate() == null) {
            throw new BadRequestException("fromDate and toDate are required");
        }
        if (dto.getToDate().isBefore(dto.getFromDate())) {
            throw new BadRequestException("toDate cannot be before fromDate");
        }

        int totalDays = (int) ChronoUnit.DAYS.between(dto.getFromDate(), dto.getToDate()) + 1;

        Leave leave = new Leave();
        leave.setEmployeeId(applicant.id());
        leave.setEmployeeName(applicant.name());
        leave.setLeaveType(dto.getLeaveType());
        leave.setFromDate(dto.getFromDate());
        leave.setToDate(dto.getToDate());
        leave.setTotalDays(totalDays);
        leave.setReason(dto.getReason());
        leave.setStatus(LeaveStatus.PENDING);
        leave.setAppliedAt(LocalDateTime.now());

        Leave saved = leaveRepository.save(leave);

        // ── Notify all Admins directly (single-level approval) ─────────────────
        List<String> adminTokens = adminRepository.findAll().stream()
                .map(Admin::getFcmTokens)
                .filter(tokens -> tokens != null)
                .flatMap(List::stream)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (!adminTokens.isEmpty()) {
            fcmService.sendToTokens(
                    adminTokens,
                    "🗓️ New Leave Request",
                    applicant.name() + " applied for " + totalDays + " day(s) leave (" +
                            dto.getFromDate().format(DATE_FMT) + " to " + dto.getToDate().format(DATE_FMT) + ")",
                    Map.of(
                            "leaveId",      saved.getId(),
                            "type",         "LEAVE_APPLIED",
                            "employeeName", applicant.name(),
                            "totalDays",    String.valueOf(totalDays)
                    )
            );
        }

        return mapToDTO(saved);
    }

    // ─── Review (Admin) ─────────────────────────────────────────────────────────

    @Override
    public LeaveResponseDTO review(String leaveId, LeaveReviewRequestDTO dto) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("This leave is not pending approval");
        }

        Admin admin = adminRepository.findById(dto.getApproverId())
                .orElseThrow(() -> new BadRequestException("Approver is not a valid Admin"));

        leave.setApproverId(admin.getId());
        leave.setApproverName(admin.getName());
        leave.setActionAt(LocalDateTime.now());
        leave.setRemarks(dto.getRemarks());
        leave.setStatus(dto.isApproved() ? LeaveStatus.APPROVED : LeaveStatus.REJECTED);

        Leave saved = leaveRepository.save(leave);

        notifyApplicantOfDecision(saved, dto.isApproved(), admin.getName());

        // ── If approved and already active today, broadcast to the org now ─────
        // (the daily scheduled job picks up every subsequent day automatically)
        if (dto.isApproved()) {
            LocalDate today = LocalDate.now(IST);
            if (!today.isBefore(saved.getFromDate()) && !today.isAfter(saved.getToDate())) {
                leaveReminderScheduler.notifyOrgOfActiveLeave(saved, today);
            }
        }

        log.info("Leave {} {} by {}", leaveId,
                dto.isApproved() ? "approved" : "rejected", admin.getName());

        return mapToDTO(saved);
    }

    private void notifyApplicantOfDecision(Leave leave, boolean approved, String decidedByName) {
        // Applicant may be an employee or an intern
        List<String> tokens = employeeRepository.findById(leave.getEmployeeId())
                .map(Employee::getFcmTokens)
                .orElseGet(() -> internRepository.findById(leave.getEmployeeId())
                        .map(Intern::getFcmTokens)   // remove if Intern has no fcmTokens field
                        .orElse(null));

        if (tokens == null || tokens.isEmpty()) return;

        String title = approved ? "✅ Leave Approved" : "❌ Leave Rejected";
        String body  = approved
                ? "Your leave request for " + leave.getTotalDays() + " day(s) (" +
                leave.getFromDate().format(DATE_FMT) + " to " + leave.getToDate().format(DATE_FMT) + ") has been approved"
                : "Your leave request for " + leave.getTotalDays() + " day(s) was rejected by " + decidedByName;

        fcmService.sendToTokens(
                tokens,
                title,
                body,
                Map.of(
                        "leaveId", leave.getId(),
                        "type",    approved ? "LEAVE_APPROVED" : "LEAVE_REJECTED"
                )
        );
    }

    // ─── Currently on leave (for dashboard) ────────────────────────────────────

    @Override
    public List<CurrentlyOnLeaveDTO> getCurrentlyOnLeave() {
        LocalDate today = LocalDate.now();

        List<Leave> onLeave = leaveRepository
                .findByStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        LeaveStatus.APPROVED, today, today);

        return onLeave.stream().map(leave -> {
            int daysElapsed = (int) ChronoUnit.DAYS.between(leave.getFromDate(), today) + 1;
            int daysRemaining = leave.getTotalDays() - daysElapsed + 1;

            return new CurrentlyOnLeaveDTO(
                    leave.getEmployeeId(),
                    leave.getEmployeeName(),
                    leave.getLeaveType(),
                    leave.getFromDate(),
                    leave.getToDate(),
                    leave.getTotalDays(),
                    daysElapsed,
                    daysRemaining
            );
        }).collect(Collectors.toList());
    }

    // ─── Reads ────────────────────────────────────────────────────────────────

    @Override
    public LeaveResponseDTO getLeaveById(String id) {
        return mapToDTO(leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found")));
    }

    @Override
    public List<LeaveResponseDTO> getAllLeaves(LeaveStatus status, String employeeId) {
        return leaveRepository.findLeavesByFilters(status, employeeId)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<LeaveResponseDTO> getReviewHistory(String approverId) {
        return leaveRepository.findByApproverIdAndActionAtIsNotNullOrderByActionAtDesc(approverId)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ─── Cancel ───────────────────────────────────────────────────────────────

    @Override
    public void cancelLeave(String id, String employeeId) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        if (!leave.getEmployeeId().equals(employeeId)) {
            throw new BadRequestException("You can only cancel your own leave request");
        }
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Leave can only be cancelled while pending approval");
        }
        leaveRepository.deleteById(id);
    }
}