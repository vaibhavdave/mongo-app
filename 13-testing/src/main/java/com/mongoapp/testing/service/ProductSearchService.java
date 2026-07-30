package com.mongoapp.testing.service;

import com.mongoapp.testing.model.Product;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
public class ProductSearchService {

    private final MongoTemplate mongoTemplate;

    public ProductSearchService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /** In-stock products in a category, cheapest first - the method under test in ProductSearchServiceIT. */
    public List<Product> findInStockByCategorySortedByPrice(String category) {
        Query queryObj = query(where("category").is(category).and("stock").gt(0))
                .with(Sort.by(Sort.Direction.ASC, "price"));
        return mongoTemplate.find(queryObj, Product.class);
    }
}
