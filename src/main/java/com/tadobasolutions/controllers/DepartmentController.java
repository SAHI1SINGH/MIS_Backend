package com.tadobasolutions.controllers;

import com.tadobasolutions.dto.DepartmentDTO;
import com.tadobasolutions.service.DepartmentService;
import com.tadobasolutions.utilities.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://tadobasolutions.com",
        "https://tadobasolutions.com",
        "http://mis.tadobasolutions.com",
        "https://mis.tadobasolutions.com"
})
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentDTO>> create(@RequestParam String name) {
        DepartmentDTO department = departmentService.createDepartment(name);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<DepartmentDTO>builder()
                        .data(department)
                        .message("Department created successfully")
                        .statusCode(HttpStatus.CREATED.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDTO>> update(@PathVariable String id, @RequestParam String name, @RequestParam(required = false) String inchargeId) {
        DepartmentDTO department = departmentService.updateDepartment(id, name, inchargeId);
        return ResponseEntity.ok(
                ApiResponse.<DepartmentDTO>builder()
                        .data(department)
                        .message("Department updated successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .data(null)
                        .message("Department deleted successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDTO>> getById(@PathVariable String id) {
        DepartmentDTO department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(
                ApiResponse.<DepartmentDTO>builder()
                        .data(department)
                        .message("Department fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getAll() {
        List<DepartmentDTO> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(
                ApiResponse.<List<DepartmentDTO>>builder()
                        .data(departments)
                        .message("Departments fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }
}