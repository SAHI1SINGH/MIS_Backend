package com.tadobasolutions.repository;

import com.tadobasolutions.entity.Attendance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends MongoRepository<Attendance, String> {

    List<Attendance> findByEmployeeIdOrderByDateDesc(String employeeId);


    @Query(value = "{ 'employeeId': ?0, 'date': { $gte: ?1, $lte: ?2 } }",
            sort  = "{ 'date': -1 }")
    List<Attendance> findByEmployeeIdAndDateBetweenOrderByDateDesc(
            String employeeId, LocalDate from, LocalDate to);

    @Query(value = "{ 'date': { $gte: ?0, $lte: ?1 } }",
            sort  = "{ 'date': 1, 'employeeName': 1 }")
    List<Attendance> findByDateBetweenOrderByDateAscEmployeeNameAsc(
            LocalDate from, LocalDate to);

    Optional<Attendance> findByEmployeeIdAndDate(String employeeId, LocalDate date);

    @Query(value = "{ 'departmentId': ?0, 'date': { $gte: ?1, $lte: ?2 } }",
            sort  = "{ 'date': 1 }")
    List<Attendance> findByDepartmentIdAndDateBetweenOrderByDateAsc(
            String departmentId, LocalDate from, LocalDate to);

    // Count for a specific employee in a range (inclusive)
    @Query(value = "{ 'employeeId': ?0, 'date': { $gte: ?1, $lte: ?2 } }", count = true)
    long countByEmployeeIdAndDateBetween(String employeeId, LocalDate from, LocalDate to);

    // All today's records (for admin dashboard)
    List<Attendance> findByDateOrderByMarkedAtAsc(LocalDate date);

    // Late entries today
    @Query("{ 'date': ?0, 'lateEntry': true }")
    List<Attendance> findLateEntriesOnDate(LocalDate date);
}