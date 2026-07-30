package com.mongoapp.testing.repository;

import com.mongoapp.testing.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByCategory(String category);

    List<Product> findByPriceLessThan(double maxPrice);
}
