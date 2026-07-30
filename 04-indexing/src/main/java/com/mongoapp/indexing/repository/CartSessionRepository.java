package com.mongoapp.indexing.repository;

import com.mongoapp.indexing.model.CartSession;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CartSessionRepository extends MongoRepository<CartSession, String> {
}
