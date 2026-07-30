package com.mongoapp.transactions.service;

import com.mongoapp.transactions.model.InventoryItem;
import com.mongoapp.transactions.repository.InventoryItemRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;

    public InventoryService(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    /**
     * Deterministically reproduces the race optimistic locking guards
     * against, without needing two real concurrent HTTP requests: load
     * the same document into two separate in-memory copies (as if two
     * requests had each read it before either wrote), save the first
     * (succeeds, version 0 -> 1), then try to save the second - its
     * version field is still 0, so the update's filter
     * ({"_id": ..., "version": 0}) matches nothing and Spring Data raises
     * OptimisticLockingFailureException instead of silently clobbering
     * the first write.
     */
    public String simulateConflict(String id) {
        InventoryItem readByRequestA = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No item " + id));
        InventoryItem readByRequestB = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No item " + id));

        readByRequestA.setStock(readByRequestA.getStock() - 1);
        inventoryItemRepository.save(readByRequestA);

        readByRequestB.setStock(readByRequestB.getStock() - 1);
        try {
            inventoryItemRepository.save(readByRequestB);
            return "Both writes succeeded - this shouldn't happen with @Version in place";
        } catch (OptimisticLockingFailureException ex) {
            return "Second write rejected: " + ex.getMessage();
        }
    }

    /**
     * The standard way to actually recover from a conflict instead of
     * just detecting it: re-read the latest version and retry the update
     * a bounded number of times.
     */
    public InventoryItem reserveWithRetry(String id, int quantity, int maxAttempts) {
        OptimisticLockingFailureException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                InventoryItem item = inventoryItemRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("No item " + id));
                item.setStock(item.getStock() - quantity);
                return inventoryItemRepository.save(item);
            } catch (OptimisticLockingFailureException ex) {
                lastFailure = ex;
            }
        }
        throw lastFailure;
    }
}
