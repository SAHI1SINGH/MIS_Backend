package com.tadobasolutions.repository;

import com.tadobasolutions.entity.Task;
import com.tadobasolutions.entity.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskSchedulerQuery {

    private final MongoTemplate mongoTemplate;

    /**
     * Finds all PENDING tasks whose targetDate has passed (i.e., overdue).
     */
    public List<Task> findOverduePendingTasks(LocalDateTime now) {
        Query query = new Query(
                Criteria.where("status").is(TaskStatus.PENDING)
                        .and("targetDate").lt(now)
        );
        return mongoTemplate.find(query, Task.class);
    }

    /**
     * Finds PENDING tasks whose targetDate falls within [from, to].
     */
    public List<Task> findPendingTasksDueWithin(LocalDateTime from, LocalDateTime to) {
        Query query = new Query(
                Criteria.where("status").is(TaskStatus.PENDING)
                        .and("targetDate").gte(from).lte(to)
        );
        return mongoTemplate.find(query, Task.class);
    }

    /**
     * Bulk updates all overdue task statuses in a single DB call
     * instead of loading and saving each one individually.
     */
    public long bulkMarkAsOverdue(LocalDateTime now) {
        Query query = new Query(
                Criteria.where("status").is(TaskStatus.PENDING)
                        .and("targetDate").lt(now)
        );
        Update update = Update.update("status", TaskStatus.OVERDUE);
        return mongoTemplate.updateMulti(query, update, Task.class).getModifiedCount();
    }
}