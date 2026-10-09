package com.tadobasolutions.repository;

import com.tadobasolutions.entity.Leave;
import com.tadobasolutions.entity.enums.LeaveStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRepository extends MongoRepository<Leave, String> {

    List<Leave> findByEmployeeId(String employeeId);

    List<Leave> findByStatus(LeaveStatus status);

    List<Leave> findByEmployeeIdAndStatus(String employeeId, LeaveStatus status);

    // Filter combo used by the admin leave list screen
    @Query("{ $and: [ " +
            "  { $or: [ { 'status': ?0 }, { $expr: { $eq: [?0, null] } } ] }, " +
            "  { $or: [ { 'employeeId': ?1 }, { $expr: { $eq: [?1, null] } } ] } " +
            "] }")
    List<Leave> findLeavesByFilters(LeaveStatus status, String employeeId);

    // Employees currently on leave: APPROVED and today falls within [fromDate, toDate]
    List<Leave> findByStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            LeaveStatus status, LocalDate fromDateBefore, LocalDate toDateAfter);

    // Review history — leaves this admin has already acted on
    List<Leave> findByApproverIdAndActionAtIsNotNullOrderByActionAtDesc(String approverId);
}