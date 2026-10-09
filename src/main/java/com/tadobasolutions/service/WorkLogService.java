package com.tadobasolutions.service;

import com.tadobasolutions.dto.request.WorkLogRequestDTO;
import com.tadobasolutions.dto.response.WorkLogResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface WorkLogService {

    // Employee submits their EOD log for today
    WorkLogResponseDTO submitWorkLog(WorkLogRequestDTO dto);

    // Employee views their own log for a specific date
    WorkLogResponseDTO getMyLogForDate(String employeeId, LocalDate date);

    // Employee views their own history
    List<WorkLogResponseDTO> getMyLogs(String employeeId);

    // Admin: EOD inbox — all logs submitted for a given date
    List<WorkLogResponseDTO> getLogsForDate(LocalDate date);

    // Admin: all logs for one employee in a date range
    List<WorkLogResponseDTO> getEmployeeLogs(String employeeId, LocalDate from, LocalDate to);

    // Admin: team logs in a date range
    List<WorkLogResponseDTO> getTeamLogs(LocalDate from, LocalDate to);

    // Admin: add a note on an employee's work log
    WorkLogResponseDTO addAdminNote(String logId, String note);

    // Used by the idle-detection scheduler — returns IDs of employees who submitted today
    List<String> getSubmittedEmployeeIdsForDate(LocalDate date);
}
