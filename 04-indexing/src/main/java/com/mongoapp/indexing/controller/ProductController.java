package com.mongoapp.indexing.controller;

import com.mongoapp.indexing.model.Product;
import com.mongoapp.indexing.repository.ProductRepository;
import com.mongoapp.indexing.service.ProductIndexService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductIndexService indexService;

    public ProductController(ProductRepository productRepository, ProductIndexService indexService) {
        this.productRepository = productRepository;
        this.indexService = indexService;
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

    @GetMapping("/explain/indexed")
    public Map<String, Object> explainIndexed(@RequestParam String category) {
        return indexService.explainCategoryPriceQuery(category);
    }

    @GetMapping("/explain/unindexed")
    public Map<String, Object> explainUnindexed(@RequestParam String snippet) {
        return indexService.explainUnindexedDescriptionSearch(snippet);
    }

    @GetMapping("/search/text")
    public List<Product> textSearch(@RequestParam String q) {
        return indexService.textSearch(q);
    }
}
