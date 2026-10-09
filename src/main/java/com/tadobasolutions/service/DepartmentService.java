package com.tadobasolutions.service;

import com.tadobasolutions.dto.DepartmentDTO;

import java.util.List;

public interface DepartmentService {
    DepartmentDTO createDepartment(String name);
    DepartmentDTO updateDepartment(String id, String name, String inchargeId);
    void deleteDepartment(String id);
    DepartmentDTO getDepartmentById(String id);
    List<DepartmentDTO> getAllDepartments();
}
