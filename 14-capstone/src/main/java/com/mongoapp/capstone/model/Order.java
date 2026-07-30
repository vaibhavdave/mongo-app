package com.mongoapp.capstone.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/** Manual reference to Customer (module 03) - deliberately not @DBRef, see that module's README for why. */
@Document(collection = "orders")
@Data
@NoArgsConstructor
public class Order {

    @Id
    private String id;

    private String customerId;

    private List<OrderItem> items;

    private double totalAmount;

    private String status;

    private Instant createdAt;
}
