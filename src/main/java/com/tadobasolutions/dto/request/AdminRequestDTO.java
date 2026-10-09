package com.tadobasolutions.dto.request;

import com.tadobasolutions.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminRequestDTO {
    private String name;
    private String email;
    private String password;
}