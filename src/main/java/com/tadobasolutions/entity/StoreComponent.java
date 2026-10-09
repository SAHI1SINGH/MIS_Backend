package com.tadobasolutions.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "store_components")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoreComponent {

    @Id
    private String id;

    private String name;
    private String unit;

    // ── Product details ──
    private String description;
    private String brand;
    private String modelNumber;
    private String category;
    private Map<String, String> specifications;
    private Double unitPrice;
    private String supplier;
    private String warranty;

    // ── Image ──
    private String imageUrl;
    private String imagePublicId;

    private int totalQuantity;
    private int usedQuantity;
    private int availableQuantity;

    private String departmentId;
    private String departmentName;

    private String createdByEmployeeId;
    private String createdByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}