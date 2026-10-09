package com.tadobasolutions.service;

import com.tadobasolutions.entity.StandupLog;
import com.tadobasolutions.repository.StandupLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StandupLogService {

    private final StandupLogRepository standupRepo;

    // ── Submit a new work entry (no daily limit) ──────────────────────────────
    public StandupLog submitStandup(StandupLog log) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // count existing entries today to set entryNumber
        long existingCount = standupRepo
                .countByEmployeeIdAndStandupDate(log.getEmployeeId(), today);

        log.setStandupDate(today);
        log.setSubmittedAt(now);
        log.setEntryNumber((int) existingCount + 1);

        // mark LATE if submitted after 10 AM
        log.setStatus(now.getHour() >= 10 ? "LATE" : "SUBMITTED");

        // default workType if not provided
        if (log.getWorkType() == null || log.getWorkType().isBlank()) {
            log.setWorkType("General");
        }

        return standupRepo.save(log);
    }

    // ── Today's entries for one employee ──────────────────────────────────────
    public List<StandupLog> getTodayLogs(String employeeId) {
        return standupRepo
                .findByEmployeeIdAndStandupDateOrderBySubmittedAtAsc(
                        employeeId, LocalDate.now()
                );
    }

    // ── Today's entry count + submitted flag ──────────────────────────────────
    public Map<String, Object> getTodayStatus(String employeeId) {
        long count = standupRepo
                .countByEmployeeIdAndStandupDate(employeeId, LocalDate.now());
        return Map.of(
                "submitted", count > 0,
                "count",     count
        );
    }

    // ── Employee: full history (all dates, all entries) ───────────────────────
    public List<StandupLog> getByEmployee(String employeeId) {
        return standupRepo
                .findByEmployeeIdOrderByStandupDateDescSubmittedAtDesc(employeeId);
    }

    // ── Admin: all employees' entries for a specific date ─────────────────────
    public List<StandupLog> getByDate(LocalDate date) {
        return standupRepo
                .findByStandupDateOrderByEmployeeIdAscSubmittedAtAsc(date);
    }
}