package com.tadobasolutions.dto;

import com.tadobasolutions.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordDTO {
    private String id;    // MongoDB String id
    private Role userType;
    private String oldPassword;
    private String newPassword;
}
