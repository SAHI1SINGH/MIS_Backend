package com.tadobasolutions.entity;

import com.tadobasolutions.entity.enums.Role;
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
import java.util.ArrayList;
import java.util.List;

@Document(collection = "employees")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Employee {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    private String password;
    private String departmentId;
    private String departmentName;

    private LocalDate joiningDate;
    private LocalDate dob;
    private String responsibilities;

    private Role role = Role.EMPLOYEE;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private List<String> fcmTokens = new ArrayList<>();
}
