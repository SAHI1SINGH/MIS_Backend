package com.tadobasolutions.entity;

import com.tadobasolutions.entity.enums.Role;
import com.tadobasolutions.entity.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "tasks")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Task {

    @Id
    private String id;

    private String task;
    private String description;
    private LocalDateTime assignedDate;
    private LocalDateTime targetDate;
    private LocalDateTime submissionDate;

    private TaskStatus status = TaskStatus.PENDING;

    // Denormalised employee reference
    private String employeeId;
    private String employeeName;

    // ── Who assigned this task (ADMIN or SUB_ADMIN employee) ─────────────────
    private String assignedById;
    private String assignedByName;
    private Role   assignedByRole;

    // ── Ratings ──────────────────────────────────────────────────────────────
    private Integer empRating;
    private Integer adminRating;
    private Integer subAdminRating;
    private String  adminRemarks;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}