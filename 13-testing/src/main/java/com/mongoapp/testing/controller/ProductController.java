package com.mongoapp.testing.controller;

import com.mongoapp.testing.model.Product;
import com.mongoapp.testing.repository.ProductRepository;
import com.mongoapp.testing.service.ProductSearchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The class under test in ProductControllerWebMvcTest - see that test
 * for the "fast, no real database" testing tier.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductSearchService productSearchService;

    public ProductController(ProductRepository productRepository, ProductSearchService productSearchService) {
        this.productRepository = productRepository;
        this.productSearchService = productSearchService;
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody Product product) {
        product.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(productRepository.save(product));
    }

    @GetMapping
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @GetMapping("/in-stock")
    public List<Product> findInStock(@RequestParam String category) {
        return productSearchService.findInStockByCategorySortedByPrice(category);
    }
}
