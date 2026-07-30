package com.mongoapp.aggregation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "orders")
@Data
@NoArgsConstructor
public class Order {

    @Id
    private String id;

    /** Manual reference (see module 03 for why) - stored as a plain string, not ObjectId. */
    private String customerId;

    private List<OrderItem> items;

    private String status;

    private Instant createdAt;
}
