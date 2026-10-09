package com.tadobasolutions.dto.response;

import com.tadobasolutions.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeResponseDTO {
    private String id;
    private String name;
    private String email;
    private String departmentId;
    private String departmentName;
    private LocalDate joiningDate;
    private LocalDate dob;
    private String responsibilities;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
