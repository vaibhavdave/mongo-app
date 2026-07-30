package com.mongoapp.transactions.repository;

import com.mongoapp.transactions.model.InventoryItem;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventoryItemRepository extends MongoRepository<InventoryItem, String> {
}
