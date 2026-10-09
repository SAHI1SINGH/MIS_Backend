package com.tadobasolutions.repository;

import com.tadobasolutions.entity.Task;
import com.tadobasolutions.entity.enums.TaskStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task> findByEmployeeId(String employeeId);

    @Query("{ $and: [ " +
            "  { $or: [ { 'status': ?0 }, { $expr: { $eq: [?0, null] } } ] }, " +
            "  { $or: [ { 'employeeId': ?1 }, { $expr: { $eq: [?1, null] } } ] }, " +
            "  { $or: [ { 'targetDate': { $gte: ?2, $lt: ?3 } }, { $expr: { $eq: [?2, null] } } ] } " +
            "] }")
    List<Task> findTasksByFilters(TaskStatus status, String employeeId,
                                  LocalDateTime startDate, LocalDateTime endDate);

    @Query("{ 'status': ?0, 'targetDate': { $lt: ?1 } }")
    List<Task> findOverduePendingTasks(TaskStatus status, LocalDateTime now);

    List<Task> findByEmployeeIdAndStatusIn(String employeeId, List<TaskStatus> statuses);

    @Query("{ 'status': ?0, 'targetDate': { $gte: ?1, $lte: ?2 } }")
    List<Task> findPendingTasksDueWithin(TaskStatus status, LocalDateTime from, LocalDateTime to);

    // ── NEW: load all tasks for a month/year across the whole team ───────────
    @Query("{ 'targetDate': { $gte: ?0, $lt: ?1 } }")
    List<Task> findAllByTargetDateBetween(LocalDateTime from, LocalDateTime to);

    // ── NEW: load tasks for a specific department in a month/year ────────────
    // Tasks store departmentName as a denormalised field copied from the employee
    // at assignment time. If you need department filtering, also store deptId on Task.
    // For now we filter in-memory after fetching by date range (fast enough for SMB scale).
}