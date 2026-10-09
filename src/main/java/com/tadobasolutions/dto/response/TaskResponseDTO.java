package com.tadobasolutions.dto.response;

import com.tadobasolutions.entity.enums.Role;
import com.tadobasolutions.entity.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskResponseDTO {
    private String id;
    private String task;
    private String description;
    private LocalDateTime targetDate;
    private LocalDateTime submissionDate;
    private TaskStatus status;
    private String employeeId;
    private String employeeName;
    private LocalDateTime assignedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    private String assignedById;
    private String assignedByName;
    private Role   assignedByRole;


    private Integer empRating;
    private Integer adminRating;
    private Integer subAdminRating;
    private String  adminRemarks;
}