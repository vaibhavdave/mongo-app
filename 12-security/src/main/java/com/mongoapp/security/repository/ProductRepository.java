package com.mongoapp.security.repository;

import com.mongoapp.security.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
}
