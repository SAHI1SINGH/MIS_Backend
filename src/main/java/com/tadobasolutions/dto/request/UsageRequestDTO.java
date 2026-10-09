package com.tadobasolutions.dto.request;

import lombok.Data;

@Data
public class UsageRequestDTO {
    private String componentId;
    private int quantityUsed;
    private String purpose;
}
