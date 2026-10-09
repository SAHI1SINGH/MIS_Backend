package com.tadobasolutions.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class BatchUsageRequestDTO {
    private String purpose;
    private List<UsageItemDTO> items;
}