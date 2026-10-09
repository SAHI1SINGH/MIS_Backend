package com.tadobasolutions.entity;

import com.tadobasolutions.entity.enums.Role;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Document(collection = "interns")
public class Intern {
   private String id;
   private String name;
   @Indexed(unique =true)
   private String email;
   private String password;
   private String departmentId;
   private String departmentName;
   private Role role =Role.INTERN;
   private LocalDate joiningDate;
   private LocalDate createdAt;
   private LocalDate updatedAt;
   private List<String> fcmTokens =new ArrayList<>();

}
