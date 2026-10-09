package com.tadobasolutions.service.Impl;

import com.tadobasolutions.dto.TaskStatusCountDTO;
import com.tadobasolutions.dto.request.TaskRequestDTO;
import com.tadobasolutions.dto.response.ReportSummaryDTO;
import com.tadobasolutions.dto.response.TaskResponseDTO;
import com.tadobasolutions.dto.response.TeamReportDTO;
import com.tadobasolutions.entity.Employee;
import com.tadobasolutions.entity.Intern;
import com.tadobasolutions.entity.Task;
import com.tadobasolutions.entity.enums.Role;
import com.tadobasolutions.entity.enums.TaskStatus;
import com.tadobasolutions.exception.BadRequestException;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.repository.AdminRepository;
import com.tadobasolutions.repository.EmployeeRepository;
import com.tadobasolutions.repository.InternRepository;
import com.tadobasolutions.repository.TaskRepository;
import com.tadobasolutions.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import com.tadobasolutions.service.Impl.FcmNotificationService;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository     taskRepository;
    private final EmployeeRepository employeeRepository;
    private final InternRepository   internRepository;
    private final AdminRepository    adminRepository;
    private final FcmNotificationService fcmService;

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private TaskResponseDTO mapToDTO(Task task) {
        return new TaskResponseDTO(
                task.getId(),
                task.getTask(),
                task.getDescription(),
                task.getTargetDate(),
                task.getSubmissionDate(),
                task.getStatus(),
                task.getEmployeeId(),
                task.getEmployeeName(),
                task.getAssignedDate(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getAssignedById(),
                task.getAssignedByName(),
                task.getAssignedByRole(),
                task.getEmpRating(),
                task.getAdminRating(),
                task.getSubAdminRating(),
                task.getAdminRemarks()
        );
    }

    // ─── Assigner resolution ──────────────────────────────────────────────────
    // Checks admins collection first, then employees collection (for sub-admins).

    private record AssignerInfo(String id, String name, Role role) {}

    private AssignerInfo resolveAssigner(String assignedById) {
        if (assignedById == null || assignedById.isBlank()) {
            throw new BadRequestException("assignedById is required");
        }
        // Try admin collection first
        var adminOpt = adminRepository.findById(assignedById);
        if (adminOpt.isPresent()) {
            return new AssignerInfo(adminOpt.get().getId(), adminOpt.get().getName(), Role.ADMIN);
        }
        // Then check employees collection — must be SUB_ADMIN role
        var empOpt = employeeRepository.findById(assignedById);
        if (empOpt.isPresent()) {
            Employee emp = empOpt.get();
            if (emp.getRole() != Role.SUB_ADMIN) {
                throw new BadRequestException(
                        "Employee " + emp.getName() + " does not have the SUB_ADMIN role and cannot assign tasks");
            }
            return new AssignerInfo(emp.getId(), emp.getName(), Role.SUB_ADMIN);
        }
        throw new ResourceNotFoundException("Assigner not found with id: " + assignedById);
    }

    // ─── Assignee resolution ────────────────────────────────────────────────
    // Checks employees collection first, then interns collection.

    private record AssigneeInfo(String id, String name, List<String> fcmTokens) {}

    private AssigneeInfo resolveAssignee(String employeeId) {
        var empOpt = employeeRepository.findById(employeeId);
        if (empOpt.isPresent()) {
            Employee e = empOpt.get();
            return new AssigneeInfo(e.getId(), e.getName(), e.getFcmTokens());
        }
        var internOpt = internRepository.findById(employeeId);
        if (internOpt.isPresent()) {
            Intern i = internOpt.get();
            return new AssigneeInfo(i.getId(), i.getName(), i.getFcmTokens());
        }
        throw new ResourceNotFoundException("Employee/Intern not found");
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @Override
    public TaskResponseDTO createTask(TaskRequestDTO dto) {
        AssigneeInfo assignee = resolveAssignee(dto.getEmployeeId());
        AssignerInfo assigner = resolveAssigner(dto.getAssignedById());

        Task task = new Task();
        task.setTask(dto.getTask());
        task.setDescription(dto.getDescription());
        task.setTargetDate(dto.getTargetDate());
        task.setAssignedDate(LocalDateTime.now());
        task.setEmployeeId(assignee.id());
        task.setEmployeeName(assignee.name());
        task.setAssignedById(assigner.id());
        task.setAssignedByName(assigner.name());
        task.setAssignedByRole(assigner.role());
        task.setStatus(TaskStatus.PENDING);
        task.setEmpRating(0);
        task.setAdminRating(0);
        task.setSubAdminRating(0);

        Task saved = taskRepository.save(task);

        // Push notification — tells assignee (all devices) who assigned the task
        if (assignee.fcmTokens() != null && !assignee.fcmTokens().isEmpty()) {
            String assignerLabel = assigner.role() == Role.SUB_ADMIN ? "Sub-Admin" : "Admin";
            fcmService.sendToTokens(
                    assignee.fcmTokens(),
                    "📋 New Task Assigned",
                    "Assigned by " + assignerLabel + " " + assigner.name() + ": " + dto.getTask(),
                    Map.of(
                            "taskId",         saved.getId(),
                            "type",           "TASK_ASSIGNED",
                            "assignedByName", assigner.name(),
                            "assignedByRole", assigner.role().name()
                    )
            );
        }

        return mapToDTO(saved);
    }

    @Override
    public TaskResponseDTO updateTask(String id, TaskRequestDTO dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (dto.getTask() != null && !dto.getTask().isBlank()) task.setTask(dto.getTask());
        if (dto.getDescription() != null)    task.setDescription(dto.getDescription());
        if (dto.getTargetDate() != null)     task.setTargetDate(dto.getTargetDate());
        if (dto.getSubmissionDate() != null) task.setSubmissionDate(dto.getSubmissionDate());
        if (dto.getEmpRating() != null)      task.setEmpRating(dto.getEmpRating());
        if (dto.getAdminRating() != null)    task.setAdminRating(dto.getAdminRating());
        if (dto.getSubAdminRating() != null) task.setSubAdminRating(dto.getSubAdminRating());
        if (dto.getAdminRemarks() != null)   task.setAdminRemarks(dto.getAdminRemarks());

        if (dto.getEmployeeId() != null) {
            AssigneeInfo assignee = resolveAssignee(dto.getEmployeeId());
            task.setEmployeeId(assignee.id());
            task.setEmployeeName(assignee.name());
        }

        // Allow re-assigning (e.g. admin takes over a sub-admin's task)
        if (dto.getAssignedById() != null && !dto.getAssignedById().isBlank()) {
            AssignerInfo assigner = resolveAssigner(dto.getAssignedById());
            task.setAssignedById(assigner.id());
            task.setAssignedByName(assigner.name());
            task.setAssignedByRole(assigner.role());
        }

        return mapToDTO(taskRepository.save(task));
    }

    @Override
    public void deleteTask(String id) {
        if (!taskRepository.existsById(id)) throw new ResourceNotFoundException("Task not found");
        taskRepository.deleteById(id);
    }

    @Override
    public TaskResponseDTO getTaskById(String id) {
        return mapToDTO(taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found")));
    }

    @Override
    public List<TaskResponseDTO> getAllTasks(TaskStatus status, String employeeId, LocalDate targetDate) {
        LocalDateTime startDate = null, endDate = null;
        if (targetDate != null) {
            startDate = targetDate.atStartOfDay();
            endDate   = targetDate.plusDays(1).atStartOfDay();
        }
        return taskRepository.findTasksByFilters(status, employeeId, startDate, endDate)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public TaskResponseDTO submitTask(String id, Integer empRating) {

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        LocalDateTime now = LocalDateTime.now();

        task.setSubmissionDate(now);

        task.setStatus(
                !now.isAfter(task.getTargetDate())
                        ? TaskStatus.COMPLETED
                        : TaskStatus.LATE
        );

        // Save employee rating
        if (empRating != null) {
            task.setEmpRating(empRating);
        }

        Task saved = taskRepository.save(task);

        log.info("Task {} submitted with status {}", id, saved.getStatus());

        // ── Notify all admins (all devices) ───────────────────────────────
        String notifTitle = "✅ Task Submitted";
        String notifBody  = task.getEmployeeName() + " submitted the task: " + task.getTask();

        List<String> adminTokens = adminRepository.findAll().stream()
                .filter(a -> a.getFcmTokens() != null)
                .flatMap(a -> a.getFcmTokens().stream())
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .collect(Collectors.toList());
        if (!adminTokens.isEmpty()) {
            fcmService.sendToTokens(adminTokens, notifTitle, notifBody,
                    Map.of("taskId", saved.getId(), "type", "TASK_SUBMITTED",
                            "employeeName", task.getEmployeeName()));
        }

        // ── Also notify the assigner if SUB_ADMIN (all their devices) ─────
        if (task.getAssignedByRole() == Role.SUB_ADMIN && task.getAssignedById() != null) {
            employeeRepository.findById(task.getAssignedById()).ifPresent(subAdmin -> {
                if (subAdmin.getFcmTokens() != null && !subAdmin.getFcmTokens().isEmpty()) {
                    fcmService.sendToTokens(subAdmin.getFcmTokens(), notifTitle, notifBody,
                            Map.of("taskId", saved.getId(), "type", "TASK_SUBMITTED",
                                    "employeeName", task.getEmployeeName()));
                }
            });
        }

        return mapToDTO(saved);
    }

    // ─── Stats ────────────────────────────────────────────────────────────────

    @Override
    public TaskStatusCountDTO getTaskStatusCounts(String employeeId) {
        List<Task> tasks = employeeId != null
                ? taskRepository.findByEmployeeId(employeeId)
                : taskRepository.findAll();
        long total     = tasks.size();
        long completed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.LATE).count();
        long pending   = tasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count();
        long late      = tasks.stream().filter(t -> t.getStatus() == TaskStatus.LATE).count();
        long overdue   = tasks.stream().filter(t -> t.getStatus() == TaskStatus.OVERDUE).count();
        return new TaskStatusCountDTO(total, completed, pending, late, overdue);
    }

    @Override
    public String getEmployeeNameById(String id) {
        return employeeRepository.findById(id).map(Employee::getName).orElse("employee");
    }

    // ─── Reports ──────────────────────────────────────────────────────────────

    @Override
    public ReportSummaryDTO getMonthlyReportSummary(String employeeId, int month, int year) {
        List<Task> tasks = taskRepository.findByEmployeeId(employeeId).stream()
                .filter(t -> t.getTargetDate() != null
                        && t.getTargetDate().getMonthValue() == month
                        && t.getTargetDate().getYear() == year)
                .collect(Collectors.toList());

        if (tasks.isEmpty()) return new ReportSummaryDTO(0, 0, 0, 0, 0, 0, 0, 0, 0);

        long total     = tasks.size();
        long completed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.LATE).count();
        long pending   = tasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count();
        long late      = tasks.stream().filter(t -> t.getStatus() == TaskStatus.LATE).count();
        long overdue   = tasks.stream().filter(t -> t.getStatus() == TaskStatus.OVERDUE).count();

        double avgEmpRating = tasks.stream()
                .filter(t -> t.getEmpRating() != null && t.getEmpRating() > 0)
                .mapToInt(Task::getEmpRating).average().orElse(0);
        double avgAdminRating = tasks.stream()
                .filter(t -> t.getAdminRating() != null && t.getAdminRating() > 0)
                .mapToInt(Task::getAdminRating).average().orElse(0);

        int workPercentage     = total == 0 ? 0 : BigDecimal.valueOf(((double) completed / total) * 100).setScale(0, RoundingMode.HALF_UP).intValue();
        int adminScorePercent  = BigDecimal.valueOf((avgAdminRating / 10) * 100).setScale(0, RoundingMode.HALF_UP).intValue();

        return new ReportSummaryDTO(total, completed, pending, late, overdue,
                round1(avgEmpRating), round1(avgAdminRating), workPercentage, adminScorePercent);
    }

    @Override
    public byte[] exportMonthlyReportExcel(String employeeId, int month, int year) {
        List<Task> tasks = taskRepository.findByEmployeeId(employeeId).stream()
                .filter(t -> t.getTargetDate() != null
                        && t.getTargetDate().getMonthValue() == month
                        && t.getTargetDate().getYear() == year)
                .collect(Collectors.toList());

        ReportSummaryDTO summary    = getMonthlyReportSummary(employeeId, month, year);
        String           employeeName = tasks.isEmpty() ? "Employee" : tasks.get(0).getEmployeeName();

        Workbook workbook = new XSSFWorkbook();
        Sheet    sheet    = workbook.createSheet("Employee Report");

        CellStyle headerStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        headerStyle.setFont(boldFont);

        CellStyle greenStyle  = colorStyle(workbook, IndexedColors.LIGHT_GREEN);
        CellStyle yellowStyle = colorStyle(workbook, IndexedColors.LIGHT_YELLOW);
        CellStyle redStyle    = colorStyle(workbook, IndexedColors.ROSE);

        int rowIndex = 0;
        sheet.createRow(rowIndex++).createCell(0).setCellValue("Employee: " + employeeName);
        sheet.createRow(rowIndex++).createCell(0).setCellValue("Month / Year: " + getMonthName(month) + " " + year);
        rowIndex++;
        Row title = sheet.createRow(rowIndex++);
        title.createCell(0).setCellValue("Monthly Report Summary");
        title.getCell(0).setCellStyle(headerStyle);
        setColoredRow(sheet.createRow(rowIndex++), "Total Tasks: ", summary.getTotalTasks(), headerStyle);
        setColoredRow(sheet.createRow(rowIndex++), "Completed: ",   summary.getCompleted(),  greenStyle);
        setColoredRow(sheet.createRow(rowIndex++), "Pending: ",     summary.getPending(),    yellowStyle);
        setColoredRow(sheet.createRow(rowIndex++), "Late: ",        summary.getLateSubmission(),       redStyle);
        setColoredRow(sheet.createRow(rowIndex++), "Overdue: ",     summary.getOverdue(),    redStyle);
        sheet.createRow(rowIndex++).createCell(0).setCellValue("Avg Emp Rating: "   + summary.getAvgEmployeeRating());
        sheet.createRow(rowIndex++).createCell(0).setCellValue("Avg Admin Rating: " + summary.getAvgAdminRating());
        rowIndex += 2;

        String[] columns = { "S.No", "Title", "Description", "Assigned Date", "Target Date",
                "Submission Date", "Status", "Assigned By",
                "Emp Rating", "Admin Rating", "Sub-Admin Rating", "Admin Remarks" };
        Row tableHeader = sheet.createRow(rowIndex++);
        for (int i = 0; i < columns.length; i++) {
            Cell c = tableHeader.createCell(i);
            c.setCellValue(columns[i]);
            c.setCellStyle(headerStyle);
        }
        int count = 1;
        for (Task t : tasks) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(count++);
            row.createCell(1).setCellValue(t.getTask());
            row.createCell(2).setCellValue(t.getDescription());
            row.createCell(3).setCellValue(formatDateTime(t.getAssignedDate()));
            row.createCell(4).setCellValue(formatDateTime(t.getTargetDate()));
            row.createCell(5).setCellValue(t.getSubmissionDate() != null ? formatDateTime(t.getSubmissionDate()) : "");
            row.createCell(6).setCellValue(t.getStatus().toString());
            row.createCell(7).setCellValue(t.getAssignedByName() != null
                    ? t.getAssignedByName() + " (" + t.getAssignedByRole() + ")" : "");
            row.createCell(8).setCellValue(t.getEmpRating()      != null ? t.getEmpRating()      : 0);
            row.createCell(9).setCellValue(t.getAdminRating()    != null ? t.getAdminRating()    : 0);
            row.createCell(10).setCellValue(t.getSubAdminRating() != null ? t.getSubAdminRating() : 0);
            row.createCell(11).setCellValue(t.getAdminRemarks()  != null ? t.getAdminRemarks()   : "");
        }
        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try { workbook.write(out); workbook.close(); }
        catch (Exception e) { log.error("Failed to write Excel report", e); }
        return out.toByteArray();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private CellStyle colorStyle(Workbook wb, IndexedColors color) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(color.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private void setColoredRow(Row row, String label, long value, CellStyle style) {
        Cell c = row.createCell(0);
        c.setCellValue(label + value);
        c.setCellStyle(style);
    }

    private double round1(double val) {
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private String getMonthName(int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a", Locale.ENGLISH));
    }
    @Override
    public TaskResponseDTO reviewTask(
            String id,
            String role,
            Integer rating,
            String remarks
    ) {

        Task task = taskRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task not found"
                        ));

        if ("ADMIN".equals(role)) {

            task.setAdminRating(rating);

        } else if ("SUB_ADMIN".equals(role)) {

            task.setSubAdminRating(rating);
        }

        task.setAdminRemarks(remarks);

        Task saved = taskRepository.save(task);

        return mapToDTO(saved);
    }
    private void notifyEmployee(String employeeId, String title, String body, String taskId, String type) {
        employeeRepository.findById(employeeId).ifPresent(emp -> {
            if (emp.getFcmTokens() != null && !emp.getFcmTokens().isEmpty()) {
                fcmService.sendToTokens(
                        emp.getFcmTokens(),
                        title,
                        body,
                        Map.of(
                                "taskId", taskId,
                                "type",   type
                        )
                );
            } else {
                log.warn("No FCM token for employee: {}", employeeId);
            }
        });
    }

    // ─── Team Report ──────────────────────────────────────────────────────────

    @Override
    public TeamReportDTO getTeamReport(int month, int year) {

        // 1. Fetch all tasks in the given month/year
        LocalDateTime from = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime to   = from.plusMonths(1);
        List<Task> tasks   = taskRepository.findAllByTargetDateBetween(from, to);

        // 2. Group by employeeId
        Map<String, List<Task>> byEmployee = tasks.stream()
                .collect(Collectors.groupingBy(Task::getEmployeeId));

        // 3. Build per-employee summaries
        List<TeamReportDTO.EmployeePerformanceSummary> empSummaries = byEmployee.entrySet().stream()
                .map(entry -> buildEmpSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingDouble(
                        TeamReportDTO.EmployeePerformanceSummary::getPerformanceScore).reversed())
                .collect(Collectors.toList());

        // 4. Group by departmentName for dept breakdown
        Map<String, List<Task>> byDept = tasks.stream()
                .filter(t -> t.getEmployeeId() != null)
                .collect(Collectors.groupingBy(t -> {
                    // resolve department via employee lookup (cached in employee document)
                    return employeeRepository.findById(t.getEmployeeId())
                            .map(Employee::getDepartmentName)
                            .orElse("Unknown");
                }));

        List<TeamReportDTO.DepartmentPerformanceSummary> deptSummaries = byDept.entrySet().stream()
                .map(e -> {
                    long total     = e.getValue().size();
                    long completed = e.getValue().stream()
                            .filter(t -> t.getStatus() == TaskStatus.COMPLETED
                                    || t.getStatus() == TaskStatus.LATE).count();
                    double rate    = total == 0 ? 0 : round1((double) completed / total * 100);
                    // departmentId: pick from first task's employee
                    String deptId  = e.getValue().stream()
                            .findFirst()
                            .flatMap(t -> employeeRepository.findById(t.getEmployeeId()))
                            .map(Employee::getDepartmentId)
                            .orElse("");
                    return new TeamReportDTO.DepartmentPerformanceSummary(
                            deptId, e.getKey(), total, completed, rate);
                })
                .sorted(Comparator.comparingDouble(
                        TeamReportDTO.DepartmentPerformanceSummary::getCompletionRate).reversed())
                .collect(Collectors.toList());

        // 5. Team-level aggregates
        long totalTasks     = tasks.size();
        long totalCompleted = tasks.stream().filter(t ->
                t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.LATE).count();
        long totalPending   = tasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count();
        long totalLate      = tasks.stream().filter(t -> t.getStatus() == TaskStatus.LATE).count();
        long totalOverdue   = tasks.stream().filter(t -> t.getStatus() == TaskStatus.OVERDUE).count();

        double teamCompletionRate = totalTasks == 0 ? 0
                : round1((double) totalCompleted / totalTasks * 100);

        double teamAvgAdminRating = tasks.stream()
                .filter(t -> t.getAdminRating() != null && t.getAdminRating() > 0)
                .mapToInt(Task::getAdminRating).average().orElse(0);

        int totalEmployees = byEmployee.size();

        return new TeamReportDTO(totalTasks, totalCompleted, totalPending, totalLate, totalOverdue,
                teamCompletionRate, round1(teamAvgAdminRating), totalEmployees,
                empSummaries, deptSummaries);
    }

    private TeamReportDTO.EmployeePerformanceSummary buildEmpSummary(
            String employeeId, List<Task> tasks) {

        String empName  = tasks.isEmpty() ? "" : tasks.get(0).getEmployeeName();
        String deptName = employeeRepository.findById(employeeId)
                .map(Employee::getDepartmentName).orElse("");

        long total     = tasks.size();
        long completed = tasks.stream().filter(t ->
                t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.LATE).count();
        long pending   = tasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count();
        long late      = tasks.stream().filter(t -> t.getStatus() == TaskStatus.LATE).count();
        long overdue   = tasks.stream().filter(t -> t.getStatus() == TaskStatus.OVERDUE).count();

        double completionRate = total == 0 ? 0 : round1((double) completed / total * 100);

        double avgEmpRating = tasks.stream()
                .filter(t -> t.getEmpRating() != null && t.getEmpRating() > 0)
                .mapToInt(Task::getEmpRating).average().orElse(0);

        double avgAdminRating = tasks.stream()
                .filter(t -> t.getAdminRating() != null && t.getAdminRating() > 0)
                .mapToInt(Task::getAdminRating).average().orElse(0);

        // Weighted composite: 60% completion, 40% admin rating (scaled to %)
        double performanceScore = round1(
                (completionRate * 0.60) + ((avgAdminRating / 10.0) * 100 * 0.40));

        return new TeamReportDTO.EmployeePerformanceSummary(
                employeeId, empName, deptName,
                total, completed, pending, late, overdue,
                completionRate, round1(avgEmpRating), round1(avgAdminRating), performanceScore);
    }

    @Override
    public byte[] exportTeamReportExcel(int month, int year) {
        TeamReportDTO report = getTeamReport(month, year);
        String monthName     = getMonthName(month);

        Workbook workbook = new XSSFWorkbook();

        // ── Sheet 1: Summary ─────────────────────────────────────────────────
        Sheet summary = workbook.createSheet("Summary");
        CellStyle headerStyle = boldStyle(workbook);
        CellStyle greenStyle  = colorStyle(workbook, IndexedColors.LIGHT_GREEN);
        CellStyle redStyle    = colorStyle(workbook, IndexedColors.ROSE);

        int r = 0;
        summary.createRow(r++).createCell(0).setCellValue("Team Performance Report – " + monthName + " " + year);
        summary.createRow(r++).createCell(0).setCellValue("Generated: " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
        r++;
        row(summary, r++, headerStyle, "Total Tasks", report.getTotalTasks());
        row(summary, r++, greenStyle,  "Completed",   report.getTotalCompleted());
        row(summary, r++, null,        "Pending",     report.getTotalPending());
        row(summary, r++, redStyle,    "Late",        report.getTotalLate());
        row(summary, r++, redStyle,    "Overdue",     report.getTotalOverdue());
        summary.createRow(r++).createCell(0).setCellValue("Team Completion Rate: " + report.getTeamCompletionRate() + "%");
        summary.createRow(r++).createCell(0).setCellValue("Avg Admin Rating: " + report.getTeamAvgAdminRating() + "/10");
        summary.createRow(r++).createCell(0).setCellValue("Total Employees: " + report.getTotalEmployees());
        summary.autoSizeColumn(0); summary.autoSizeColumn(1);

        // ── Sheet 2: Employee Breakdown ──────────────────────────────────────
        Sheet empSheet = workbook.createSheet("Employee Breakdown");
        String[] empCols = {"Rank","Employee","Department","Total","Completed","Pending","Late","Overdue",
                "Completion %","Emp Rating","Admin Rating","Score"};
        Row empHeader = empSheet.createRow(0);
        for (int i = 0; i < empCols.length; i++) {
            Cell c = empHeader.createCell(i); c.setCellValue(empCols[i]); c.setCellStyle(headerStyle);
        }
        int rank = 1;
        for (TeamReportDTO.EmployeePerformanceSummary e : report.getEmployees()) {
            Row row = empSheet.createRow(rank);
            row.createCell(0).setCellValue(rank++);
            row.createCell(1).setCellValue(e.getEmployeeName());
            row.createCell(2).setCellValue(e.getDepartmentName());
            row.createCell(3).setCellValue(e.getTotalTasks());
            row.createCell(4).setCellValue(e.getCompleted());
            row.createCell(5).setCellValue(e.getPending());
            row.createCell(6).setCellValue(e.getLate());
            row.createCell(7).setCellValue(e.getOverdue());
            row.createCell(8).setCellValue(e.getCompletionRate() + "%");
            row.createCell(9).setCellValue(e.getAvgEmpRating());
            row.createCell(10).setCellValue(e.getAvgAdminRating());
            row.createCell(11).setCellValue(e.getPerformanceScore());
        }
        for (int i = 0; i < empCols.length; i++) empSheet.autoSizeColumn(i);

        // ── Sheet 3: Department Breakdown ────────────────────────────────────
        Sheet deptSheet = workbook.createSheet("Department Breakdown");
        String[] deptCols = {"Department","Total Tasks","Completed","Completion %"};
        Row deptHeader = deptSheet.createRow(0);
        for (int i = 0; i < deptCols.length; i++) {
            Cell c = deptHeader.createCell(i); c.setCellValue(deptCols[i]); c.setCellStyle(headerStyle);
        }
        int dr = 1;
        for (TeamReportDTO.DepartmentPerformanceSummary d : report.getDepartments()) {
            Row row = deptSheet.createRow(dr++);
            row.createCell(0).setCellValue(d.getDepartmentName());
            row.createCell(1).setCellValue(d.getTotalTasks());
            row.createCell(2).setCellValue(d.getCompleted());
            row.createCell(3).setCellValue(d.getCompletionRate() + "%");
        }
        for (int i = 0; i < deptCols.length; i++) deptSheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try { workbook.write(out); workbook.close(); }
        catch (Exception e) { log.error("Failed to write team Excel report", e); }
        return out.toByteArray();
    }

    // ── Extra helpers ──────────────────────────────────────────────────────

    private CellStyle boldStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); s.setFont(f);
        return s;
    }

    private void row(Sheet sheet, int rowIdx, CellStyle style, String label, long value) {
        Row r = sheet.createRow(rowIdx);
        Cell c = r.createCell(0); c.setCellValue(label + ": " + value);
        if (style != null) c.setCellStyle(style);
    }
}