package com.tadobasolutions.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection ="store_component_usege")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class StoreComponentUsage {
    @Id
    private String id;
    private String componentId;
    private String componentName;
    private String unit;
    private int quantityUsed;
    private String purpose;
    private String departmentId;
    private String departmentName;
    private String usedByEmployeeId;
    private String usedByName;
    private String batchId;
    private LocalDateTime usedAt;
}
