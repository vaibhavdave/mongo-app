package com.mongoapp.changestreams.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "products")
@Data
@NoArgsConstructor
public class Product {

    @Id
    private String id;

    @NotBlank
    private String name;

    private double price;

    private int stock;

    private Instant updatedAt;
}
