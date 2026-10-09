package com.tadobasolutions.service.Impl;

import com.tadobasolutions.entity.Employee;
import com.tadobasolutions.repository.AttendanceRepository;
import com.tadobasolutions.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * Sends a "Please mark your attendance!" push notification to every employee
 * (and sub-admin) at 09:55 AM IST every weekday (Mon–Sat).
 *
 * Also sends a 5:00 PM "Don't forget — attendance still not marked" follow-up
 * only to employees who haven't marked yet.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceScheduler {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final EmployeeRepository     employeeRepository;
    private final AttendanceRepository   attendanceRepository;
    private final FcmNotificationService fcmService;

    /**
     * 10:00 AM IST, Mon–Sat  →  remind ALL employees to mark attendance.
     */
    @Scheduled(cron = "0 00 10 * * MON-SAT", zone = "Asia/Kolkata")
    public void morningAttendanceReminder() {
        List<Employee> employees = employeeRepository.findAll();
        for (Employee emp : employees) {
            if (emp.getFcmTokens() == null || emp.getFcmTokens().isEmpty()) continue;

            fcmService.sendToTokens(
                    emp.getFcmTokens(),
                    "📋 Mark Your Attendance",
                    "Good morning, " + emp.getName().split(" ")[0] +
                            "! Please mark your attendance for today.",
                    Map.of("type", "ATTENDANCE_REMINDER", "screen", "attendance")
            );
            log.info("[AttendanceScheduler] Morning reminder → {}", emp.getId());
        }
    }

    /**
     * 5:55 PM IST, Mon–Sat  →  send follow-up only to employees who haven't
     * marked attendance yet today.
     */
    /**
     * 5:55 PM IST, Mon–Sat  →  remind employees who checked in but haven't
     * checked out yet to mark their checkout before leaving.
     */
    @Scheduled(cron = "0 55 17 * * MON-SAT", zone = "Asia/Kolkata")
    public void checkoutReminder() {
        LocalDate today = LocalDate.now(IST);
        List<Employee> employees = employeeRepository.findAll();

        for (Employee emp : employees) {
            if (emp.getFcmTokens() == null || emp.getFcmTokens().isEmpty()) continue;

            // Only remind if checked IN but NOT yet checked out
            attendanceRepository.findByEmployeeIdAndDate(emp.getId(), today)
                    .ifPresent(attendance -> {
                        if (!attendance.isCheckedOut()) {
                            fcmService.sendToTokens(
                                    emp.getFcmTokens(),
                                    "🕔 Don't Forget to Check Out!",
                                    "Hi " + emp.getName().split(" ")[0] +
                                            ", please mark your check-out before leaving. Have a great evening!",
                                    Map.of("type", "CHECKOUT_REMINDER", "screen", "attendance")
                            );
                            log.info("[AttendanceScheduler] Checkout reminder → {}", emp.getId());
                        }
                    });
        }
    }
}
