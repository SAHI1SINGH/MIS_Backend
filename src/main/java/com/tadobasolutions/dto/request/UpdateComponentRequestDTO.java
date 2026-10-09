package com.tadobasolutions.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class UpdateComponentRequestDTO {
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
}