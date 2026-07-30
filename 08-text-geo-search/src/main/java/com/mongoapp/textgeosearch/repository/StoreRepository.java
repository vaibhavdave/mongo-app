package com.mongoapp.textgeosearch.repository;

import com.mongoapp.textgeosearch.model.Store;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StoreRepository extends MongoRepository<Store, String> {
}
