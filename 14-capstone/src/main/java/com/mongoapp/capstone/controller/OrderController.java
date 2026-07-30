package com.mongoapp.capstone.controller;

import com.mongoapp.capstone.exception.NotFoundException;
import com.mongoapp.capstone.model.Order;
import com.mongoapp.capstone.model.OrderPlaceRequest;
import com.mongoapp.capstone.repository.OrderRepository;
import com.mongoapp.capstone.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public OrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @PostMapping
    public ResponseEntity<Order> placeOrder(@Valid @RequestBody OrderPlaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request));
    }

    @GetMapping
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    public Order findById(@PathVariable String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No order with id " + id));
    }

    @GetMapping("/by-customer/{customerId}")
    public List<Order> findByCustomer(@PathVariable String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }
}
