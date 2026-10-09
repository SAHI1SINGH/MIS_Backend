package com.tadobasolutions.service;

import com.tadobasolutions.dto.request.AdminRequestDTO;
import com.tadobasolutions.dto.response.AdminResponseDTO;

import java.util.List;

public interface AdminService {
    AdminResponseDTO createAdmin(AdminRequestDTO dto);
    AdminResponseDTO updateAdmin(String id, AdminRequestDTO dto);
    void deleteAdmin(String id);
    AdminResponseDTO getAdminById(String id);
    List<AdminResponseDTO> getAllAdmins();
}
