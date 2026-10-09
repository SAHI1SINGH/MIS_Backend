package com.tadobasolutions.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsageResponseDTO {
    private String id;
    private String componentName;
    private String unit;
    private int quantityUsed;
    private String batchId;
    private String purpose;
    private String departmentName;
    private String usedByName;
    private LocalDateTime usedAt;

}
