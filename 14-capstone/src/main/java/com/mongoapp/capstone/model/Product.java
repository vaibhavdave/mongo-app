package com.mongoapp.capstone.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Indexes (module 04): unique sku, compound category+price, text name+description. */
@Document(collection = "products")
@CompoundIndex(name = "category_price_idx", def = "{'category': 1, 'price': -1}")
@Data
@NoArgsConstructor
public class Product {

    @Id
    private String id;

    @Indexed(unique = true)
    @NotBlank
    private String sku;

    @NotBlank
    @TextIndexed(weight = 2)
    private String name;

    @TextIndexed
    private String description;

    private String category;

    @PositiveOrZero
    private double price;

    @PositiveOrZero
    private int stock;

    private Instant createdAt;
}
