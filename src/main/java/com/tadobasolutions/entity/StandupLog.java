package com.tadobasolutions.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Document(collection = "standup_logs")
public class StandupLog {

    @Id
    private String id;

    // which employee submitted this
    @Indexed
    private String employeeId;
    private String employeeName;

    // type of work done: "Site Visit", "Meeting", "Office Work" etc.
    private String workType;

    // what was done in this entry
    private String didYesterday;   // kept for backward compat (morning plan)
    private String doingToday;     // main description of work done
    private String blockers;       // "None" or actual blocker text

    // auto-set: which entry of the day this is (1st, 2nd, 3rd...)
    private Integer entryNumber;

    // date of standup (just the date, not time)
    @Indexed
    private LocalDate standupDate;

    // exact submit timestamp
    private LocalDateTime submittedAt;

    // "SUBMITTED" or "LATE" (submitted after 10 AM)
    private String status;
}