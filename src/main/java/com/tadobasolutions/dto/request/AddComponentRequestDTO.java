package com.tadobasolutions.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class AddComponentRequestDTO {
    private String name;
    private String unit;
    private int initialQuantity;

    // Optional product details
    private String description;
    private String brand;
    private String modelNumber;
    private String category;
    private Map<String, String> specifications;
    private Double unitPrice;
    private String supplier;
    private String warranty;
}