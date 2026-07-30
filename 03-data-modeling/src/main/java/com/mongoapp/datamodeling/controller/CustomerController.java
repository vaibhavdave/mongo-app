package com.mongoapp.datamodeling.controller;

import com.mongoapp.datamodeling.exception.NotFoundException;
import com.mongoapp.datamodeling.model.Customer;
import com.mongoapp.datamodeling.repository.CustomerRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @PostMapping
    public ResponseEntity<Customer> create(@Valid @RequestBody Customer customer) {
        customer.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(customerRepository.save(customer));
    }

    @GetMapping
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @GetMapping("/{id}")
    public Customer findById(@PathVariable String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No customer with id " + id));
    }
}
