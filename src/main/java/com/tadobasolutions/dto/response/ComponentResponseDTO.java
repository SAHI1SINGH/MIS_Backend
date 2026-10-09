package com.tadobasolutions.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentResponseDTO {
    private String id;
    private String name;
    private String unit;

    private String description;
    private String brand;
    private String modelNumber;
    private String category;
    private Map<String, String> specifications;
    private Double unitPrice;
    private String supplier;
    private String warranty;
    private String imageUrl;

    private int totalQuantity;
    private int usedQuantity;
    private int availableQuantity;

    private String departmentId;
    private String departmentName;
}