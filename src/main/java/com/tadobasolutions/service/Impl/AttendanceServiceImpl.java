package com.tadobasolutions.service.Impl;

import com.tadobasolutions.dto.request.AttendanceCheckOutRequestDTO;
import com.tadobasolutions.dto.request.AttendanceRequestDTO;
import com.tadobasolutions.dto.response.AttendanceResponseDTO;
import com.tadobasolutions.dto.response.AttendanceSummaryDTO;
import com.tadobasolutions.entity.Attendance;
import com.tadobasolutions.entity.Employee;
import com.tadobasolutions.entity.Intern;
import com.tadobasolutions.entity.enums.AttendanceType;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.repository.AttendanceRepository;
import com.tadobasolutions.repository.EmployeeRepository;
import com.tadobasolutions.repository.InternRepository;
import com.tadobasolutions.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.util.Comparator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private static final ZoneId      IST         = ZoneId.of("Asia/Kolkata");

    // ── 10:05 AM grace period — marking at or before 10:05 is PRESENT ────────
    private static final LocalTime   LATE_CUTOFF = LocalTime.of(10, 5);

    private static final List<String> OFFICE_LAN_PREFIXES = List.of(
            "192.168.1.",
            "10.0.0."
    );

    private static final List<String> OFFICE_CIDR_RANGES = List.of(
            "152.56.160.0/20"
    );

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository   employeeRepository;
    private final InternRepository     internRepository;

    // ── Assignee resolution ────────────────────────────────────────────────
    // Checks employees collection first, then interns collection — same
    // pattern used in TaskServiceImpl so both Employee and Intern attendance
    // work off this one entity/repository pair.

    private record PersonInfo(String id, String name, String departmentId, String departmentName) {}

    private PersonInfo resolvePerson(String employeeId) {
        var empOpt = employeeRepository.findById(employeeId);
        if (empOpt.isPresent()) {
            Employee e = empOpt.get();
            return new PersonInfo(e.getId(), e.getName(), e.getDepartmentId(), e.getDepartmentName());
        }
        var internOpt = internRepository.findById(employeeId);
        if (internOpt.isPresent()) {
            Intern i = internOpt.get();
            return new PersonInfo(i.getId(), i.getName(), i.getDepartmentId(), i.getDepartmentName());
        }
        throw new ResourceNotFoundException("Employee/Intern not found: " + employeeId);
    }

    // ── Check-In (Employee/Intern self-service) ─────────────────────────────

    @Override
    public AttendanceResponseDTO markAttendance(AttendanceRequestDTO dto, HttpServletRequest request) {
        PersonInfo person = resolvePerson(dto.getEmployeeId());

        LocalDate today    = LocalDate.now(IST);
        LocalDate markDate = dto.getDate() != null ? dto.getDate() : today;

        Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndDate(dto.getEmployeeId(), markDate);
        if (existing.isPresent()) {
            throw new IllegalStateException("Attendance already marked for " + markDate);
        }

        // ── Biometric validation ──────────────────────────────────────────────
        String  clientIp     = getClientIp(request);
        boolean isOfficeNet  = isOfficeIp(clientIp);
        boolean isBiometric  = dto.isBiometricVerified();

        AttendanceType requestedType = dto.getType() != null ? dto.getType() : AttendanceType.PRESENT;

        if (requestedType == AttendanceType.BIOMETRIC || requestedType == AttendanceType.BIOMETRIC_REMOTE) {
            if (!isBiometric) {
                throw new IllegalStateException("Biometric verification is required to mark biometric attendance.");
            }
            if (requestedType == AttendanceType.BIOMETRIC_REMOTE
                    && (dto.getRemoteReason() == null || dto.getRemoteReason().isBlank())) {
                throw new IllegalStateException("A reason is required when marking attendance outside office network.");
            }
        }
        // ─────────────────────────────────────────────────────────────────────

        LocalDateTime now    = LocalDateTime.now(IST);
        boolean isLate       = markDate.equals(today) && now.toLocalTime().isAfter(LATE_CUTOFF);

        AttendanceType type  = requestedType;
        if (isLate && type == AttendanceType.PRESENT) {
            type = AttendanceType.LATE;
        }

        Attendance record = new Attendance();
        record.setEmployeeId(person.id());
        record.setEmployeeName(person.name());
        record.setDepartmentId(person.departmentId());
        record.setDepartmentName(person.departmentName());
        record.setDate(markDate);
        record.setMarkedAt(now);
        record.setType(type);
        record.setReason(dto.getRemoteReason() != null && !dto.getRemoteReason().isBlank()
                ? dto.getRemoteReason()
                : dto.getReason());
        record.setRemarks(dto.getRemarks());
        record.setLateEntry(isLate || markDate.isBefore(today));
        record.setCheckedOut(false);
        record.setBiometricVerified(isBiometric);
        record.setClientIp(clientIp);

        attendanceRepository.save(record);
        log.info("[Attendance] Check-in: person={} date={} type={} late={} biometric={} officeNet={}",
                person.id(), markDate, type, record.isLateEntry(), isBiometric, isOfficeNet);
        return toDTO(record, isOfficeNet);
    }

    // ── Check-Out (Employee/Intern self-service) — independent of check-in ──

    @Override
    public AttendanceResponseDTO checkOutAttendance(AttendanceCheckOutRequestDTO dto, HttpServletRequest request) {
        PersonInfo person = resolvePerson(dto.getEmployeeId());

        LocalDate today        = LocalDate.now(IST);
        LocalDate checkoutDate = dto.getDate() != null ? dto.getDate() : today;
        LocalDateTime now      = LocalDateTime.now(IST);

        String  clientIp    = getClientIp(request);
        boolean isOfficeNet = isOfficeIp(clientIp);
        boolean isBiometric = dto.isBiometricVerified();

        Optional<Attendance> existingOpt =
                attendanceRepository.findByEmployeeIdAndDate(dto.getEmployeeId(), checkoutDate);

        if (existingOpt.isPresent()) {
            // ── Case A: app check-in exists, close it out ───────────────────
            Attendance record = existingOpt.get();

            if (record.isCheckedOut()) {
                throw new IllegalStateException("You have already checked out for today.");
            }
            if (record.getMarkedAt() != null && now.isBefore(record.getMarkedAt())) {
                throw new IllegalStateException("Check-out time cannot be before check-in time.");
            }

            record.setCheckOutTime(now);
            record.setCheckOutRemarks(dto.getRemarks());
            record.setCheckedOut(true);

            if (record.getMarkedAt() != null) {
                double hours = Duration.between(record.getMarkedAt(), now).toMinutes() / 60.0;
                record.setWorkingHours(Math.round(hours * 100.0) / 100.0);
            }

            attendanceRepository.save(record);
            log.info("[Attendance] Check-out (app check-in existed): person={} date={} hours={}",
                    dto.getEmployeeId(), checkoutDate, record.getWorkingHours());
            return toDTO(record);
        }

        // ── Case B: standalone checkout, no app check-in for this date ──────
        if (isBiometric && !isOfficeNet
                && (dto.getRemoteReason() == null || dto.getRemoteReason().isBlank())) {
            throw new IllegalStateException(
                    "A reason is required when checking out outside the office network.");
        }

        Attendance record = new Attendance();
        record.setEmployeeId(person.id());
        record.setEmployeeName(person.name());
        record.setDepartmentId(person.departmentId());
        record.setDepartmentName(person.departmentName());
        record.setDate(checkoutDate);
        record.setMarkedAt(null);
        record.setType(isBiometric
                ? (isOfficeNet ? AttendanceType.BIOMETRIC : AttendanceType.BIOMETRIC_REMOTE)
                : AttendanceType.MANUAL);
        record.setReason(dto.getRemoteReason());
        record.setLateEntry(false);
        record.setBiometricVerified(isBiometric);
        record.setClientIp(clientIp);

        record.setCheckOutTime(now);
        record.setCheckOutRemarks(dto.getRemarks());
        record.setCheckedOut(true);
        record.setWorkingHours(null);

        attendanceRepository.save(record);
        log.info("[Attendance] Standalone check-out (no app check-in): person={} date={} biometric={} officeNet={}",
                person.id(), checkoutDate, isBiometric, isOfficeNet);
        return toDTO(record, isOfficeNet);
    }

    // ── Admin mark / correction ───────────────────────────────────────────────

    @Override
    public AttendanceResponseDTO adminMarkAttendance(AttendanceRequestDTO dto) {
        PersonInfo person = resolvePerson(dto.getEmployeeId());

        LocalDate markDate = dto.getDate() != null ? dto.getDate() : LocalDate.now(IST);

        attendanceRepository.findByEmployeeIdAndDate(dto.getEmployeeId(), markDate)
                .ifPresent(attendanceRepository::delete);

        Attendance record = new Attendance();
        record.setEmployeeId(person.id());
        record.setEmployeeName(person.name());
        record.setDepartmentId(person.departmentId());
        record.setDepartmentName(person.departmentName());
        record.setDate(markDate);
        record.setMarkedAt(LocalDateTime.now(IST));
        record.setType(dto.getType() != null ? dto.getType() : AttendanceType.MANUAL);
        record.setReason(dto.getReason());
        record.setRemarks(dto.getRemarks());
        record.setLateEntry(false);
        record.setCheckedOut(false);
        record.setBiometricVerified(false);
        record.setClientIp(null);

        if (dto.getCheckOutTime() != null) {
            record.setCheckOutTime(dto.getCheckOutTime());
            record.setCheckOutRemarks(dto.getCheckOutRemarks());
            record.setCheckedOut(true);
            double hours = Duration.between(record.getMarkedAt(), dto.getCheckOutTime()).toMinutes() / 60.0;
            record.setWorkingHours(Math.round(hours * 100.0) / 100.0);
        }

        attendanceRepository.save(record);
        return toDTO(record);
    }

    // ── Network check (Flutter pre-flight) ───────────────────────────────────

    @Override
    public boolean isOfficeNetwork(HttpServletRequest request) {
        return isOfficeIp(getClientIp(request));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Override
    public List<AttendanceResponseDTO> getMyAttendance(String employeeId, int month, int year) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());
        return attendanceRepository.findByEmployeeIdAndDateBetweenOrderByDateDesc(employeeId, from, to)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public AttendanceSummaryDTO getMySummary(String employeeId, int month, int year) {
        PersonInfo person = resolvePerson(employeeId);

        LocalDate from       = LocalDate.of(year, month, 1);
        LocalDate to         = from.withDayOfMonth(from.lengthOfMonth());
        List<Attendance> records = attendanceRepository
                .findByEmployeeIdAndDateBetweenOrderByDateDesc(employeeId, from, to);

        int  totalWorkingDays = countWorkingDays(from, to);
        long presentCount     = records.stream().filter(r -> r.getType() == AttendanceType.PRESENT).count();
        long lateCount        = records.stream().filter(r -> r.getType() == AttendanceType.LATE).count();
        long wfhCount         = records.stream().filter(r -> r.getType() == AttendanceType.WORK_FROM_HOME).count();
        long ouCount          = records.stream().filter(r -> r.getType() == AttendanceType.ON_DUTY_OUTSIDE).count();
        long manualCount      = records.stream().filter(r -> r.getType() == AttendanceType.MANUAL).count();
        long biometricCount   = records.stream().filter(r ->
                r.getType() == AttendanceType.BIOMETRIC ||
                        r.getType() == AttendanceType.BIOMETRIC_REMOTE).count();
        int  totalMarked      = records.size();
        int  absentCount      = Math.max(0, totalWorkingDays - totalMarked);
        double pct            = totalWorkingDays == 0 ? 0 : (totalMarked * 100.0 / totalWorkingDays);

        double totalHours  = records.stream()
                .filter(r -> r.getWorkingHours() != null)
                .mapToDouble(Attendance::getWorkingHours)
                .sum();
        long daysWithHours = records.stream().filter(r -> r.getWorkingHours() != null).count();
        double avgHours    = daysWithHours == 0 ? 0 : totalHours / daysWithHours;

        return AttendanceSummaryDTO.builder()
                .employeeId(employeeId)
                .employeeName(person.name())
                .departmentName(person.departmentName())
                .totalDays(totalWorkingDays)
                .presentCount((int)(presentCount + manualCount + biometricCount))
                .lateCount((int)lateCount)
                .wfhCount((int)wfhCount)
                .onDutyOutsideCount((int)ouCount)
                .absentCount(absentCount)
                .attendancePercentage(Math.round(pct * 10.0) / 10.0)
                .totalWorkingHours(Math.round(totalHours * 100.0) / 100.0)
                .avgWorkingHours(Math.round(avgHours * 100.0) / 100.0)
                .records(records.stream().map(this::toDTO).collect(Collectors.toList()))
                .build();
    }

    @Override
    public List<AttendanceResponseDTO> getTodayAttendance() {
        LocalDate today = LocalDate.now(IST);
        return attendanceRepository.findByDateOrderByMarkedAtAsc(today)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<AttendanceResponseDTO> getMonthlyAttendance(int month, int year) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());
        return attendanceRepository.findByDateBetweenOrderByDateAscEmployeeNameAsc(from, to)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<AttendanceResponseDTO> getEmployeeAttendance(String employeeId, int month, int year) {
        return getMyAttendance(employeeId, month, year);
    }

    @Override
    public AttendanceResponseDTO addAdminNote(String attendanceId, String adminNote) {
        Attendance record = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));
        record.setAdminNote(adminNote);
        attendanceRepository.save(record);
        return toDTO(record);
    }

    @Override
    public void downloadEmployeeExcel(String employeeId, int month, int year,
                                      HttpServletResponse response) throws Exception {

        PersonInfo person = resolvePerson(employeeId);

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());

        List<Attendance> records = attendanceRepository
                .findByEmployeeIdAndDateBetweenOrderByDateDesc(employeeId, from, to);
        records = records.stream()
                .sorted(Comparator.comparing(Attendance::getDate))
                .collect(Collectors.toList());
        String monthLabel = from.format(DateTimeFormatter.ofPattern("MMMM_yyyy"));
        String safeName   = person.name().replaceAll("[^a-zA-Z0-9]", "_");

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"Attendance_" + safeName + "_" + monthLabel + ".xlsx\"");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Attendance " + monthLabel);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle lateStyle = workbook.createCellStyle();
            lateStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            lateStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);
            wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

            Row titleRow  = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Attendance Report — " + person.name()
                    + " — " + from.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));
            titleRow.setHeightInPoints(24);

            Row genRow = sheet.createRow(1);
            genRow.createCell(0).setCellValue("Generated on: " +
                    LocalDateTime.now(IST).format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 11));

            sheet.createRow(2);

            String[] headers = {
                    "S.No", "Date", "Employee Name", "Department",
                    "Type", "Reason", "Remarks", "Check-In (IST)",
                    "Check-Out (IST)", "Working Hrs", "Late Entry", "Biometric"
            };
            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(20);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter istDateFmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
            DateTimeFormatter istDtFmt   = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

            int rowNum = 4, sno = 1;
            for (Attendance r : records) {
                Row row = sheet.createRow(rowNum++);
                row.setHeightInPoints(
                        r.getRemarks() != null && r.getRemarks().length() > 30 ? 40 : 18);

                row.createCell(0).setCellValue(sno++);
                row.createCell(1).setCellValue(r.getDate() != null
                        ? r.getDate().format(istDateFmt) : "");
                row.createCell(2).setCellValue(r.getEmployeeName() != null
                        ? r.getEmployeeName() : "");
                row.createCell(3).setCellValue(r.getDepartmentName() != null
                        ? r.getDepartmentName() : "");
                row.createCell(4).setCellValue(r.getType() != null
                        ? r.getType().name() : "");
                row.createCell(5).setCellValue(r.getReason() != null
                        ? r.getReason() : "");

                Cell remarksCell = row.createCell(6);
                remarksCell.setCellValue(r.getRemarks() != null ? r.getRemarks() : "");
                remarksCell.setCellStyle(wrapStyle);

                row.createCell(7).setCellValue(r.getMarkedAt() != null
                        ? r.getMarkedAt().format(istDtFmt) : "");
                row.createCell(8).setCellValue(r.getCheckOutTime() != null
                        ? r.getCheckOutTime().format(istDtFmt) : "—");
                row.createCell(9).setCellValue(r.getWorkingHours() != null
                        ? String.format("%.2f", r.getWorkingHours()) : "—");
                row.createCell(10).setCellValue(r.isLateEntry() ? "YES" : "NO");
                row.createCell(11).setCellValue(r.isBiometricVerified() ? "✔" : "—");

                if (r.isLateEntry()) {
                    for (int c = 0; c <= 11; c++) {
                        if (row.getCell(c) != null) row.getCell(c).setCellStyle(lateStyle);
                    }
                }
            }

            int summaryStart = rowNum + 2;
            Row sumTitleRow  = sheet.createRow(summaryStart);
            Cell sumTitle    = sumTitleRow.createCell(0);
            sumTitle.setCellValue("Summary — " + person.name());
            CellStyle boldStyle = workbook.createCellStyle();
            Font bold = workbook.createFont(); bold.setBold(true);
            boldStyle.setFont(bold);
            sumTitle.setCellStyle(boldStyle);

            long present   = records.stream().filter(r -> r.getType() == AttendanceType.PRESENT).count();
            long late      = records.stream().filter(r -> r.getType() == AttendanceType.LATE).count();
            long wfh       = records.stream().filter(r -> r.getType() == AttendanceType.WORK_FROM_HOME).count();
            long ou        = records.stream().filter(r -> r.getType() == AttendanceType.ON_DUTY_OUTSIDE).count();
            long manual    = records.stream().filter(r -> r.getType() == AttendanceType.MANUAL).count();
            long biometric = records.stream().filter(r ->
                    r.getType() == AttendanceType.BIOMETRIC ||
                            r.getType() == AttendanceType.BIOMETRIC_REMOTE).count();
            long lateEntries    = records.stream().filter(Attendance::isLateEntry).count();
            long checkedOutCount = records.stream().filter(Attendance::isCheckedOut).count();
            long biometricVerified = records.stream().filter(Attendance::isBiometricVerified).count();
            double avgHours = records.stream()
                    .filter(r -> r.getWorkingHours() != null)
                    .mapToDouble(Attendance::getWorkingHours)
                    .average().orElse(0.0);

            String[][] summary = {
                    {"Total Records",      String.valueOf(records.size())},
                    {"Present",            String.valueOf(present)},
                    {"Late Arrival",       String.valueOf(late)},
                    {"Work From Home",     String.valueOf(wfh)},
                    {"On Duty Outside",    String.valueOf(ou)},
                    {"Manual / Admin",     String.valueOf(manual)},
                    {"Biometric",          String.valueOf(biometric)},
                    {"Late Submissions",   String.valueOf(lateEntries)},
                    {"Checked Out",        String.valueOf(checkedOutCount)},
                    {"Biometric Verified", String.valueOf(biometricVerified)},
                    {"Avg Working Hours",  String.format("%.2f", avgHours)},
            };
            for (int i = 0; i < summary.length; i++) {
                Row r = sheet.createRow(summaryStart + 1 + i);
                r.createCell(0).setCellValue(summary[i][0]);
                r.createCell(1).setCellValue(summary[i][1]);
            }

            int[] widths = {8, 16, 24, 22, 20, 30, 36, 22, 22, 12, 14, 12};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            workbook.write(response.getOutputStream());
        }
    }
    @Override
    public boolean hasMarkedToday(String employeeId) {
        LocalDate today = LocalDate.now(IST);
        return attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .map(a -> a.getMarkedAt() != null)
                .orElse(false);
    }

    @Override
    public boolean hasCheckedOutToday(String employeeId) {
        LocalDate today = LocalDate.now(IST);
        return attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .map(Attendance::isCheckedOut)
                .orElse(false);
    }

    // ── Excel Export ──────────────────────────────────────────────────────────

    @Override
    public void downloadMonthlyExcel(int month, int year, HttpServletResponse response) throws Exception {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());
        List<Attendance> records = attendanceRepository
                .findByDateBetweenOrderByDateAscEmployeeNameAsc(from, to);

        String monthLabel = from.format(DateTimeFormatter.ofPattern("MMMM_yyyy"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"Attendance_" + monthLabel + ".xlsx\"");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Attendance " + monthLabel);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short)14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle lateStyle = workbook.createCellStyle();
            lateStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            lateStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);
            wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

            Row titleRow  = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Attendance Report — " + from.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));
            titleRow.setHeightInPoints(24);

            Row genRow = sheet.createRow(1);
            genRow.createCell(0).setCellValue("Generated on: " +
                    LocalDateTime.now(IST).format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 11));

            sheet.createRow(2);

            String[] headers = {
                    "S.No", "Date", "Employee Name", "Department",
                    "Type", "Reason", "Remarks", "Check-In (IST)",
                    "Check-Out (IST)", "Working Hrs", "Late Entry", "Biometric"
            };
            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(20);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter istDateFmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
            DateTimeFormatter istDtFmt   = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

            int rowNum = 4;
            int sno    = 1;
            for (Attendance r : records) {
                Row row = sheet.createRow(rowNum++);
                row.setHeightInPoints(r.getRemarks() != null && r.getRemarks().length() > 30 ? 40 : 18);

                row.createCell(0).setCellValue(sno++);
                row.createCell(1).setCellValue(r.getDate() != null ? r.getDate().format(istDateFmt) : "");
                row.createCell(2).setCellValue(r.getEmployeeName() != null ? r.getEmployeeName() : "");
                row.createCell(3).setCellValue(r.getDepartmentName() != null ? r.getDepartmentName() : "");
                row.createCell(4).setCellValue(r.getType() != null ? r.getType().name() : "");
                row.createCell(5).setCellValue(r.getReason() != null ? r.getReason() : "");

                Cell remarksCell = row.createCell(6);
                remarksCell.setCellValue(r.getRemarks() != null ? r.getRemarks() : "");
                remarksCell.setCellStyle(wrapStyle);

                row.createCell(7).setCellValue(
                        r.getMarkedAt() != null ? r.getMarkedAt().format(istDtFmt) : "");
                row.createCell(8).setCellValue(
                        r.getCheckOutTime() != null ? r.getCheckOutTime().format(istDtFmt) : "—");
                row.createCell(9).setCellValue(
                        r.getWorkingHours() != null ? String.format("%.2f", r.getWorkingHours()) : "—");
                row.createCell(10).setCellValue(r.isLateEntry() ? "YES" : "NO");
                row.createCell(11).setCellValue(r.isBiometricVerified() ? "✔" : "—");

                if (r.isLateEntry()) {
                    for (int c = 0; c <= 11; c++) {
                        if (row.getCell(c) != null) row.getCell(c).setCellStyle(lateStyle);
                    }
                }
            }

            int summaryStart = rowNum + 2;
            Row sumTitleRow  = sheet.createRow(summaryStart);
            Cell sumTitle    = sumTitleRow.createCell(0);
            sumTitle.setCellValue("Summary");
            CellStyle boldStyle = workbook.createCellStyle();
            Font bold = workbook.createFont(); bold.setBold(true);
            boldStyle.setFont(bold);
            sumTitle.setCellStyle(boldStyle);

            long present     = records.stream().filter(r -> r.getType() == AttendanceType.PRESENT).count();
            long late        = records.stream().filter(r -> r.getType() == AttendanceType.LATE).count();
            long wfh         = records.stream().filter(r -> r.getType() == AttendanceType.WORK_FROM_HOME).count();
            long ou          = records.stream().filter(r -> r.getType() == AttendanceType.ON_DUTY_OUTSIDE).count();
            long manual      = records.stream().filter(r -> r.getType() == AttendanceType.MANUAL).count();
            long biometric   = records.stream().filter(r ->
                    r.getType() == AttendanceType.BIOMETRIC ||
                            r.getType() == AttendanceType.BIOMETRIC_REMOTE).count();
            long lateEntries    = records.stream().filter(Attendance::isLateEntry).count();
            long checkedOutCount = records.stream().filter(Attendance::isCheckedOut).count();
            long biometricVerified = records.stream().filter(Attendance::isBiometricVerified).count();
            double avgHours  = records.stream()
                    .filter(r -> r.getWorkingHours() != null)
                    .mapToDouble(Attendance::getWorkingHours)
                    .average().orElse(0.0);

            String[][] summary = {
                    {"Total Records",       String.valueOf(records.size())},
                    {"Present",             String.valueOf(present)},
                    {"Late Arrival",        String.valueOf(late)},
                    {"Work From Home",      String.valueOf(wfh)},
                    {"On Duty Outside",     String.valueOf(ou)},
                    {"Manual / Admin",      String.valueOf(manual)},
                    {"Biometric",           String.valueOf(biometric)},
                    {"Late Submissions",    String.valueOf(lateEntries)},
                    {"Checked Out",         String.valueOf(checkedOutCount)},
                    {"Biometric Verified",  String.valueOf(biometricVerified)},
                    {"Avg Working Hours",   String.format("%.2f", avgHours)},
            };
            for (int i = 0; i < summary.length; i++) {
                Row r = sheet.createRow(summaryStart + 1 + i);
                r.createCell(0).setCellValue(summary[i][0]);
                r.createCell(1).setCellValue(summary[i][1]);
            }

            int[] widths = {8, 16, 24, 22, 20, 30, 36, 22, 22, 12, 14, 12};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            workbook.write(response.getOutputStream());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AttendanceResponseDTO toDTO(Attendance a) {
        return toDTO(a, isOfficeIp(a.getClientIp()));
    }

    private AttendanceResponseDTO toDTO(Attendance a, boolean isOfficeNetwork) {
        return AttendanceResponseDTO.builder()
                .id(a.getId())
                .employeeId(a.getEmployeeId())
                .employeeName(a.getEmployeeName())
                .departmentId(a.getDepartmentId())
                .departmentName(a.getDepartmentName())
                .date(a.getDate())
                .markedAt(a.getMarkedAt())
                .type(a.getType())
                .reason(a.getReason())
                .remarks(a.getRemarks())
                .lateEntry(a.isLateEntry())
                .checkOutTime(a.getCheckOutTime())
                .checkOutRemarks(a.getCheckOutRemarks())
                .checkedOut(a.isCheckedOut())
                .workingHours(a.getWorkingHours())
                .adminNote(a.getAdminNote())
                .createdAt(a.getCreatedAt())
                .biometricVerified(a.isBiometricVerified())
                .isOfficeNetwork(isOfficeNetwork)
                .build();
    }

    private boolean isOfficeIp(String ip) {
        if (ip == null || ip.isBlank()) return false;

        boolean lanMatch = OFFICE_LAN_PREFIXES.stream()
                .anyMatch(prefix -> ip.startsWith(prefix) || ip.equals(prefix));
        if (lanMatch) return true;

        return OFFICE_CIDR_RANGES.stream().anyMatch(cidr -> isIpInCidr(ip, cidr));
    }

    private boolean isIpInCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) return false;

            int prefixLength = Integer.parseInt(parts[1].trim());
            if (prefixLength < 0 || prefixLength > 32) return false;

            int ipInt      = ipToInt(InetAddress.getByName(ip.trim()));
            int subnetInt  = ipToInt(InetAddress.getByName(parts[0].trim()));

            int mask = prefixLength == 0 ? 0 : (0xFFFFFFFF << (32 - prefixLength));

            return (ipInt & mask) == (subnetInt & mask);
        } catch (UnknownHostException | NumberFormatException e) {
            log.warn("[Attendance] Invalid IP/CIDR while checking office network: ip={} cidr={} err={}",
                    ip, cidr, e.getMessage());
            return false;
        }
    }

    private int ipToInt(InetAddress address) {
        byte[] bytes = address.getAddress();
        int result = 0;
        for (byte b : bytes) {
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }

    private int countWorkingDays(LocalDate from, LocalDate to) {
        int count = 0;
        LocalDate d = from;
        while (!d.isAfter(to)) {
            if (d.getDayOfWeek() != DayOfWeek.SUNDAY) count++;
            d = d.plusDays(1);
        }
        return count;
    }
}