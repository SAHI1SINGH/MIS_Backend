package com.tadobasolutions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskStatusCountDTO {
    private long total;
    private long completed;
    private long pending;
    private long late;
    private long overdue;
}