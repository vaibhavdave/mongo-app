package com.mongoapp.aggregation.repository;

import com.mongoapp.aggregation.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerRepository extends MongoRepository<Customer, String> {
}
