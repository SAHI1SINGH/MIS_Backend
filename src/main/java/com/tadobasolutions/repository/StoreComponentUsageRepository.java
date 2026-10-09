package com.tadobasolutions.repository;

import com.tadobasolutions.entity.StoreComponentUsage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StoreComponentUsageRepository extends MongoRepository<StoreComponentUsage, String> {
    List<StoreComponentUsage> findByDepartmentNameOrderByUsedAtDesc(String departmentName);
    List<StoreComponentUsage> findByDepartmentIdOrderByUsedAtDesc(String departmentId);
    List<StoreComponentUsage> findAllByOrderByUsedAtDesc();
}