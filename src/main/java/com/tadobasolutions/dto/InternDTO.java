package com.tadobasolutions.dto;


import com.tadobasolutions.entity.enums.Role;
import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InternDTO {
    private String id;
    private String name;
    private String email;
    private String password;
    private String departmentId;
    private String departmentName;
    private LocalDate joiningDate;
    private Role role;
    private LocalDate createdAt;
    private LocalDate updatedAt;

}
