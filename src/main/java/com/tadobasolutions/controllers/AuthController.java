package com.tadobasolutions.controllers;

import com.tadobasolutions.dto.ChangePasswordDTO;
import com.tadobasolutions.dto.LoginDTO;
import com.tadobasolutions.security.JwtUtil;
import com.tadobasolutions.service.AuthService;
import com.tadobasolutions.utilities.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://tadobasolutions.com",
        "https://tadobasolutions.com",
        "http://mis.tadobasolutions.com",
        "https://mis.tadobasolutions.com"
})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil     jwtUtil;

    // ── Login ─────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginDTO>> login(@RequestBody LoginDTO dto) {
        LoginDTO loggedIn = authService.login(dto);
        return ResponseEntity.ok(
                ApiResponse.<LoginDTO>builder()
                        .data(loggedIn)
                        .message("Login successful")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────
    //
    // Flutter calls this automatically when any request returns 401.
    // Request body: { "refreshToken": "..." }
    // Response:     { "data": { "accessToken": "...", "refreshToken": "..." } }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(
            @RequestBody Map<String, String> body) {

        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.<Map<String, String>>builder()
                            .message("Refresh token is required")
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .timeStamp(LocalDateTime.now())
                            .success(false)
                            .build()
            );
        }

        // Validate refresh token
        if (!jwtUtil.isTokenValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<Map<String, String>>builder()
                            .message("Refresh token is invalid or expired. Please login again.")
                            .statusCode(HttpStatus.UNAUTHORIZED.value())
                            .timeStamp(LocalDateTime.now())
                            .success(false)
                            .build()
            );
        }

        // Extract email and generate new tokens
        String email          = jwtUtil.extractEmail(refreshToken);
        LoginDTO userDetails  = authService.getUserByEmail(email);

        String newAccessToken  = jwtUtil.generateAccessToken(
                email, userDetails.getUserType(), userDetails.getId());
        String newRefreshToken = jwtUtil.generateRefreshToken(email);

        return ResponseEntity.ok(
                ApiResponse.<Map<String, String>>builder()
                        .data(Map.of(
                                "accessToken",  newAccessToken,
                                "refreshToken", newRefreshToken
                        ))
                        .message("Token refreshed successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }

    // ── Change Password ───────────────────────────────────────────────────────

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Password changed successfully")
                        .statusCode(HttpStatus.OK.value())
                        .timeStamp(LocalDateTime.now())
                        .success(true)
                        .build()
        );
    }
}