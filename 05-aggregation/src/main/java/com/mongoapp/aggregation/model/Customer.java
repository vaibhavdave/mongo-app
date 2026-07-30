package com.mongoapp.aggregation.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "customers")
@Data
@NoArgsConstructor
public class Customer {

    @Id
    private String id;

    @NotBlank
    private String name;

    private String email;
}
