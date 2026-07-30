package com.mongoapp.foundations.repository;

import com.mongoapp.foundations.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Spring Data derives the query for each method below from its name alone -
 * no implementation, no annotations needed. Under the hood these become
 * Mongo find() filters, e.g. findByCategory("electronics") becomes
 * db.products.find({ category: "electronics" }).
 *
 * MongoRepository already provides save(), findById(), findAll(),
 * deleteById(), count(), etc.
 */
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByCategory(String category);

    List<Product> findByPriceLessThan(double maxPrice);

    List<Product> findByNameContainingIgnoreCase(String namePart);

    List<Product> findByInStockTrue();
}
