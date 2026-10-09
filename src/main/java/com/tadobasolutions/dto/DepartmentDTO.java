package com.tadobasolutions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentDTO {
    private String id;
    private String name;
    private String inchargeId;
    private String inchargeName;
}
