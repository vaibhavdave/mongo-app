package com.mongoapp.transactions.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "inventory")
@Data
@NoArgsConstructor
public class InventoryItem {

    @Id
    private String id;

    @NotBlank
    private String sku;

    private int stock;

    @Version
    private Long version;
}
