package com.tadobasolutions.service.Impl;

import com.tadobasolutions.entity.Admin;
import com.tadobasolutions.entity.Employee;
import com.tadobasolutions.entity.Leave;
import com.tadobasolutions.entity.enums.LeaveStatus;
import com.tadobasolutions.repository.AdminRepository;
import com.tadobasolutions.repository.EmployeeRepository;
import com.tadobasolutions.repository.LeaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveReminderSchedulerImpl {

    private final LeaveRepository        leaveRepository;
    private final EmployeeRepository     employeeRepository;
    private final AdminRepository        adminRepository;
    private final FcmNotificationService fcmService;


    @Scheduled(cron = "0 5 9 * * *", zone = "Asia/Kolkata")
    public void sendDailyOnLeaveReminders() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        List<Leave> activeLeaves = leaveRepository
                .findByStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        LeaveStatus.APPROVED, today, today);

        if (activeLeaves.isEmpty()) return;

        for (Leave leave : activeLeaves) {
            notifyOrgOfActiveLeave(leave, today);
        }
    }
    public void notifyOrgOfActiveLeave(Leave leave, LocalDate today) {
        int daysElapsed   = (int) ChronoUnit.DAYS.between(leave.getFromDate(), today) + 1;
        int daysRemaining = leave.getTotalDays() - daysElapsed + 1;

        String title = "🌴 " + leave.getEmployeeName() + " is on leave";
        String body  = daysRemaining > 1
                ? leave.getEmployeeName() + " is on leave today. " + daysRemaining + " day(s) remaining."
                : leave.getEmployeeName() + " is on leave today — last day of their leave.";

        List<String> tokens = collectOrgTokensExcept(leave.getEmployeeId());
        if (tokens.isEmpty()) return;

        fcmService.sendToTokens(
                tokens,
                title,
                body,
                Map.of(
                        "leaveId",       leave.getId(),
                        "type",          "LEAVE_ACTIVE_REMINDER",
                        "employeeName",  leave.getEmployeeName(),
                        "daysRemaining", String.valueOf(daysRemaining),
                        "totalDays",     String.valueOf(leave.getTotalDays())
                )
        );

        log.info("On-leave reminder sent for {} — {} recipient(s), {} day(s) remaining",
                leave.getEmployeeName(), tokens.size(), daysRemaining);
    }
    private List<String> collectOrgTokensExcept(String excludeId) {
        List<String> tokens = new ArrayList<>();

        tokens.addAll(employeeRepository.findAll().stream()
                .filter(e -> !e.getId().equals(excludeId))
                .map(Employee::getFcmTokens)
                .filter(t -> t != null)
                .flatMap(List::stream)
                .collect(Collectors.toList()));

        tokens.addAll(adminRepository.findAll().stream()
                .map(Admin::getFcmTokens)
                .filter(t -> t != null)
                .flatMap(List::stream)
                .collect(Collectors.toList()));

        return tokens.stream()
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}