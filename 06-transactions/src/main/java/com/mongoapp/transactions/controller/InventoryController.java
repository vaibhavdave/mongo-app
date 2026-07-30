package com.mongoapp.transactions.controller;

import com.mongoapp.transactions.model.InventoryItem;
import com.mongoapp.transactions.repository.InventoryItemRepository;
import com.mongoapp.transactions.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryService inventoryService;

    public InventoryController(InventoryItemRepository inventoryItemRepository, InventoryService inventoryService) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<InventoryItem> create(@Valid @RequestBody InventoryItem item) {
        item.setId(null);
        item.setVersion(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryItemRepository.save(item));
    }

    @GetMapping
    public List<InventoryItem> findAll() {
        return inventoryItemRepository.findAll();
    }

    @PostMapping("/{id}/simulate-conflict")
    public Map<String, String> simulateConflict(@PathVariable String id) {
        return Map.of("result", inventoryService.simulateConflict(id));
    }

    @PostMapping("/{id}/reserve-with-retry")
    public InventoryItem reserveWithRetry(@PathVariable String id, @RequestParam int quantity,
                                           @RequestParam(defaultValue = "3") int maxAttempts) {
        return inventoryService.reserveWithRetry(id, quantity, maxAttempts);
    }
}
