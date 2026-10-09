package com.tadobasolutions.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaveReviewRequestDTO {
    // id of the Employee (Level 1) or Admin (Level 2) performing the review
    private String approverId;
    private boolean approved;
    private String remarks;
}
