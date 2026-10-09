package com.tadobasolutions.repository;

import com.tadobasolutions.entity.StandupLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface StandupLogRepository extends MongoRepository<StandupLog, String> {

    // all entries by one employee on a specific date (multiple allowed)
    List<StandupLog> findByEmployeeIdAndStandupDateOrderBySubmittedAtAsc(
            String employeeId, LocalDate standupDate
    );

    // all entries by one employee — full history, newest first
    List<StandupLog> findByEmployeeIdOrderByStandupDateDescSubmittedAtDesc(
            String employeeId
    );

    // all entries on a given date — admin sees everyone's work
    List<StandupLog> findByStandupDateOrderByEmployeeIdAscSubmittedAtAsc(
            LocalDate date
    );

    // count entries for an employee on a date — used for entryNumber
    long countByEmployeeIdAndStandupDate(
            String employeeId, LocalDate standupDate
    );
}