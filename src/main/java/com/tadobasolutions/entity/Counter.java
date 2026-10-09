package com.tadobasolutions.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "counters")
@AllArgsConstructor
@NoArgsConstructor
public class Counter {

    @Id
    private String id;

    private long visitCount;
    private LocalDateTime lastUpdated;
}
