package com.mongoapp.schemavalidation.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Bean Validation here (@NotBlank etc.) only runs when a request comes
 * through this app's REST controller. It cannot stop a bad document
 * written directly via Mongo Express, a script, or another service - only
 * the $jsonSchema validator attached to the "products" collection itself
 * (see ProductCollectionInitializer) can do that, because MongoDB
 * enforces it for every write regardless of client. Also note: the
 * database schema below additionally restricts "category" to a fixed
 * enum - something this Java-level validation doesn't even attempt,
 * showing the two layers aren't redundant, they cover different things.
 */
@Document(collection = "products")
@Data
@NoArgsConstructor
public class Product {

    @Id
    private String id;

    @NotBlank
    private String sku;

    @NotBlank
    private String name;

    @NotNull
    @Valid
    private Money price;

    private String category;

    private Instant createdAt;
}
