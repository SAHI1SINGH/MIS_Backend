package com.tadobasolutions.controllers;

import com.tadobasolutions.dto.request.EmployeeRequestDTO;
import com.tadobasolutions.dto.response.EmployeeResponseDTO;
import com.tadobasolutions.entity.Employee;
import com.tadobasolutions.entity.enums.Role;
import com.tadobasolutions.service.EmployeeService;
import com.tadobasolutions.utilities.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://tadobasolutions.com",
        "https://tadobasolutions.com",
        "http://mis.tadobasolutions.com",
        "https://mis.tadobasolutions.com"
})
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> create(@RequestBody EmployeeRequestDTO dto) {
        EmployeeResponseDTO created = employeeService.createEmployee(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<EmployeeResponseDTO>builder()
                        .data(created).message("Employee created successfully")
                        .statusCode(HttpStatus.CREATED.value()).timeStamp(LocalDateTime.now()).success(true).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> update(@PathVariable String id,
                                                                   @RequestBody EmployeeRequestDTO dto) {
        EmployeeResponseDTO updated = employeeService.updateEmployee(id, dto);
        return ResponseEntity.ok(
                ApiResponse.<EmployeeResponseDTO>builder()
                        .data(updated).message("Employee updated successfully")
                        .statusCode(HttpStatus.OK.value()).timeStamp(LocalDateTime.now()).success(true).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .data(null).message("Employee deleted successfully")
                        .statusCode(HttpStatus.OK.value()).timeStamp(LocalDateTime.now()).success(true).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> getById(@PathVariable String id) {
        EmployeeResponseDTO emp = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(
                ApiResponse.<EmployeeResponseDTO>builder()
                        .data(emp).message("Employee fetched successfully")
                        .statusCode(HttpStatus.OK.value()).timeStamp(LocalDateTime.now()).success(true).build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getAll() {
        List<EmployeeResponseDTO> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(
                ApiResponse.<List<EmployeeResponseDTO>>builder()
                        .data(employees).message("Employees fetched successfully")
                        .statusCode(HttpStatus.OK.value()).timeStamp(LocalDateTime.now()).success(true).build());
    }

    // ── Sub-admin management ──────────────────────────────────────────────────

    /**
     * GET /api/employees/sub-admins
     * Returns all employees who currently hold the SUB_ADMIN role.
     */
    @GetMapping("/sub-admins")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getAllSubAdmins() {
        List<EmployeeResponseDTO> subAdmins = employeeService.getAllSubAdmins();
        return ResponseEntity.ok(
                ApiResponse.<List<EmployeeResponseDTO>>builder()
                        .data(subAdmins).message("Sub-admins fetched successfully")
                        .statusCode(HttpStatus.OK.value()).timeStamp(LocalDateTime.now()).success(true).build());
    }

    /**
     * PUT /api/employees/{id}/promote-sub-admin
     * Promotes an employee to SUB_ADMIN role.
     * Only ADMIN can call this (enforced by SecurityConfig).
     */
    @PutMapping("/{id}/promote-sub-admin")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> promoteToSubAdmin(@PathVariable String id) {
        EmployeeResponseDTO updated = employeeService.changeRole(id, Role.SUB_ADMIN);
        return ResponseEntity.ok(
                ApiResponse.<EmployeeResponseDTO>builder()
                        .data(updated).message("Employee promoted to Sub-Admin")
                        .statusCode(HttpStatus.OK.value()).timeStamp(LocalDateTime.now()).success(true).build());
    }

    /**
     * PUT /api/employees/{id}/demote-employee
     * Reverts a sub-admin back to EMPLOYEE role.
     */
    @PutMapping("/{id}/demote-employee")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> demoteToEmployee(@PathVariable String id) {
        EmployeeResponseDTO updated = employeeService.changeRole(id, Role.EMPLOYEE);
        return ResponseEntity.ok(
                ApiResponse.<EmployeeResponseDTO>builder()
                        .data(updated).message("Sub-Admin demoted to Employee")
                        .statusCode(HttpStatus.OK.value()).timeStamp(LocalDateTime.now()).success(true).build());
    }


    @GetMapping("/me")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> getMyProfile(@RequestParam String id) {
        EmployeeResponseDTO emp = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(
                ApiResponse.<EmployeeResponseDTO>builder()
                        .data(emp)
                        .message("Profile fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build());
    }

}
