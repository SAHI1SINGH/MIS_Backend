package com.tadobasolutions.service.Impl;

import com.tadobasolutions.dto.ChangePasswordDTO;
import com.tadobasolutions.dto.LoginDTO;
import com.tadobasolutions.entity.Admin;
import com.tadobasolutions.entity.Employee;
import com.tadobasolutions.entity.Intern;
import com.tadobasolutions.entity.enums.Role;
import com.tadobasolutions.exception.BadRequestException;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.repository.AdminRepository;
import com.tadobasolutions.repository.EmployeeRepository;
import com.tadobasolutions.repository.InternRepository;
import com.tadobasolutions.security.JwtUtil;
import com.tadobasolutions.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AdminRepository    adminRepository;
    private final EmployeeRepository employeeRepository;
    private final InternRepository   internRepository;
    private final JwtUtil            jwtUtil;
    private final PasswordEncoder    passwordEncoder;

    @Override
    public LoginDTO login(LoginDTO dto) {
        if (dto.getUserType() == null) throw new BadRequestException("User type is required");

        switch (dto.getUserType()) {
            case ADMIN -> {
                Admin admin = adminRepository.findByEmail(dto.getEmail())
                        .orElseThrow(() -> new ResourceNotFoundException("Incorrect email"));
                if (!matchPassword(dto.getPassword(), admin.getPassword()))
                    throw new BadRequestException("Incorrect password");

                if (dto.getFcmToken() != null && !dto.getFcmToken().isBlank()) {
                    if (admin.getFcmTokens() == null) admin.setFcmTokens(new ArrayList<>());
                    if (!admin.getFcmTokens().contains(dto.getFcmToken()))
                        admin.getFcmTokens().add(dto.getFcmToken());
                    adminRepository.save(admin);
                }

                String accessToken  = jwtUtil.generateAccessToken(admin.getEmail(), Role.ADMIN, admin.getId());
                String refreshToken = jwtUtil.generateRefreshToken(admin.getEmail());

                return LoginDTO.builder()
                        .userType(Role.ADMIN)
                        .email(admin.getEmail())
                        .id(admin.getId())
                        .name(admin.getName())
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
            }

            case EMPLOYEE, SUB_ADMIN -> {
                Employee emp = employeeRepository.findByEmail(dto.getEmail())
                        .orElseThrow(() -> new ResourceNotFoundException("Incorrect email"));
                if (!matchPassword(dto.getPassword(), emp.getPassword()))
                    throw new BadRequestException("Incorrect password");

                if (dto.getFcmToken() != null && !dto.getFcmToken().isBlank()) {
                    if (emp.getFcmTokens() == null) emp.setFcmTokens(new ArrayList<>());
                    if (!emp.getFcmTokens().contains(dto.getFcmToken()))
                        emp.getFcmTokens().add(dto.getFcmToken());
                    employeeRepository.save(emp);
                }

                Role   actualRole   = emp.getRole();
                String accessToken  = jwtUtil.generateAccessToken(emp.getEmail(), actualRole, emp.getId());
                String refreshToken = jwtUtil.generateRefreshToken(emp.getEmail());

                return LoginDTO.builder()
                        .userType(actualRole)
                        .email(emp.getEmail())
                        .id(emp.getId())
                        .name(emp.getName())
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
            }

            case INTERN -> {
                Intern intern = internRepository.findByEmail(dto.getEmail())
                        .orElseThrow(() -> new ResourceNotFoundException("Incorrect email"));
                if (!matchPassword(dto.getPassword(), intern.getPassword()))
                    throw new BadRequestException("Incorrect password");

                if (dto.getFcmToken() != null && !dto.getFcmToken().isBlank()) {
                    if (intern.getFcmTokens() == null) intern.setFcmTokens(new ArrayList<>());
                    if (!intern.getFcmTokens().contains(dto.getFcmToken()))
                        intern.getFcmTokens().add(dto.getFcmToken());
                    internRepository.save(intern);
                }

                String accessToken  = jwtUtil.generateAccessToken(intern.getEmail(), Role.INTERN, intern.getId());
                String refreshToken = jwtUtil.generateRefreshToken(intern.getEmail());

                return LoginDTO.builder()
                        .userType(Role.INTERN)
                        .email(intern.getEmail())
                        .id(intern.getId())
                        .name(intern.getName())
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
            }

            default -> throw new BadRequestException("Unsupported user type");
        }
    }

    // ── Used by refresh-token endpoint to get user details from email ─────────

    @Override
    public LoginDTO getUserByEmail(String email) {
        // Try admin first
        var adminOpt = adminRepository.findByEmail(email);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            return LoginDTO.builder()
                    .userType(Role.ADMIN)
                    .email(admin.getEmail())
                    .id(admin.getId())
                    .name(admin.getName())
                    .build();
        }

        // Try employee
        var empOpt = employeeRepository.findByEmail(email);
        if (empOpt.isPresent()) {
            Employee emp = empOpt.get();
            return LoginDTO.builder()
                    .userType(emp.getRole())
                    .email(emp.getEmail())
                    .id(emp.getId())
                    .name(emp.getName())
                    .build();
        }

        // Try intern
        Intern intern = internRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));

        return LoginDTO.builder()
                .userType(Role.INTERN)
                .email(intern.getEmail())
                .id(intern.getId())
                .name(intern.getName())
                .build();
    }

    // ── Change password ───────────────────────────────────────────────────────

    @Override
    public void changePassword(ChangePasswordDTO dto) {
        if (dto.getUserType() == null) throw new BadRequestException("User type is required");

        switch (dto.getUserType()) {
            case ADMIN -> {
                Admin admin = adminRepository.findById(dto.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                if (!matchPassword(dto.getOldPassword(), admin.getPassword()))
                    throw new BadRequestException("Incorrect old password");
                admin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
                adminRepository.save(admin);
            }
            case EMPLOYEE, SUB_ADMIN -> {
                Employee emp = employeeRepository.findById(dto.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                if (!matchPassword(dto.getOldPassword(), emp.getPassword()))
                    throw new BadRequestException("Incorrect old password");
                emp.setPassword(passwordEncoder.encode(dto.getNewPassword()));
                employeeRepository.save(emp);
            }
            case INTERN -> {
                Intern intern = internRepository.findById(dto.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                if (!matchPassword(dto.getOldPassword(), intern.getPassword()))
                    throw new BadRequestException("Incorrect old password");
                intern.setPassword(passwordEncoder.encode(dto.getNewPassword()));
                internRepository.save(intern);
            }
            default -> throw new BadRequestException("Unsupported user type");
        }
    }

    private boolean matchPassword(String rawInput, String storedPassword) {
        if (storedPassword != null && storedPassword.startsWith("$2")) {
            return passwordEncoder.matches(rawInput, storedPassword);
        }
        return rawInput.equals(storedPassword);
    }
}