package com.mongoapp.capstone.repository;

import com.mongoapp.capstone.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerRepository extends MongoRepository<Customer, String> {
}
