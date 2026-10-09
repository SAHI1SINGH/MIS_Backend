package com.tadobasolutions.entity;

import com.tadobasolutions.entity.enums.CertificateStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "certificates")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Certificate {

    @Id
    private String id;

    @Indexed(unique = true)
    private String referenceId;

    private LocalDate dob;
    private String fileName;
    private String filePath;

    private CertificateStatus status = CertificateStatus.ACTIVE;
    private String uploadedBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
