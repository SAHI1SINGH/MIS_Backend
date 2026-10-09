package com.tadobasolutions.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "departments")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Department {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    // Store incharge as embedded reference
    private String inchargeId;
    private String inchargeName;
}
