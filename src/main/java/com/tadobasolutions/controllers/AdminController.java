package com.tadobasolutions.controllers;

import com.tadobasolutions.dto.request.AdminRequestDTO;
import com.tadobasolutions.dto.response.AdminResponseDTO;
import com.tadobasolutions.entity.Certificate;
import com.tadobasolutions.service.AdminService;
import com.tadobasolutions.service.CertificateService;
import com.tadobasolutions.utilities.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admins")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://tadobasolutions.com",
        "https://tadobasolutions.com",
        "http://mis.tadobasolutions.com",
        "https://mis.tadobasolutions.com"
})
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final CertificateService certificateService;


    @PostMapping
    public ResponseEntity<ApiResponse<AdminResponseDTO>> create(@RequestBody AdminRequestDTO dto) {
        AdminResponseDTO created = adminService.createAdmin(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<AdminResponseDTO>builder()
                        .data(created)
                        .message("Admin created successfully")
                        .statusCode(HttpStatus.CREATED.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminResponseDTO>> update(@PathVariable String id,
                                                                @RequestBody AdminRequestDTO dto) {
        AdminResponseDTO updated = adminService.updateAdmin(id, dto);
        return ResponseEntity.ok(
                ApiResponse.<AdminResponseDTO>builder()
                        .data(updated)
                        .message("Admin updated successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        adminService.deleteAdmin(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .data(null)
                        .message("Admin deleted successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminResponseDTO>> getById(@PathVariable String id) {
        AdminResponseDTO admin = adminService.getAdminById(id);
        return ResponseEntity.ok(
                ApiResponse.<AdminResponseDTO>builder()
                        .data(admin)
                        .message("Admin fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminResponseDTO>>> getAll() {
        List<AdminResponseDTO> admins = adminService.getAllAdmins();
        return ResponseEntity.ok(
                ApiResponse.<List<AdminResponseDTO>>builder()
                        .data(admins)
                        .message("Admins fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }


    /**
     * Upload a new certificate PDF file
     */
    @PostMapping("/certificates/upload")
    public ResponseEntity<ApiResponse<Certificate>> uploadCertificate(
            @RequestParam("referenceId") String referenceId,
            @RequestParam("dob") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadedBy", required = false, defaultValue = "admin") String uploadedBy
    ) throws IOException {

        Certificate saved = certificateService.saveCertificate(file, referenceId, dob, uploadedBy);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<Certificate>builder()
                        .data(saved)
                        .message("Certificate uploaded successfully")
                        .statusCode(HttpStatus.CREATED.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    /**
     * Revoke a certificate (mark as invalid)
     */
    @PutMapping("/certificates/revoke/{referenceId}")
    public ResponseEntity<ApiResponse<Certificate>> revokeCertificate(@PathVariable String referenceId) {
        Certificate revoked = certificateService.revokeCertificate(referenceId);

        return ResponseEntity.ok(
                ApiResponse.<Certificate>builder()
                        .data(revoked)
                        .message("Certificate revoked successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }
}