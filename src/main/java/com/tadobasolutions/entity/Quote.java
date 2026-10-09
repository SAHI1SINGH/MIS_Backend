package com.tadobasolutions.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "quotes")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Quote {

    @Id
    private String id;

    @NotBlank(message = "customer name should not be blank.")
    private String customerName;

    @NotBlank(message = "customer email should not be blank.")
    @Email(message = "Email Invalid format.")
    private String customerEmail;

    @NotBlank(message = "customer mobile number should not be blank.")
    @Pattern(regexp = "[0-9]{10}$", message = "Phone number must be 10 digits.")
    private String customerMobileNumber;

    @NotBlank(message = "Description should not be blank.")
    private String QueryDescription;
}
