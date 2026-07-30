package com.mongoapp.capstone.repository;

import com.mongoapp.capstone.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
}
