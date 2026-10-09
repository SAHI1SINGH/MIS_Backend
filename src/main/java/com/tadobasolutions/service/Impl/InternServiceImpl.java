package com.tadobasolutions.service.Impl;

import com.tadobasolutions.dto.InternDTO;
import com.tadobasolutions.entity.Department;
import com.tadobasolutions.entity.Intern;
import com.tadobasolutions.entity.enums.Role;
import com.tadobasolutions.exception.BadRequestException;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.repository.DepartmentRepository;
import com.tadobasolutions.repository.InternRepository;
import com.tadobasolutions.service.InternService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InternServiceImpl implements InternService {

    private final InternRepository internRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public InternDTO createIntern(InternDTO dto) {
        if (internRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new BadRequestException("An Intern with this email already exists");
        }

        Intern intern = new Intern();
        intern.setName(dto.getName());
        intern.setEmail(dto.getEmail());
        intern.setPassword(passwordEncoder.encode(dto.getPassword()));
        intern.setJoiningDate(dto.getJoiningDate());
        intern.setRole(Role.INTERN);

        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            intern.setDepartmentId(department.getId());
            intern.setDepartmentName(department.getName());
        }

        Intern saved = internRepository.save(intern);
        return toDTO(saved);
    }

    @Override
    public List<InternDTO> getAllInterns() {
        return internRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    public InternDTO getInternById(String internId) {
        Intern intern = internRepository.findById(internId)
                .orElseThrow(() -> new ResourceNotFoundException("Intern Not Found"));
        return toDTO(intern);
    }

    @Override
    public InternDTO getInternByEmail(String email) {
        Intern intern = internRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Intern Not Found"));
        return toDTO(intern);
    }

    @Override
    public InternDTO updateIntern(String id, InternDTO dto) {
        Intern intern = internRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intern Not Found"));

        if (dto.getName() != null) intern.setName(dto.getName());
        if (dto.getEmail() != null) intern.setEmail(dto.getEmail());
        if (dto.getPassword() != null && !dto.getPassword().isBlank())
            intern.setPassword(passwordEncoder.encode(dto.getPassword()));
        if (dto.getJoiningDate() != null) intern.setJoiningDate(dto.getJoiningDate());

        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            intern.setDepartmentId(department.getId());
            intern.setDepartmentName(department.getName());
        }

        Intern saved = internRepository.save(intern);
        return toDTO(saved);
    }

    @Override
    public void deleteIntern(String id) {
        if (!internRepository.existsById(id)) {
            throw new ResourceNotFoundException("Intern Not Found");
        }
        internRepository.deleteById(id);
    }

    private InternDTO toDTO(Intern intern) {
        return InternDTO.builder()
                .id(intern.getId())
                .name(intern.getName())
                .email(intern.getEmail())
                .departmentName(intern.getDepartmentName())
                .departmentId(intern.getDepartmentId())
                .joiningDate(intern.getJoiningDate())
                .role(intern.getRole())
                .createdAt(intern.getCreatedAt())
                .updatedAt(intern.getUpdatedAt())
                .build();
    }
}