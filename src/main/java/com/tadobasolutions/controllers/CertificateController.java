package com.tadobasolutions.controllers;

import com.tadobasolutions.dto.response.VerifyResponse;
import com.tadobasolutions.exception.CertificateNotFoundException;
import com.tadobasolutions.service.CertificateService;
import com.tadobasolutions.utilities.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://tadobasolutions.com",
        "https://tadobasolutions.com",
        "http://mis.tadobasolutions.com",
        "https://mis.tadobasolutions.com"
})
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    /**
     * Verify certificate by reference ID and DOB
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<VerifyResponse>> verifyCertificate(@RequestBody Map<String, String> body) {
        String referenceId = body.get("referenceId");
        String dobStr = body.get("dob");

        if (referenceId == null || dobStr == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<VerifyResponse>builder()
                            .message("Missing referenceId or dob")
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .timeStamp(LocalDateTime.now())
                            .success(false)
                            .build()
            );
        }

        try {
            LocalDate dob = LocalDate.parse(dobStr);
            VerifyResponse response = certificateService.verify(referenceId, dob);

            return ResponseEntity.ok(
                    ApiResponse.<VerifyResponse>builder()
                            .data(response)
                            .message("Certificate verified successfully")
                            .statusCode(HttpStatus.OK.value())
                            .timeStamp(LocalDateTime.now())
                            .success(true)
                            .build()
            );

        } catch (CertificateNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<VerifyResponse>builder()
                            .message(ex.getMessage())
                            .statusCode(HttpStatus.NOT_FOUND.value())
                            .timeStamp(LocalDateTime.now())
                            .success(false)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<VerifyResponse>builder()
                            .message("Server error: " + e.getMessage())
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .timeStamp(LocalDateTime.now())
                            .success(false)
                            .build()
            );
        }
    }

    /**
     * Preview certificate (inline PDF display)
     */
    @GetMapping("/preview/{referenceId}")
    public ResponseEntity<Resource> previewCertificate(@PathVariable String referenceId) throws IOException {
        Resource resource = certificateService.loadAsResource(referenceId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(resource.getFilename()).build());

        // Allow embedding in iframe from your frontend dev server
        headers.set("Content-Security-Policy", "frame-ancestors 'self' http://localhost:4200");
        // Clear X-Frame-Options (some security layers may set it)
        headers.set("X-Frame-Options", "");

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }




    /**
     * Download certificate as file
     */
    @GetMapping("/download/{referenceId}")
    public ResponseEntity<Resource> downloadCertificate(@PathVariable String referenceId) throws IOException {
        Resource resource = certificateService.loadAsResource(referenceId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename(resource.getFilename()).build());

        headers.set("X-Frame-Options", "");

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }
}