package com.mongoapp.capstone.service;

import com.mongoapp.capstone.model.Product;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/** Dynamic Criteria search + pagination (module 02) and text search (module 04/08), over the capstone catalog. */
@Service
public class ProductQueryService {

    private final MongoTemplate mongoTemplate;

    public ProductQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<Product> search(String category, Double maxPrice, int page, int size) {
        List<Criteria> criteria = new ArrayList<>();
        if (category != null && !category.isBlank()) {
            criteria.add(where("category").is(category));
        }
        if (maxPrice != null) {
            criteria.add(where("price").lte(maxPrice));
        }

        Criteria combined = criteria.isEmpty() ? new Criteria() : new Criteria().andOperator(criteria.toArray(new Criteria[0]));
        Query pageQuery = query(combined)
                .with(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")));
        return mongoTemplate.find(pageQuery, Product.class);
    }

    public List<Product> textSearch(String search) {
        TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(search.split("\\s+"));
        return mongoTemplate.find(TextQuery.queryText(criteria).sortByScore(), Product.class);
    }
}
