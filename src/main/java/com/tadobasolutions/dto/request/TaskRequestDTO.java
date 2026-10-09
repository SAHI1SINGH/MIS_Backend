package com.tadobasolutions.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskRequestDTO {
    private String task;
    private String description;
    private LocalDateTime targetDate;
    private LocalDateTime submissionDate;
    private String employeeId;


    private String assignedById;

    private Integer empRating;
    private Integer adminRating;
    private Integer subAdminRating;
    private String  adminRemarks;
}