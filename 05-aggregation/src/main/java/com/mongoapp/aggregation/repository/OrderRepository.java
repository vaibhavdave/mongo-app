package com.mongoapp.aggregation.repository;

import com.mongoapp.aggregation.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<Order, String> {
}
