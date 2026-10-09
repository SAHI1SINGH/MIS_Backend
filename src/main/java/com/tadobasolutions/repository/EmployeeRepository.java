package com.tadobasolutions.repository;

import com.tadobasolutions.entity.Employee;
import com.tadobasolutions.entity.enums.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends MongoRepository<Employee, String> {
    Optional<Employee> findByEmail(String email);
    List<Employee> findByRole(Role role);
    Optional<Employee> findByNameIgnoreCase(String name);
}
