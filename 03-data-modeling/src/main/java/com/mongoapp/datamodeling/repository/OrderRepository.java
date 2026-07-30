package com.mongoapp.datamodeling.repository;

import com.mongoapp.datamodeling.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {

    /**
     * Filters on the manual-reference field: a single, ordinary query -
     * no extra round trips, unlike resolving a @DBRef.
     */
    List<Order> findByCustomerId(String customerId);
}
