package com.mongoapp.querying.service;

import com.mongoapp.querying.dto.PageResponse;
import com.mongoapp.querying.model.Product;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Everything here goes through MongoTemplate instead of derived query
 * methods. You'd reach for this once a query has several *optional*,
 * combinable filters (a derived method per combination doesn't scale),
 * needs pagination/sorting/projections, or needs an update operator that
 * has no equivalent derived-method syntax (e.g. atomic conditional
 * decrement).
 */
@Service
public class ProductQueryService {

    private final MongoTemplate mongoTemplate;

    public ProductQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Builds a Criteria filter from whichever parameters are non-null,
     * combined with andOperator(). This is the pattern derived query
     * methods can't express: an arbitrary, runtime-decided combination of
     * filters in one query.
     */
    public PageResponse<Product> search(String category, Double minPrice, Double maxPrice, String tag,
                                         Boolean inStock, int page, int size, String sortBy, String sortDir) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (category != null) {
            criteriaList.add(where("category").is(category));
        }
        if (minPrice != null) {
            criteriaList.add(where("price").gte(minPrice));
        }
        if (maxPrice != null) {
            criteriaList.add(where("price").lte(maxPrice));
        }
        if (tag != null) {
            criteriaList.add(where("tags").in(tag));
        }
        if (inStock != null) {
            criteriaList.add(where("inStock").is(inStock));
        }

        Criteria criteria = criteriaList.isEmpty()
                ? new Criteria()
                : new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));

        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy != null ? sortBy : "name");

        Query pageQuery = query(criteria).with(PageRequest.of(page, size, sort));
        List<Product> content = mongoTemplate.find(pageQuery, Product.class);
        long total = mongoTemplate.count(query(criteria), Product.class);

        return new PageResponse<>(content, page, size, total, (int) Math.ceil((double) total / size));
    }

    /**
     * A projection: Query.fields().include(...) tells MongoDB to return
     * only those fields (plus _id) over the wire, rather than the full
     * document. Useful once documents get large and you only need a
     * couple of fields client-side.
     */
    public List<Map> nameAndPriceOnly() {
        Query projection = new Query();
        projection.fields().include("name").include("price");
        return mongoTemplate.find(projection, Map.class, "products");
    }

    /** update.inc() maps to Mongo's $inc - adjusts a numeric field in place, atomically. */
    public Product adjustStock(String id, int delta) {
        Update update = new Update().inc("stock", delta);
        return mongoTemplate.findAndModify(
                query(where("id").is(id)),
                update,
                FindAndModifyOptions.options().returnNew(true),
                Product.class);
    }

    /** update.addToSet() maps to $addToSet - adds to an array only if not already present. */
    public Product addTag(String id, String tag) {
        Update update = new Update().addToSet("tags", tag);
        return mongoTemplate.findAndModify(
                query(where("id").is(id)),
                update,
                FindAndModifyOptions.options().returnNew(true),
                Product.class);
    }

    /**
     * Atomic "purchase": the filter (stock $gte quantity) and the update
     * ($inc stock by -quantity) happen as a single server-side operation,
     * so concurrent purchases can't both succeed against insufficient
     * stock (no read-then-write race). Returns null if the filter didn't
     * match anything (i.e. not enough stock).
     */
    public Product purchase(String id, int quantity) {
        Query condition = query(where("id").is(id).and("stock").gte(quantity));
        Update update = new Update().inc("stock", -quantity);
        Product result = mongoTemplate.findAndModify(
                condition, update, FindAndModifyOptions.options().returnNew(true), Product.class);

        if (result != null && result.getStock() == 0) {
            markInStock(result.getId(), false);
        }
        return result;
    }

    private void markInStock(String id, boolean inStock) {
        mongoTemplate.updateFirst(query(where("id").is(id)), new Update().set("inStock", inStock), Product.class);
    }

    /**
     * updateMulti() maps to Mongo's updateMany - applies the update to
     * every document matching the filter, not just the first one.
     */
    public long markCategoryOutOfStock(String category) {
        Update update = new Update().set("inStock", false);
        return mongoTemplate.updateMulti(query(where("category").is(category)), update, Product.class)
                .getModifiedCount();
    }
}
