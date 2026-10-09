package com.tadobasolutions.controllers;

import com.tadobasolutions.entity.Admin;
import com.tadobasolutions.entity.Employee;
import com.tadobasolutions.entity.Intern;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.repository.AdminRepository;
import com.tadobasolutions.repository.EmployeeRepository;
import com.tadobasolutions.repository.InternRepository;
import com.tadobasolutions.utilities.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmController {
    private final EmployeeRepository employeeRepository;
    private final InternRepository   internRepository;
    private final AdminRepository    adminRepository;
    @PutMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerEmployeeToken(@RequestBody Map<String, Object> body) {
        String employeeId = body.get("employeeId").toString();
        String fcmToken   = body.get("fcmToken").toString();

        var empOpt = employeeRepository.findById(employeeId);
        if (empOpt.isPresent()) {
            Employee emp = empOpt.get();
            if (emp.getFcmTokens() == null) emp.setFcmTokens(new ArrayList<>());
            if (!emp.getFcmTokens().contains(fcmToken)) {
                emp.getFcmTokens().add(fcmToken);
                employeeRepository.save(emp);
            }
        } else {
            Intern intern = internRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee/Intern not found: " + employeeId));
            if (intern.getFcmTokens() == null) intern.setFcmTokens(new ArrayList<>());
            if (!intern.getFcmTokens().contains(fcmToken)) {
                intern.getFcmTokens().add(fcmToken);
                internRepository.save(intern);
            }
        }

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("FCM token registered successfully")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build());
    }
    @DeleteMapping("/unregister")
    public ResponseEntity<ApiResponse<Void>> unregisterEmployeeToken(@RequestBody Map<String, Object> body) {
        String employeeId = body.get("employeeId").toString();
        String fcmToken   = body.get("fcmToken").toString();
        var empOpt = employeeRepository.findById(employeeId);
        if (empOpt.isPresent()) {
            Employee emp = empOpt.get();
            if (emp.getFcmTokens() != null) {
                emp.getFcmTokens().remove(fcmToken);
                employeeRepository.save(emp);
            }
        } else {
            Intern intern = internRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee/Intern not found: " + employeeId));
            if (intern.getFcmTokens() != null) {
                intern.getFcmTokens().remove(fcmToken);
                internRepository.save(intern);
            }
        }
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("FCM token removed successfully")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build());
    }
    @PutMapping("/register/admin")
    public ResponseEntity<ApiResponse<Void>> registerAdminToken(@RequestBody Map<String, Object> body) {
        String adminId  = body.get("adminId").toString();
        String fcmToken = body.get("fcmToken").toString();

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found: " + adminId));

        if (admin.getFcmTokens() == null) admin.setFcmTokens(new ArrayList<>());
        if (!admin.getFcmTokens().contains(fcmToken)) {
            admin.getFcmTokens().add(fcmToken);
            adminRepository.save(admin);
        }

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Admin FCM token registered successfully")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build());
    }
    @DeleteMapping("/unregister/admin")
    public ResponseEntity<ApiResponse<Void>> unregisterAdminToken(@RequestBody Map<String, Object> body) {
        String adminId  = body.get("adminId").toString();
        String fcmToken = body.get("fcmToken").toString();

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found: " + adminId));
        if (admin.getFcmTokens() != null) {
            admin.getFcmTokens().remove(fcmToken);
            adminRepository.save(admin);
        }
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Admin FCM token removed successfully")
                .statusCode(HttpStatus.OK.value())
                .timeStamp(LocalDateTime.now())
                .success(true)
                .build());
    }
}