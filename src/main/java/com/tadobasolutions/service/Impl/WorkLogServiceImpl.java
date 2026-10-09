package com.tadobasolutions.service.Impl;

import com.tadobasolutions.dto.request.WorkLogRequestDTO;
import com.tadobasolutions.dto.response.WorkLogResponseDTO;
import com.tadobasolutions.entity.DailyWorkLog;
import com.tadobasolutions.entity.Employee;
import com.tadobasolutions.entity.enums.TaskStatus;
import com.tadobasolutions.exception.BadRequestException;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.repository.DailyWorkLogRepository;
import com.tadobasolutions.repository.EmployeeRepository;
import com.tadobasolutions.repository.TaskRepository;
import com.tadobasolutions.service.WorkLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkLogServiceImpl implements WorkLogService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final DailyWorkLogRepository workLogRepository;
    private final EmployeeRepository     employeeRepository;
    private final TaskRepository         taskRepository;

    // ── Submit EOD log ────────────────────────────────────────────────────────

    @Override
    public WorkLogResponseDTO submitWorkLog(WorkLogRequestDTO dto) {
        LocalDate today = LocalDate.now(IST);

        // Prevent double submission for same day
        workLogRepository.findByEmployeeIdAndDate(dto.getEmployeeId(), today)
                .ifPresent(existing -> {
                    throw new BadRequestException(
                            "Work log already submitted for today. Use update if you want to change it.");
                });

        Employee emp = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + dto.getEmployeeId()));

        LocalDateTime now = LocalDateTime.now(IST);

        // Auto-count how many tasks this employee COMPLETED today
        int tasksDoneToday = countTasksCompletedToday(dto.getEmployeeId(), today);

        // Late submission = submitted after 8 PM IST
        boolean isLate = now.getHour() >= 20;

        DailyWorkLog workLog = new DailyWorkLog();

        workLog.setEmployeeId(dto.getEmployeeId());
        workLog.setEmployeeName(emp.getName());
        workLog.setDepartmentId(emp.getDepartmentId());
        workLog.setDepartmentName(emp.getDepartmentName());
        workLog.setDate(today);
        workLog.setSubmittedAt(now);
        workLog.setTasksCompleted(dto.getTasksCompleted());
        workLog.setWorkSummary(dto.getWorkSummary());
        workLog.setChallenges(dto.getChallenges());
        workLog.setTomorrowPlan(dto.getTomorrowPlan());
        workLog.setTasksDoneCount(tasksDoneToday);
        workLog.setLateSubmission(isLate);

        DailyWorkLog saved = workLogRepository.save(workLog);

        log.info("EOD log submitted -> employee={} date={} tasksDone={} late={}",
                emp.getId(), today, tasksDoneToday, isLate);

        return toDTO(saved);
    }

    // ── Employee views their own log for a date ───────────────────────────────

    @Override
    public WorkLogResponseDTO getMyLogForDate(String employeeId, LocalDate date) {
        DailyWorkLog log = workLogRepository.findByEmployeeIdAndDate(employeeId, date)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No work log found for date: " + date));
        return toDTO(log);
    }

    // ── Employee views their full history ─────────────────────────────────────

    @Override
    public List<WorkLogResponseDTO> getMyLogs(String employeeId) {
        return workLogRepository.findByEmployeeIdOrderByDateDesc(employeeId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── Admin: EOD inbox for a given date ─────────────────────────────────────

    @Override
    public List<WorkLogResponseDTO> getLogsForDate(LocalDate date) {
        return workLogRepository.findByDateOrderByEmployeeNameAsc(date)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── Admin: one employee's logs in a range ────────────────────────────────

    @Override
    public List<WorkLogResponseDTO> getEmployeeLogs(String employeeId,
                                                    LocalDate from, LocalDate to) {
        return workLogRepository
                .findByEmployeeIdAndDateBetweenOrderByDateDesc(employeeId, from, to)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── Admin: full team logs in a range ─────────────────────────────────────

    @Override
    public List<WorkLogResponseDTO> getTeamLogs(LocalDate from, LocalDate to) {
        return workLogRepository
                .findByDateBetweenOrderByDateDescEmployeeNameAsc(from, to)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── Admin: add a review note on a log ────────────────────────────────────

    @Override
    public WorkLogResponseDTO addAdminNote(String logId, String note) {
        DailyWorkLog log = workLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Work log not found: " + logId));
        log.setAdminNote(note);
        return toDTO(workLogRepository.save(log));
    }

    // ── Used by idle-detection scheduler ─────────────────────────────────────

    @Override
    public List<String> getSubmittedEmployeeIdsForDate(LocalDate date) {
        return workLogRepository.findEmployeeIdsWhoSubmittedOn(date)
                .stream()
                .map(DailyWorkLog::getEmployeeId)
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Count Task documents for this employee that were completed today.
     * Uses submissionDate (set when employee calls submitTask) falling within today.
     */
    private int countTasksCompletedToday(String employeeId, LocalDate today) {
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay   = today.atTime(23, 59, 59);

        return (int) taskRepository
                .findByEmployeeIdAndStatusIn(employeeId,
                        List.of(TaskStatus.COMPLETED, TaskStatus.LATE))
                .stream()
                .filter(t -> t.getSubmissionDate() != null
                        && !t.getSubmissionDate().isBefore(startOfDay)
                        && !t.getSubmissionDate().isAfter(endOfDay))
                .count();
    }

    private WorkLogResponseDTO toDTO(DailyWorkLog log) {
        return WorkLogResponseDTO.builder()
                .id(log.getId())
                .employeeId(log.getEmployeeId())
                .employeeName(log.getEmployeeName())
                .departmentName(log.getDepartmentName())
                .date(log.getDate())
                .submittedAt(log.getSubmittedAt())
                .tasksCompleted(log.getTasksCompleted())
                .workSummary(log.getWorkSummary())
                .challenges(log.getChallenges())
                .tomorrowPlan(log.getTomorrowPlan())
                .tasksDoneCount(log.getTasksDoneCount())
                .lateSubmission(log.isLateSubmission())
                .adminNote(log.getAdminNote())
                .build();
    }
}