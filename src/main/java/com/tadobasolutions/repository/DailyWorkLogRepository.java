package com.tadobasolutions.repository;

import com.tadobasolutions.entity.DailyWorkLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyWorkLogRepository extends MongoRepository<DailyWorkLog, String> {

    // Employee: check if already submitted for today
    Optional<DailyWorkLog> findByEmployeeIdAndDate(String employeeId, LocalDate date);

    // Employee: their own history newest-first
    List<DailyWorkLog> findByEmployeeIdOrderByDateDesc(String employeeId);

    // Admin: all logs for a specific date (EOD inbox for today)
    List<DailyWorkLog> findByDateOrderByEmployeeNameAsc(LocalDate date);

    // Admin: all logs in a date range for one employee
    List<DailyWorkLog> findByEmployeeIdAndDateBetweenOrderByDateDesc(
            String employeeId, LocalDate from, LocalDate to);

    // Admin: all logs in a date range across full team
    List<DailyWorkLog> findByDateBetweenOrderByDateDescEmployeeNameAsc(
            LocalDate from, LocalDate to);

    // Admin: employees who have NOT submitted for a given date
    // (used by idle detection — fetches submitted IDs, then we diff against all employees)
    @Query(value = "{ 'date': ?0 }", fields = "{ 'employeeId': 1 }")
    List<DailyWorkLog> findEmployeeIdsWhoSubmittedOn(LocalDate date);

    // Count submissions for streak calculation
    long countByEmployeeIdAndDateBetween(String employeeId, LocalDate from, LocalDate to);
}
