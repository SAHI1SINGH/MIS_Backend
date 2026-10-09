package com.tadobasolutions.repository;

import com.tadobasolutions.entity.StoreComponent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StoreComponentRepository extends MongoRepository<StoreComponent, String> {
    Optional<StoreComponent> findByDepartmentIdAndNameIgnoreCase(String departmentId, String name);
    List<StoreComponent> findByDepartmentId(String departmentId);
}