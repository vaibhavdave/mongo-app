package com.mongoapp.indexing.repository;

import com.mongoapp.indexing.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
}
