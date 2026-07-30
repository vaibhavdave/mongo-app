package com.mongoapp.capstone.service;

import com.mongoapp.capstone.dto.ProductSales;
import com.mongoapp.capstone.dto.StatusSales;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

/** Aggregation pipelines (module 05) over the capstone's orders collection. */
@Service
public class ReportService {

    private final MongoTemplate mongoTemplate;

    public ReportService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    private static AggregationExpression lineRevenue() {
        return ArithmeticOperators.Multiply.valueOf("items.unitPrice").multiplyBy("items.quantity");
    }

    public List<ProductSales> topProducts(int limit) {
        var agg = newAggregation(
                unwind("items"),
                group("items.productId", "items.productName")
                        .sum("items.quantity").as("totalQuantity")
                        .sum(lineRevenue()).as("totalRevenue"),
                project("totalQuantity", "totalRevenue")
                        .and("_id.productId").as("productId")
                        .and("_id.productName").as("productName")
                        .andExclude("_id"),
                sort(Sort.Direction.DESC, "totalRevenue"),
                limit(limit)
        );
        return mongoTemplate.aggregate(agg, "orders", ProductSales.class).getMappedResults();
    }

    public List<StatusSales> revenueByStatus() {
        var agg = newAggregation(
                group("status")
                        .sum("totalAmount").as("totalRevenue")
                        .count().as("orderCount"),
                project("totalRevenue", "orderCount").and("_id").as("status").andExclude("_id"),
                sort(Sort.Direction.DESC, "totalRevenue")
        );
        return mongoTemplate.aggregate(agg, "orders", StatusSales.class).getMappedResults();
    }
}
