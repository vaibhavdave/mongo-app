package com.mongoapp.capstone.controller;

import com.mongoapp.capstone.model.Product;
import com.mongoapp.capstone.repository.ProductRepository;
import com.mongoapp.capstone.service.ProductQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductQueryService productQueryService;

    public ProductController(ProductRepository productRepository, ProductQueryService productQueryService) {
        this.productRepository = productRepository;
        this.productQueryService = productQueryService;
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

    @GetMapping("/search")
    public List<Product> search(@RequestParam(required = false) String category,
                                 @RequestParam(required = false) Double maxPrice,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return productQueryService.search(category, maxPrice, page, size);
    }

    @GetMapping("/search/text")
    public List<Product> textSearch(@RequestParam String q) {
        return productQueryService.textSearch(q);
    }
}
