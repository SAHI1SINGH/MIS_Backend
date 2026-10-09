package com.tadobasolutions.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tadobasolutions.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginDTO {

    // ─── Request fields ───────────────────────────────────────────────────────
    private Role userType;
    private String email;
    private String password;
    private String fcmToken;

    // ─── Response fields ──────────────────────────────────────────────────────
    private String id;    // MongoDB uses String ObjectId
    private String name;
    private String accessToken;
    private String refreshToken;
}
