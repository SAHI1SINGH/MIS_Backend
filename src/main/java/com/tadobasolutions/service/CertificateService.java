package com.tadobasolutions.service;

import com.tadobasolutions.dto.response.VerifyResponse;
import com.tadobasolutions.entity.Certificate;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

public interface CertificateService {

    /**
     * Upload and save a certificate PDF file for a referenceId + dob.
     * Returns saved Certificate entity.
     */
    Certificate saveCertificate(MultipartFile file,
                                String referenceId,
                                LocalDate dob,
                                String uploadedBy) throws IOException;

    /**
     * Verify referenceId + dob and return a DTO containing preview/download URLs.
     * Should throw an exception if not found or revoked.
     */
    VerifyResponse verify(String referenceId, LocalDate dob);

    /**
     * Load the certificate file as a Resource for streaming/preview/download.
     * Throws when not found or revoked.
     */
    Resource loadAsResource(String referenceId) throws IOException;

    /**
     * Find certificate by reference id (optional).
     */
    Optional<Certificate> findByReferenceId(String referenceId);

    /**
     * Revoke (or update status) of a certificate.
     */
    Certificate revokeCertificate(String referenceId);

}
