package com.mongoapp.changestreams.controller;

import com.mongoapp.changestreams.model.Product;
import com.mongoapp.changestreams.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Product> create(@Valid @RequestBody Product product) {
        product.setId(null);
        product.setUpdatedAt(Instant.now());
        return productRepository.save(product);
    }

    @GetMapping
    public Flux<Product> findAll() {
        return productRepository.findAll();
    }

    @PutMapping("/{id}")
    public Mono<Product> update(@PathVariable String id, @Valid @RequestBody Product update) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No product " + id)))
                .flatMap(existing -> {
                    existing.setName(update.getName());
                    existing.setPrice(update.getPrice());
                    existing.setStock(update.getStock());
                    existing.setUpdatedAt(Instant.now());
                    return productRepository.save(existing);
                });
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable String id) {
        return productRepository.deleteById(id);
    }
}
