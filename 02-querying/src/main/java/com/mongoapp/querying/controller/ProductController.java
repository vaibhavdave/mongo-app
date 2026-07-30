package com.mongoapp.querying.controller;

import com.mongoapp.querying.dto.PageResponse;
import com.mongoapp.querying.model.Product;
import com.mongoapp.querying.repository.ProductRepository;
import com.mongoapp.querying.service.ProductQueryService;
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
    private final ProductQueryService queryService;

    public ProductController(ProductRepository productRepository, ProductQueryService queryService) {
        this.productRepository = productRepository;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody Product product) {
        product.setId(null);
        product.setCreatedAt(Instant.now());
        product.setInStock(product.getStock() > 0);
        return ResponseEntity.status(HttpStatus.CREATED).body(productRepository.save(product));
    }

    @GetMapping
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/query")
    public PageResponse<Product> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        return queryService.search(category, minPrice, maxPrice, tag, inStock, page, size, sortBy, sortDir);
    }

    @GetMapping("/projection")
    public List<Map> nameAndPriceOnly() {
        return queryService.nameAndPriceOnly();
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> adjustStock(@PathVariable String id, @RequestParam int delta) {
        Product updated = queryService.adjustStock(id, delta);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/tags")
    public ResponseEntity<Product> addTag(@PathVariable String id, @RequestParam String tag) {
        Product updated = queryService.addTag(id, tag);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/purchase")
    public ResponseEntity<?> purchase(@PathVariable String id, @RequestParam(defaultValue = "1") int quantity) {
        Product updated = queryService.purchase(id, quantity);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Not enough stock (or product not found)"));
        }
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/bulk/out-of-stock")
    public Map<String, Object> bulkMarkOutOfStock(@RequestParam String category) {
        long modified = queryService.markCategoryOutOfStock(category);
        return Map.of("category", category, "modifiedCount", modified);
    }
}
