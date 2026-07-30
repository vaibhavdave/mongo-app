package com.mongoapp.datamodeling.controller;

import com.mongoapp.datamodeling.exception.NotFoundException;
import com.mongoapp.datamodeling.model.Customer;
import com.mongoapp.datamodeling.model.Order;
import com.mongoapp.datamodeling.model.OrderCreateRequest;
import com.mongoapp.datamodeling.repository.CustomerRepository;
import com.mongoapp.datamodeling.repository.OrderRepository;
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
    private final CustomerRepository customerRepository;

    public OrderController(OrderRepository orderRepository, CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    @PostMapping
    public ResponseEntity<Order> create(@Valid @RequestBody OrderCreateRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("No customer with id " + request.getCustomerId()));

        Order order = new Order();
        order.setCustomerId(customer.getId());
        order.setCustomerRef(customer); // populated purely so GET /api/orders can demonstrate DBRef resolution
        order.setItems(request.getItems());
        order.setPayment(request.getPayment());
        order.setStatus("PLACED");
        order.setCreatedAt(Instant.now());

        return ResponseEntity.status(HttpStatus.CREATED).body(orderRepository.save(order));
    }

    /**
     * Loading every order here eagerly resolves customerRef for each one
     * (one extra query per order, visible in the MongoTemplate DEBUG log)
     * because @DBRef defaults to eager. That N+1 pattern is the main
     * reason to prefer manual references for anything beyond a handful of
     * documents.
     */
    @GetMapping
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    public Order findById(@PathVariable String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No order with id " + id));
    }

    /** Manual-reference lookup: single filtered query, no DBRef involved. */
    @GetMapping("/by-customer/{customerId}")
    public List<Order> findByCustomer(@PathVariable String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    /**
     * Resolves the customer explicitly via the manual reference
     * (customerId) - one clearly-visible query, triggered only when this
     * endpoint is actually called. Contrast with customerRef on the order
     * itself, which is already resolved (or not, if lazy) by the time you
     * get the Order object back.
     */
    @GetMapping("/{id}/customer")
    public Customer getCustomerViaManualReference(@PathVariable String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No order with id " + id));
        return customerRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new NotFoundException("No customer with id " + order.getCustomerId()));
    }
}
