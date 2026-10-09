package com.tadobasolutions.service;

import com.tadobasolutions.dto.request.EmployeeRequestDTO;
import com.tadobasolutions.dto.response.EmployeeResponseDTO;
import com.tadobasolutions.entity.enums.Role;

import java.util.List;

public interface EmployeeService {
    EmployeeResponseDTO createEmployee(EmployeeRequestDTO dto);
    EmployeeResponseDTO updateEmployee(String id, EmployeeRequestDTO dto);
    void deleteEmployee(String id);
    EmployeeResponseDTO getEmployeeById(String id);
    List<EmployeeResponseDTO> getAllEmployees();

    /** Promotes or demotes an employee by changing their role. */
    EmployeeResponseDTO changeRole(String id, Role newRole);

    /** Returns all employees currently holding the SUB_ADMIN role. */
    List<EmployeeResponseDTO> getAllSubAdmins();
}