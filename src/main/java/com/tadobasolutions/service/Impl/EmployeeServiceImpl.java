package com.tadobasolutions.service.Impl;

import com.tadobasolutions.dto.request.EmployeeRequestDTO;
import com.tadobasolutions.dto.response.EmployeeResponseDTO;
import com.tadobasolutions.entity.Department;
import com.tadobasolutions.entity.Employee;
import com.tadobasolutions.entity.enums.Role;
import com.tadobasolutions.exception.BadRequestException;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.repository.DepartmentRepository;
import com.tadobasolutions.repository.EmployeeRepository;
import com.tadobasolutions.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    private EmployeeResponseDTO mapToDTO(Employee emp) {
        return new EmployeeResponseDTO(
                emp.getId(),
                emp.getName(),
                emp.getEmail(),
                emp.getDepartmentId(),
                emp.getDepartmentName(),
                emp.getJoiningDate(),
                emp.getDob(),
                emp.getResponsibilities(),
                emp.getRole(),
                emp.getCreatedAt(),
                emp.getUpdatedAt()
        );
    }

    @Override
    public EmployeeResponseDTO createEmployee(EmployeeRequestDTO dto) {
        if (employeeRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new BadRequestException("Employee with this email already exists");
        }
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        Employee emp = new Employee();
        emp.setName(dto.getName());
        emp.setEmail(dto.getEmail());
        emp.setJoiningDate(dto.getJoiningDate());
        emp.setPassword("Tadoba@123");
        emp.setDepartmentId(department.getId());
        emp.setDepartmentName(department.getName());
        emp.setDob(dto.getDob());
        emp.setResponsibilities(dto.getResponsibilities());
        // role defaults to EMPLOYEE inside the entity

        return mapToDTO(employeeRepository.save(emp));
    }

    @Override
    public EmployeeResponseDTO updateEmployee(String id, EmployeeRequestDTO dto) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (dto.getName() != null && !dto.getName().isBlank()) emp.setName(dto.getName());
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) emp.setEmail(dto.getEmail());
        if (dto.getJoiningDate() != null) emp.setJoiningDate(dto.getJoiningDate());
        if (dto.getDob() != null) emp.setDob(dto.getDob());
        if (dto.getResponsibilities() != null) emp.setResponsibilities(dto.getResponsibilities());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) emp.setPassword(dto.getPassword());

        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            emp.setDepartmentId(department.getId());
            emp.setDepartmentName(department.getName());
        }

        return mapToDTO(employeeRepository.save(emp));
    }

    @Override
    public void deleteEmployee(String id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        departmentRepository.findByInchargeId(id).ifPresent(dept -> {
            dept.setInchargeId(null);
            dept.setInchargeName(null);
            departmentRepository.save(dept);
        });
        employeeRepository.delete(employee);
    }

    @Override
    public EmployeeResponseDTO getEmployeeById(String id) {
        return mapToDTO(employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found")));
    }

    @Override
    public List<EmployeeResponseDTO> getAllEmployees() {
        return employeeRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ── Sub-admin methods ─────────────────────────────────────────────────────

    @Override
    public EmployeeResponseDTO changeRole(String id, Role newRole) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // Prevent changing an ADMIN accidentally through this endpoint
        if (emp.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot change role of an ADMIN through this endpoint");
        }
        emp.setRole(newRole);
        return mapToDTO(employeeRepository.save(emp));
    }

    @Override
    public List<EmployeeResponseDTO> getAllSubAdmins() {
        return employeeRepository.findByRole(Role.SUB_ADMIN)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }
}
