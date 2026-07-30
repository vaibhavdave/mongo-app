package com.mongoapp.foundations.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Maps to a document in the "products" collection.
 *
 * MongoDB is schema-less: every Product document can, in principle, have
 * different fields. This class is *our* application-level contract on top
 * of that flexibility - Spring Data reads/writes it to/from BSON.
 */
@Document(collection = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    /**
     * Maps to MongoDB's "_id" field. If left null on insert, Spring Data
     * lets the driver generate an ObjectId and populates this field with
     * its 24-char hex string form.
     */
    @Id
    private String id;

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    @PositiveOrZero(message = "price must be zero or positive")
    private double price;

    @Indexed
    private String category;

    private List<String> tags;

    private boolean inStock;

    private Instant createdAt;
}
