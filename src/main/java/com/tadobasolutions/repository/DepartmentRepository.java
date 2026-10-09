package com.tadobasolutions.repository;

import com.tadobasolutions.entity.Department;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DepartmentRepository extends MongoRepository<Department, String> {
    Optional<Department> findByName(String name);
    Optional<Department> findByInchargeId(String inchargeId);
}
