package com.tadobasolutions.service.Impl;

import com.tadobasolutions.dto.response.VerifyResponse;
import com.tadobasolutions.entity.Certificate;
import com.tadobasolutions.entity.enums.CertificateStatus;
import com.tadobasolutions.exception.CertificateNotFoundException;
import com.tadobasolutions.repository.CertificateRepository;
import com.tadobasolutions.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;

    @Value("${cert.storage.path}")
    private String storagePath;

    // optional, used to build full preview/download URLs. Controller can also build these instead.
    @Value("${cert.preview.url-base:}")
    private String baseUrl;

    @Override
    public Certificate saveCertificate(MultipartFile file, String referenceId, LocalDate dob, String uploadedBy) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        if (!originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF allowed");
        }

        // create storage folder if missing
        Path folder = Paths.get(storagePath);
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        // create a safe file name
        String storedFileName = referenceId + "_" + System.currentTimeMillis() + ".pdf";
        Path target = folder.resolve(storedFileName);

        // copy file
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IOException("Failed to store file", e);
        }

        // upsert certificate record
        Optional<Certificate> existing = certificateRepository.findByReferenceId(referenceId);
        Certificate cert;
        if (existing.isPresent()) {
            cert = existing.get();
            cert.setDob(dob);
            cert.setFileName(originalFilename);
            cert.setFilePath(target.toAbsolutePath().toString());
            cert.setUploadedBy(uploadedBy);
            cert.setStatus(CertificateStatus.ACTIVE);
        } else {
            cert = new Certificate();
            cert.setReferenceId(referenceId);
            cert.setDob(dob);
            cert.setFileName(originalFilename);
            cert.setFilePath(target.toAbsolutePath().toString());
            cert.setUploadedBy(uploadedBy);
            cert.setStatus(CertificateStatus.ACTIVE);
        }
        return certificateRepository.save(cert);
    }

    @Override
    
    public VerifyResponse verify(String referenceId, LocalDate dob) {
        Certificate cert = certificateRepository
                .findByReferenceIdAndDob(referenceId, dob)
                .orElseThrow(() -> new CertificateNotFoundException("Certificate not found or DOB mismatch"));

        if (cert.getStatus() == CertificateStatus.REVOKED) {
            throw new CertificateNotFoundException("Certificate is revoked");
        }

        // Build URLs (controller could build them instead); keep consistent with previous design.
        // controller mappings live under /api/certificates so return matching paths
        String previewUrl = buildUrl("/api/certificates/preview/", cert.getReferenceId());
        String downloadUrl = buildUrl("/api/certificates/download/", cert.getReferenceId());

        return new VerifyResponse(previewUrl, downloadUrl, cert.getFileName());
    }

    private String buildUrl(String path, String referenceId) {
        if (baseUrl == null || baseUrl.isBlank()) {
            // fallback to relative path
            return path + referenceId;
        }
        // ensure no double slashes
        String trimmedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String trimmedPath = path.startsWith("/") ? path : "/" + path;
        return trimmedBase + trimmedPath + referenceId;
    }

    @Override
    
    public Resource loadAsResource(String referenceId) throws IOException {
        Certificate cert = certificateRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new CertificateNotFoundException("Certificate not found"));

        if (cert.getStatus() == CertificateStatus.REVOKED) {
            throw new CertificateNotFoundException("Certificate is revoked");
        }

        Path file = Paths.get(cert.getFilePath());
        if (!Files.exists(file) || !Files.isReadable(file)) {
            throw new CertificateNotFoundException("Certificate file not found on disk");
        }

        return new PathResource(file);
    }

    @Override
    
    public Optional<Certificate> findByReferenceId(String referenceId) {
        return certificateRepository.findByReferenceId(referenceId);
    }

    @Override
    public Certificate revokeCertificate(String referenceId) {
        Certificate cert = certificateRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new CertificateNotFoundException("Certificate not found"));
        cert.setStatus(CertificateStatus.REVOKED);
        return certificateRepository.save(cert);
    }
}
