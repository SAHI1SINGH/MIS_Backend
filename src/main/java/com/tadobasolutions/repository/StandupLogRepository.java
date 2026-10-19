package com.tadobasolutions.repository;

import com.tadobasolutions.entity.StandupLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StandupLogRepository extends MongoRepository<StandupLog, String> {
    Optional<StandupLog> findFirstBySubmittedByIdAndStandupDateAndSubmittedByAdminFalse(
            String submittedById, LocalDate standupDate);
    List<StandupLog> findBySubmittedByIdAndSubmittedByAdminFalseOrderByStandupDateDesc(String submittedById);
    Optional<StandupLog> findByDepartmentIdAndStandupDateAndSubmittedById(
            String departmentId, LocalDate standupDate, String submittedById);
    List<StandupLog> findByStandupDateOrderByDepartmentNameAsc(LocalDate standupDate);
    List<StandupLog> findByStandupDateAndDepartmentIdOrderByDepartmentNameAsc(
            LocalDate standupDate, String departmentId);
    List<StandupLog> findByStandupDateBetweenOrderByStandupDateAscDepartmentNameAsc(
            LocalDate from, LocalDate to);
    List<StandupLog> findByDraftTrueAndStandupDateLessThanEqual(LocalDate date);
}