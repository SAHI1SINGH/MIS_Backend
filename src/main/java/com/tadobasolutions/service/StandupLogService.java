package com.tadobasolutions.service;

import com.tadobasolutions.dto.request.EodRemarksRequestDTO;
import com.tadobasolutions.dto.request.EodReportRequestDTO;
import com.tadobasolutions.entity.Employee;
import com.tadobasolutions.entity.StandupLog;
import com.tadobasolutions.exception.BadRequestException;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.repository.EmployeeRepository;
import com.tadobasolutions.repository.StandupLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StandupLogService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final int MAX_ROWS = 15;

    private final StandupLogRepository standupRepo;
    private final EmployeeRepository employeeRepository;

    // ── EMPLOYEE: auto-save draft (call as often as you like) ─────────────────
    public StandupLog saveDraft(EodReportRequestDTO dto) {
        Employee emp = requireEmployeeWithDept(dto);
        LocalDate today = LocalDate.now(IST);

        StandupLog log = standupRepo
                .findFirstBySubmittedByIdAndStandupDateAndSubmittedByAdminFalse(emp.getId(), today)
                .orElseGet(StandupLog::new);
        if (log.getId() != null && !log.isDraft()) {
            throw new BadRequestException("Today's report is already submitted and locked");
        }
        fill(log, dto, emp, today, false);   // lenient: empty / half-typed rows allowed
        log.setDraft(true);
        log.setDraftSavedAt(LocalDateTime.now(IST));
        return standupRepo.save(log);
    }

    // ── EMPLOYEE: final submit -> locks ───────────────────────────────────────
    public StandupLog submitByEmployee(EodReportRequestDTO dto) {
        Employee emp = requireEmployeeWithDept(dto);
        LocalDate today = LocalDate.now(IST);

        StandupLog log = standupRepo
                .findFirstBySubmittedByIdAndStandupDateAndSubmittedByAdminFalse(emp.getId(), today)
                .orElseGet(StandupLog::new);
        if (log.getId() != null && !log.isDraft()) {
            throw new BadRequestException("Report already submitted for " + today);
        }
        fill(log, dto, emp, today, true);    // strict: at least 1 row with work text
        log.setDraft(false);
        log.setDraftSavedAt(null);
        log.setSubmittedAt(LocalDateTime.now(IST));
        return standupRepo.save(log);
    }

    // ── ADMIN: submit for any department (dropdown), date optional ────────────
    public StandupLog submitByAdmin(EodReportRequestDTO dto) {
        if (dto.getDepartmentId() == null || dto.getDepartmentId().isBlank()) {
            throw new BadRequestException("departmentId is required");
        }
        String submitterId = dto.getEmployeeId();
        String submitterName = (dto.getSubmittedByName() != null && !dto.getSubmittedByName().isBlank())
                ? dto.getSubmittedByName().trim() : "Admin";
        if (dto.getEmployeeId() != null && !dto.getEmployeeId().isBlank()) {
            Optional<Employee> admin = employeeRepository.findById(dto.getEmployeeId());
            if (admin.isPresent()) {
                submitterId = admin.get().getId();
                submitterName = admin.get().getName();
            }
        }
        LocalDate date = dto.getReportDate() != null ? dto.getReportDate() : LocalDate.now(IST);
        return create(dto, dto.getDepartmentId(), dto.getDepartmentName(),
                date, submitterId, submitterName, true);
    }

    // ── ADMIN: fill Remarks (star rating + text) on rows ──────────────────────
    public StandupLog addRemarks(String logId, EodRemarksRequestDTO dto) {
        StandupLog log = getById(logId);

        if (log.isDraft()) {
            throw new BadRequestException("Report is still a draft and not submitted yet");
        }
        if (dto.getRemarks() == null || dto.getRemarks().isEmpty()) {
            throw new BadRequestException("remarks list is empty");
        }

        for (EodRemarksRequestDTO.RowRemark r : dto.getRemarks()) {
            if (r.getSrNo() == null) {
                throw new BadRequestException("srNo is required for every remark");
            }
            StandupLog.EodItem item = log.getItems().stream()
                    .filter(i -> r.getSrNo().equals(i.getSrNo()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("No row with srNo " + r.getSrNo()));

            if (r.getRating() != null) {
                if (r.getRating() < 0 || r.getRating() > 5) {
                    throw new BadRequestException(
                            "rating must be between 0 and 5 (srNo " + r.getSrNo() + ")");
                }
                item.setRating(r.getRating());
            }
            if (r.getRemarkText() != null) {
                item.setRemarkText(r.getRemarkText().trim());
            }
        }

        log.setRemarkedBy(dto.getRemarkedBy());
        log.setRemarkedAt(LocalDateTime.now(IST));
        return standupRepo.save(log);
    }

    // ── EMPLOYEE: today's own report (draft or submitted) ─────────────────────
    public Optional<StandupLog> getToday(String employeeId) {
        return standupRepo.findFirstBySubmittedByIdAndStandupDateAndSubmittedByAdminFalse(
                employeeId, LocalDate.now(IST));
    }

    // ── EMPLOYEE: own history (submitted only) ────────────────────────────────
    public List<StandupLog> getHistory(String employeeId) {
        return standupRepo.findBySubmittedByIdAndSubmittedByAdminFalseOrderByStandupDateDesc(employeeId)
                .stream()
                .filter(l -> !l.isDraft())
                .toList();
    }

    // ── ADMIN: submitted reports for a date, sorted by department then person ─
    public List<StandupLog> getByDate(LocalDate date, String departmentId) {
        List<StandupLog> list = (departmentId != null && !departmentId.isBlank())
                ? standupRepo.findByStandupDateAndDepartmentIdOrderByDepartmentNameAsc(date, departmentId)
                : standupRepo.findByStandupDateOrderByDepartmentNameAsc(date);
        return list.stream()
                .filter(l -> !l.isDraft())
                .sorted(Comparator.comparing((StandupLog l) -> nz(l.getDepartmentName()))
                        .thenComparing(l -> nz(l.getSubmittedByName())))
                .toList();
    }

    // ── Single report (both sides) ────────────────────────────────────────────
    public StandupLog getById(String id) {
        return standupRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
    }

    // ── ADMIN: Excel export — all submitted reports in ONE sheet, with filter ─
    // One row per task. Summary columns are filled on the first row of each report.
    public byte[] exportExcel(LocalDate from, LocalDate to) throws IOException {
        if (from == null) from = LocalDate.now(IST);
        if (to == null) to = from;
        if (to.isBefore(from)) {
            throw new BadRequestException("'to' date cannot be before 'from' date");
        }

        List<StandupLog> reports = standupRepo
                .findByStandupDateBetweenOrderByStandupDateAscDepartmentNameAsc(from, to)
                .stream()
                .sorted(Comparator.comparing(StandupLog::getStandupDate)
                        .thenComparing(l -> nz(l.getDepartmentName()))
                        .thenComparing(l -> nz(l.getSubmittedByName())))
                .toList();

        String[] headers = {
                "Date", "Department", "Employee", "S. No.", "Work Particulars / Tasks", "Completed By",
                "Results / Pending / Follow-Up", "Rating (1-5)", "Stars", "Remarks",
                "Submitted At",
                "Key Achievements Today", "Challenges / Issues Faced",
                "Support Required From Management", "Planned Work For Tomorrow"
        };
        int[] widths = { 14, 24, 22, 8, 42, 22, 38, 12, 12, 34, 20, 40, 40, 40, 40 };

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("EOD Reports");

            // ── styles ────────────────────────────────────────────────────────
            Font white = wb.createFont();
            white.setBold(true);
            white.setColor(IndexedColors.WHITE.getIndex());

            CellStyle head = wb.createCellStyle();
            head.setFont(white);
            head.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            head.setAlignment(HorizontalAlignment.CENTER);
            head.setVerticalAlignment(VerticalAlignment.CENTER);
            head.setWrapText(true);
            applyBorder(head);

            CellStyle text = wb.createCellStyle();
            text.setWrapText(true);
            text.setVerticalAlignment(VerticalAlignment.TOP);
            applyBorder(text);

            CellStyle center = wb.createCellStyle();
            center.cloneStyleFrom(text);
            center.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.cloneStyleFrom(center);
            dateStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd-mmm-yyyy"));

            // ── header row ────────────────────────────────────────────────────
            Row hr = sheet.createRow(0);
            hr.setHeightInPoints(30);
            for (int c = 0; c < headers.length; c++) {
                Cell cell = hr.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(head);
                sheet.setColumnWidth(c, widths[c] * 256);
            }

            // ── data rows ─────────────────────────────────────────────────────
            int r = 1;
            for (StandupLog log : reports) {
                // skip drafts and old-style documents without a table
                if (log.isDraft() || log.getItems() == null || log.getItems().isEmpty()) continue;

                Date date = Date.from(log.getStandupDate().atStartOfDay(IST).toInstant());
                String submittedAt = (log.getSubmittedAt() == null ? "" : log.getSubmittedAt().format(timeFmt))
                        + (log.isAutoSubmitted() ? " (auto)" : "");
                String who = nz(log.getSubmittedByName()) + (log.isSubmittedByAdmin() ? " (Admin)" : "");

                boolean first = true;
                for (StandupLog.EodItem item : log.getItems()) {
                    Row row = sheet.createRow(r++);
                    int rating = item.getRating() == null ? 0 : item.getRating();

                    Cell d = row.createCell(0);
                    d.setCellValue(date);
                    d.setCellStyle(dateStyle);

                    put(row, 1, nz(log.getDepartmentName()), text);
                    put(row, 2, who, text);

                    if (item.getSrNo() != null) {
                        Cell sn = row.createCell(3);
                        sn.setCellValue(item.getSrNo());
                        sn.setCellStyle(center);
                    } else {
                        put(row, 3, "", center);
                    }
                    put(row, 4, nz(item.getWorkParticulars()), text);
                    put(row, 5, nz(item.getCompletedBy()), text);
                    put(row, 6, nz(item.getResultOrFollowUp()), text);

                    if (rating > 0) {
                        Cell rt = row.createCell(7);
                        rt.setCellValue(rating);
                        rt.setCellStyle(center);
                        put(row, 8, "★".repeat(rating) + "☆".repeat(5 - rating), center);
                    } else {
                        put(row, 7, "", center);
                        put(row, 8, "", center);
                    }
                    put(row, 9, nz(item.getRemarkText()), text);
                    put(row, 10, submittedAt, text);

                    put(row, 11, first ? nz(log.getKeyAchievements()) : "", text);
                    put(row, 12, first ? nz(log.getChallenges()) : "", text);
                    put(row, 13, first ? nz(log.getSupportRequired()) : "", text);
                    put(row, 14, first ? nz(log.getTomorrowPlan()) : "", text);
                    first = false;
                }
            }

            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(r - 1, 0), 0, headers.length - 1));
            sheet.createFreezePane(0, 1);

            wb.write(out);
            return out.toByteArray();
        }
    }

    private void put(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void applyBorder(CellStyle s) {
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Employee requireEmployeeWithDept(EodReportRequestDTO dto) {
        if (dto.getEmployeeId() == null || dto.getEmployeeId().isBlank()) {
            throw new BadRequestException("employeeId is required");
        }
        Employee emp = findEmployee(dto.getEmployeeId());
        if (emp.getDepartmentId() == null || emp.getDepartmentId().isBlank()) {
            throw new BadRequestException("No department assigned to this employee");
        }
        return emp;
    }

    // copies DTO + employee info into the log (used by draft and employee submit)
    private void fill(StandupLog log, EodReportRequestDTO dto, Employee emp,
                      LocalDate date, boolean strict) {
        log.setStandupDate(date);
        log.setDepartmentId(emp.getDepartmentId());
        log.setDepartmentName(emp.getDepartmentName());
        log.setSubmittedById(emp.getId());
        log.setSubmittedByName(emp.getName());
        log.setSubmittedByAdmin(false);
        log.setItems(cleanItems(dto.getItems(), strict));
        log.setKeyAchievements(trim(dto.getKeyAchievements()));
        log.setChallenges(trim(dto.getChallenges()));
        log.setSupportRequired(trim(dto.getSupportRequired()));
        log.setTomorrowPlan(trim(dto.getTomorrowPlan()));
    }

    // admin-on-behalf creation
    private StandupLog create(EodReportRequestDTO dto, String departmentId, String departmentName,
                              LocalDate date, String submitterId, String submitterName,
                              boolean byAdmin) {

        standupRepo.findByDepartmentIdAndStandupDateAndSubmittedById(departmentId, date, submitterId)
                .ifPresent(existing -> {
                    throw new BadRequestException(
                            "Report already submitted for " + departmentName + " on " + date);
                });

        StandupLog log = new StandupLog();
        log.setStandupDate(date);
        log.setDepartmentId(departmentId);
        log.setDepartmentName(departmentName);
        log.setSubmittedById(submitterId);
        log.setSubmittedByName(submitterName);
        log.setSubmittedByAdmin(byAdmin);
        log.setSubmittedAt(LocalDateTime.now(IST));
        log.setDraft(false);
        log.setItems(cleanItems(dto.getItems(), true));
        log.setKeyAchievements(trim(dto.getKeyAchievements()));
        log.setChallenges(trim(dto.getChallenges()));
        log.setSupportRequired(trim(dto.getSupportRequired()));
        log.setTomorrowPlan(trim(dto.getTomorrowPlan()));

        return standupRepo.save(log);
    }

    // strict (submit): drop rows without work text, need >= 1 row
    // lenient (draft): keep a row if ANY field has text, empty list allowed
    private List<StandupLog.EodItem> cleanItems(List<EodReportRequestDTO.Item> raw, boolean strict) {
        List<StandupLog.EodItem> items = new ArrayList<>();
        if (raw != null) {
            for (EodReportRequestDTO.Item in : raw) {
                if (in == null) continue;
                boolean hasWork = in.getWorkParticulars() != null && !in.getWorkParticulars().isBlank();
                boolean hasAny = hasWork
                        || (in.getCompletedBy() != null && !in.getCompletedBy().isBlank())
                        || (in.getResultOrFollowUp() != null && !in.getResultOrFollowUp().isBlank());
                if (strict ? !hasWork : !hasAny) continue;

                StandupLog.EodItem item = new StandupLog.EodItem();
                item.setSrNo(items.size() + 1);
                item.setWorkParticulars(trim(in.getWorkParticulars()));
                item.setCompletedBy(trim(in.getCompletedBy()));
                item.setResultOrFollowUp(trim(in.getResultOrFollowUp()));
                item.setRating(0);
                items.add(item);
            }
        }
        if (strict && items.isEmpty()) {
            throw new BadRequestException("At least one work row is required");
        }
        if (items.size() > MAX_ROWS) {
            throw new BadRequestException("Maximum " + MAX_ROWS + " rows allowed");
        }
        return items;
    }
    public String resolveEmployeeId(String principal) {
        if (principal == null || principal.isBlank()) {
            throw new BadRequestException("Not authenticated");
        }
        return employeeRepository.findByEmail(principal)
                .map(Employee::getId)
                .orElseGet(() -> employeeRepository.findById(principal)
                        .map(Employee::getId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Employee not found: " + principal)));
    }
    public int autoSubmitDrafts(boolean includeToday) {
        LocalDate limit = includeToday ? LocalDate.now(IST) : LocalDate.now(IST).minusDays(1);
        int count = 0;
        for (StandupLog log : standupRepo.findByDraftTrueAndStandupDateLessThanEqual(limit)) {
            List<StandupLog.EodItem> kept = new ArrayList<>();
            if (log.getItems() != null) {
                for (StandupLog.EodItem it : log.getItems()) {
                    if (it.getWorkParticulars() == null || it.getWorkParticulars().isBlank()) continue;
                    it.setSrNo(kept.size() + 1);
                    kept.add(it);
                }
            }
            if (kept.isEmpty()) continue;

            log.setItems(kept);
            log.setDraft(false);
            log.setAutoSubmitted(true);
            log.setSubmittedAt(LocalDateTime.now(IST));
            standupRepo.save(log);
            count++;
        }
        return count;
    }

    private Employee findEmployee(String employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }
}