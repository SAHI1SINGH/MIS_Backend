package com.tadobasolutions.service;

import com.tadobasolutions.dto.request.AttendanceCheckOutRequestDTO;
import com.tadobasolutions.dto.request.AttendanceRequestDTO;
import com.tadobasolutions.dto.response.AttendanceResponseDTO;
import com.tadobasolutions.dto.response.AttendanceSummaryDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface AttendanceService {

    AttendanceResponseDTO checkOutAttendance(AttendanceCheckOutRequestDTO dto, HttpServletRequest request);

    List<AttendanceResponseDTO> getMyAttendance(String employeeId, int month, int year);

    AttendanceSummaryDTO getMySummary(String employeeId, int month, int year);

    List<AttendanceResponseDTO> getTodayAttendance();

    List<AttendanceResponseDTO> getMonthlyAttendance(int month, int year);

    List<AttendanceResponseDTO> getEmployeeAttendance(String employeeId, int month, int year);

    AttendanceResponseDTO addAdminNote(String attendanceId, String adminNote);

    AttendanceResponseDTO adminMarkAttendance(AttendanceRequestDTO dto);

    void downloadMonthlyExcel(int month, int year, HttpServletResponse response) throws Exception;

    boolean hasMarkedToday(String employeeId);

    boolean hasCheckedOutToday(String employeeId);

    void downloadEmployeeExcel(String employeeId, int month, int year,
                               HttpServletResponse response) throws Exception;

    AttendanceResponseDTO markAttendance(AttendanceRequestDTO dto, HttpServletRequest request);

    boolean isOfficeNetwork(HttpServletRequest request);
}