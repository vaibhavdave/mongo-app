package com.mongoapp.querying.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    private String id;

    @NotBlank
    private String name;

    private String description;

    @PositiveOrZero
    private double price;

    private String category;

    private List<String> tags;

    @PositiveOrZero
    private int stock;

    private boolean inStock;

    private Instant createdAt;
}
