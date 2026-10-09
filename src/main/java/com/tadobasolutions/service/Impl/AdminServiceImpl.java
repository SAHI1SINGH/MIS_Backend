package com.tadobasolutions.service.Impl;

import com.tadobasolutions.dto.request.AdminRequestDTO;
import com.tadobasolutions.dto.response.AdminResponseDTO;
import com.tadobasolutions.entity.Admin;
import com.tadobasolutions.exception.BadRequestException;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.repository.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.tadobasolutions.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    private AdminResponseDTO mapToDTO(Admin admin) {
        return new AdminResponseDTO(
                admin.getId(),
                admin.getName(),
                admin.getEmail(),
                admin.getRole(),
                admin.getCreatedAt(),
                admin.getUpdatedAt()
        );
    }

    @Override
    public AdminResponseDTO createAdmin(AdminRequestDTO dto) {
        if (adminRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new BadRequestException("Admin with this email already exists");
        }
        Admin admin = new Admin();
        admin.setName(dto.getName());
        admin.setEmail(dto.getEmail());
        admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        return mapToDTO(adminRepository.save(admin));
    }

    @Override
    public AdminResponseDTO updateAdmin(String id, AdminRequestDTO dto) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (dto.getName() != null && !dto.getName().isBlank()) admin.setName(dto.getName());
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) admin.setEmail(dto.getEmail());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) admin.setPassword(passwordEncoder.encode(dto.getPassword()));

        return mapToDTO(adminRepository.save(admin));
    }

    @Override
    public void deleteAdmin(String id) {
        if (!adminRepository.existsById(id)) {
            throw new ResourceNotFoundException("Admin not found");
        }
        adminRepository.deleteById(id);
    }

    @Override
    public AdminResponseDTO getAdminById(String id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        return mapToDTO(admin);
    }

    @Override
    public List<AdminResponseDTO> getAllAdmins() {
        return adminRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
}
