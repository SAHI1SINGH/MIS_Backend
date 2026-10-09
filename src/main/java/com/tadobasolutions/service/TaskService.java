package com.tadobasolutions.service;

import com.tadobasolutions.dto.TaskStatusCountDTO;
import com.tadobasolutions.dto.request.TaskRequestDTO;
import com.tadobasolutions.dto.response.ReportSummaryDTO;
import com.tadobasolutions.dto.response.TaskResponseDTO;
import com.tadobasolutions.dto.response.TeamReportDTO;
import com.tadobasolutions.entity.enums.TaskStatus;

import java.time.LocalDate;
import java.util.List;

public interface TaskService {
    TaskResponseDTO createTask(TaskRequestDTO dto);
    TaskResponseDTO updateTask(String id, TaskRequestDTO dto);
    void deleteTask(String id);
    TaskResponseDTO getTaskById(String id);
    List<TaskResponseDTO> getAllTasks(TaskStatus status, String employeeId, LocalDate targetDate);
    TaskResponseDTO submitTask(String id, Integer empRating);
    TaskStatusCountDTO getTaskStatusCounts(String employeeId);
    ReportSummaryDTO getMonthlyReportSummary(String employeeId, int month, int year);
    byte[] exportMonthlyReportExcel(String employeeId, int month, int year);
    String getEmployeeNameById(String id);
    TaskResponseDTO reviewTask(String id, String role, Integer rating, String remarks);

    // ── NEW ──────────────────────────────────────────────────────────────────
    /** Admin: full team performance report for a given month/year */
    TeamReportDTO getTeamReport(int month, int year);

    /** Admin: export team report as Excel */
    byte[] exportTeamReportExcel(int month, int year);
}