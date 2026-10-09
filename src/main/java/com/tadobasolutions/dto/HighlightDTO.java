package com.tadobasolutions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HighlightDTO {
    private String id;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
