package com.mongoapp.aggregation.dto;

import com.mongoapp.aggregation.model.OrderItem;

import java.time.Instant;
import java.util.List;

public record OrderWithCustomer(String orderId, String customerName, String customerEmail,
                                 String status, Instant createdAt, List<OrderItem> items) {
}
