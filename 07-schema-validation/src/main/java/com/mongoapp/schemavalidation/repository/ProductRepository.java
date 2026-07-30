package com.mongoapp.schemavalidation.repository;

import com.mongoapp.schemavalidation.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
}
