package com.tadobasolutions.repository;

import com.tadobasolutions.entity.Certificate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CertificateRepository extends MongoRepository<Certificate, String> {
    Optional<Certificate> findByReferenceId(String referenceId);
    Optional<Certificate> findByReferenceIdAndDob(String referenceId, LocalDate dob);
}
