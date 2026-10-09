package com.tadobasolutions.dto.request;

import lombok.Data;

@Data
public class UsageItemDTO {
    private String componentId;
    private int quantityUsed;
}