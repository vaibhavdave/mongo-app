package com.mongoapp.indexing.controller;

import com.mongoapp.indexing.model.CartSession;
import com.mongoapp.indexing.repository.CartSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/cart-sessions")
public class CartSessionController {

    private final CartSessionRepository cartSessionRepository;

    public CartSessionController(CartSessionRepository cartSessionRepository) {
        this.cartSessionRepository = cartSessionRepository;
    }

    @PostMapping
    public ResponseEntity<CartSession> create(@RequestParam String owner,
                                               @RequestParam(defaultValue = "120") long ttlSeconds) {
        CartSession session = new CartSession();
        session.setOwner(owner);
        session.setProductIds(List.of());
        session.setCreatedAt(Instant.now());
        session.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        return ResponseEntity.status(HttpStatus.CREATED).body(cartSessionRepository.save(session));
    }

    @GetMapping
    public List<CartSession> findAll() {
        return cartSessionRepository.findAll();
    }
}
