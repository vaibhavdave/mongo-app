package com.mongoapp.querying.repository;

import com.mongoapp.querying.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Only used here for the basic save/list/delete operations needed to seed
 * data through the UI. The interesting queries in this module are built
 * with MongoTemplate in ProductQueryService - see that class for why.
 */
public interface ProductRepository extends MongoRepository<Product, String> {
}
