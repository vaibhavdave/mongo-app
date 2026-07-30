package com.mongoapp.indexing.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Every index used in this module is declared right here, next to the
 * field it applies to (or at the class level for compound indexes), and
 * created automatically at startup because application.yml turns on
 * spring.data.mongodb.auto-index-creation.
 */
@Document(collection = "products")
@CompoundIndexes({
        // Supports queries that filter by category and sort by price -
        // see ProductIndexService.explainCategoryPriceQuery.
        @CompoundIndex(name = "category_price_idx", def = "{'category': 1, 'price': -1}")
})
@Data
@NoArgsConstructor
public class Product {

    @Id
    private String id;

    /** Unique single-field index - inserting a duplicate sku fails fast. */
    @Indexed(unique = true)
    private String sku;

    @NotBlank
    @TextIndexed(weight = 2) // matches in name count more than matches in description
    private String name;

    @TextIndexed
    private String description;

    private double price;

    private String category;

    private List<String> tags;

    private Instant createdAt;
}
