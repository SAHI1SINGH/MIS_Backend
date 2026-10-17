package com.tadobasolutions.repository;

import com.tadobasolutions.entity.StandupLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StandupLogRepository extends MongoRepository<StandupLog, String> {

    // one report per department per day — duplicate check + "today" lookup
    Optional<StandupLog> findByDepartmentIdAndStandupDate(String departmentId, LocalDate standupDate);

    // incharge: own department history, newest first
    List<StandupLog> findByDepartmentIdOrderByStandupDateDesc(String departmentId);

    // admin: all departments on a date
    List<StandupLog> findByStandupDateOrderByDepartmentNameAsc(LocalDate standupDate);

    // admin: one department on a date
    List<StandupLog> findByStandupDateAndDepartmentIdOrderByDepartmentNameAsc(
            LocalDate standupDate, String departmentId);

    // admin: all departments in a date range — used for the Excel export
    List<StandupLog> findByStandupDateBetweenOrderByStandupDateAscDepartmentNameAsc(
            LocalDate from, LocalDate to);
}