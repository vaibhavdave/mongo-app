package com.mongoapp.foundations.controller;

import com.mongoapp.foundations.exception.ProductNotFoundException;
import com.mongoapp.foundations.model.Product;
import com.mongoapp.foundations.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Thin REST layer over ProductRepository - every endpoint here maps to one
 * or two lines of Spring Data, so you can see exactly which Mongo operation
 * each HTTP call triggers (also visible in the console thanks to the
 * MongoTemplate DEBUG logging enabled in application.yml).
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody Product product) {
        product.setId(null); // ignore any client-supplied id, let Mongo generate one
        product.setCreatedAt(Instant.now());
        Product saved = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public Product findById(@PathVariable String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable String id, @Valid @RequestBody Product update) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        existing.setName(update.getName());
        existing.setDescription(update.getDescription());
        existing.setPrice(update.getPrice());
        existing.setCategory(update.getCategory());
        existing.setTags(update.getTags());
        existing.setInStock(update.isInStock());

        return productRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Demonstrates Spring Data's derived query methods - each optional
     * filter is a separate repository method, combined here at the web
     * layer. Try it with just ?category=, just ?maxPrice=, ?name=, or
     * ?inStockOnly=true.
     */
    @GetMapping("/search")
    public List<Product> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String name,
            @RequestParam(required = false, defaultValue = "false") boolean inStockOnly) {

        if (category != null) {
            return productRepository.findByCategory(category);
        }
        if (maxPrice != null) {
            return productRepository.findByPriceLessThan(maxPrice);
        }
        if (name != null) {
            return productRepository.findByNameContainingIgnoreCase(name);
        }
        if (inStockOnly) {
            return productRepository.findByInStockTrue();
        }
        return productRepository.findAll();
    }
}
