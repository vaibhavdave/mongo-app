package com.mongoapp.datamodeling.repository;

import com.mongoapp.datamodeling.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerRepository extends MongoRepository<Customer, String> {
}
