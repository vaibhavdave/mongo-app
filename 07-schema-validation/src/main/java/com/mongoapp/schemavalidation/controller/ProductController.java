package com.mongoapp.schemavalidation.controller;

import com.mongoapp.schemavalidation.model.Product;
import com.mongoapp.schemavalidation.repository.ProductRepository;
import jakarta.validation.Valid;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    public ProductController(ProductRepository productRepository, MongoTemplate mongoTemplate) {
        this.productRepository = productRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody Product product) {
        product.setId(null);
        product.setCreatedAt(Instant.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(productRepository.save(product));
    }

    @GetMapping
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    /**
     * Deliberately skips the repository (and therefore Bean Validation
     * entirely) and inserts a raw, intentionally-invalid BSON document
     * straight through the driver - missing "category", which the
     * collection's $jsonSchema validator requires. Even bypassing every
     * layer of application code, MongoDB itself still rejects the write.
     * That's the point: the database-level constraint is the one no
     * client, however careless or malicious, can route around.
     */
    @PostMapping("/raw-insert-invalid")
    public ResponseEntity<?> rawInsertInvalid() {
        Document invalid = new Document()
                .append("sku", "sku-bypassing-validation")
                .append("name", "This document is missing 'category' and 'price'");
        mongoTemplate.getCollection("products").insertOne(invalid);
        // if we get here, the insert was NOT rejected - shouldn't happen
        return ResponseEntity.ok("Insert unexpectedly succeeded");
    }
}
