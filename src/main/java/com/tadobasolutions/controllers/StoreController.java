package com.tadobasolutions.controllers;

import com.tadobasolutions.dto.request.AddComponentRequestDTO;
import com.tadobasolutions.dto.request.AddStockRequestDTO;
import com.tadobasolutions.dto.request.BatchUsageRequestDTO;
import com.tadobasolutions.dto.request.UpdateComponentRequestDTO;
import com.tadobasolutions.dto.request.UsageRequestDTO;
import com.tadobasolutions.dto.response.ComponentResponseDTO;
import com.tadobasolutions.dto.response.UsageResponseDTO;
import com.tadobasolutions.entity.Admin;
import com.tadobasolutions.entity.Employee;
import com.tadobasolutions.entity.enums.Role;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.repository.AdminRepository;
import com.tadobasolutions.repository.EmployeeRepository;
import com.tadobasolutions.service.StoreService;
import com.tadobasolutions.utilities.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/store")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://tadobasolutions.com",
        "https://tadobasolutions.com",
        "http://mis.tadobasolutions.com",
        "https://mis.tadobasolutions.com"
})
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final EmployeeRepository employeeRepository;
    private final AdminRepository adminRepository;


    private Employee currentEmployee() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        boolean isAdminUser = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdminUser) {
            Admin admin = adminRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

            Employee virtualAdminEmployee = new Employee();
            virtualAdminEmployee.setId(admin.getId());
            virtualAdminEmployee.setName(admin.getName());
            virtualAdminEmployee.setEmail(admin.getEmail());
            virtualAdminEmployee.setRole(Role.ADMIN);
            return virtualAdminEmployee;
        }

        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    @PostMapping("/components")
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> addComponent(@RequestBody AddComponentRequestDTO dto) {
        Employee employee = currentEmployee();
        ComponentResponseDTO created = storeService.addComponent(dto, employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ComponentResponseDTO>builder()
                        .data(created)
                        .message("Component added successfully")
                        .statusCode(HttpStatus.CREATED.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/components/{id}")
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> getComponent(@PathVariable String id) {
        Employee employee = currentEmployee();
        ComponentResponseDTO component = storeService.getComponentById(id, employee);
        return ResponseEntity.ok(
                ApiResponse.<ComponentResponseDTO>builder()
                        .data(component)
                        .message("Component fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @PutMapping("/components/{id}")
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> updateComponent(
            @PathVariable String id, @RequestBody UpdateComponentRequestDTO dto) {
        Employee employee = currentEmployee();
        ComponentResponseDTO updated = storeService.updateComponent(id, dto, employee);
        return ResponseEntity.ok(
                ApiResponse.<ComponentResponseDTO>builder()
                        .data(updated)
                        .message("Component updated successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @PostMapping(value = "/components/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> uploadComponentImage(
            @PathVariable String id, @RequestParam("image") MultipartFile image) {
        Employee employee = currentEmployee();
        ComponentResponseDTO updated = storeService.uploadComponentImage(id, image, employee);
        return ResponseEntity.ok(
                ApiResponse.<ComponentResponseDTO>builder()
                        .data(updated)
                        .message("Component image uploaded successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @DeleteMapping("/components/{id}/image")
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> deleteComponentImage(@PathVariable String id) {
        Employee employee = currentEmployee();
        ComponentResponseDTO updated = storeService.deleteComponentImage(id, employee);
        return ResponseEntity.ok(
                ApiResponse.<ComponentResponseDTO>builder()
                        .data(updated)
                        .message("Component image removed successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @PutMapping("/components/{id}/add-stock")
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> addStock(
            @PathVariable String id, @RequestBody AddStockRequestDTO dto) {
        Employee employee = currentEmployee();
        ComponentResponseDTO updated = storeService.addStock(id, dto, employee);
        return ResponseEntity.ok(
                ApiResponse.<ComponentResponseDTO>builder()
                        .data(updated)
                        .message("Stock updated successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @PostMapping("/usage")
    public ResponseEntity<ApiResponse<UsageResponseDTO>> recordUsage(@RequestBody UsageRequestDTO dto) {
        Employee employee = currentEmployee();
        UsageResponseDTO usage = storeService.recordUsage(dto, employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<UsageResponseDTO>builder()
                        .data(usage)
                        .message("Usage recorded successfully")
                        .statusCode(HttpStatus.CREATED.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/usage/history")
    public ResponseEntity<ApiResponse<List<UsageResponseDTO>>> getUsageHistory(
            @RequestParam(required = false) String department) {
        Employee employee = currentEmployee();
        List<UsageResponseDTO> history = storeService.getUsageHistory(employee, department);
        return ResponseEntity.ok(
                ApiResponse.<List<UsageResponseDTO>>builder()
                        .data(history)
                        .message("Usage history fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/usage/export")
    public ResponseEntity<InputStreamResource> exportUsage(@RequestParam(required = false) String department) {
        Employee employee = currentEmployee();
        ByteArrayInputStream stream = storeService.exportUsageExcel(employee, department);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=component_usage_report.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/components")
    public ResponseEntity<ApiResponse<List<ComponentResponseDTO>>> getComponents(
            @RequestParam(required = false) String departmentId) {
        Employee employee = currentEmployee();
        List<ComponentResponseDTO> components = storeService.getComponents(employee, departmentId);
        return ResponseEntity.ok(
                ApiResponse.<List<ComponentResponseDTO>>builder()
                        .data(components)
                        .message("Components fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @PostMapping("/usage/batch")
    public ResponseEntity<ApiResponse<List<UsageResponseDTO>>> recordBatchUsage(@RequestBody BatchUsageRequestDTO dto) {
        Employee employee = currentEmployee();
        List<UsageResponseDTO> results = storeService.recordBatchUsage(dto, employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<List<UsageResponseDTO>>builder()
                        .data(results)
                        .message("Usage recorded successfully")
                        .statusCode(HttpStatus.CREATED.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }
}