package com.mongoapp.aggregation.controller;

import com.mongoapp.aggregation.model.Order;
import com.mongoapp.aggregation.model.OrderCreateRequest;
import com.mongoapp.aggregation.repository.OrderRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping
    public ResponseEntity<Order> create(@Valid @RequestBody OrderCreateRequest request) {
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setItems(request.getItems());
        order.setStatus("PLACED");
        order.setCreatedAt(Instant.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderRepository.save(order));
    }

    @GetMapping
    public List<Order> findAll() {
        return orderRepository.findAll();
    }
}
