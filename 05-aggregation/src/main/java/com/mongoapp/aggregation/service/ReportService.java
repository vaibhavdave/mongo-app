package com.mongoapp.aggregation.service;

import com.mongoapp.aggregation.dto.CategorySales;
import com.mongoapp.aggregation.dto.OrderWithCustomer;
import com.mongoapp.aggregation.dto.ProductSales;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

/**
 * Every report here is one aggregation pipeline: a sequence of stages,
 * each transforming the documents flowing through it, all executed
 * server-side in a single round trip. Compare that to pulling every order
 * into the JVM and summing things up in a loop - the aggregation runs
 * next to the data, only ships back the (much smaller) result, and can
 * use indexes for its $match/$sort stages.
 */
@Service
public class ReportService {

    private final MongoTemplate mongoTemplate;

    public ReportService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    private static AggregationExpression lineRevenue() {
        return ArithmeticOperators.Multiply.valueOf("items.unitPrice").multiplyBy("items.quantity");
    }

    /** $unwind items -> $group by category, summing revenue/quantity -> $sort by revenue. */
    public List<CategorySales> salesByCategory() {
        Aggregation agg = newAggregation(
                unwind("items"),
                group("items.category")
                        .sum("items.quantity").as("totalQuantity")
                        .sum(lineRevenue()).as("totalRevenue")
                        .count().as("lineItemCount"),
                project("totalQuantity", "totalRevenue", "lineItemCount")
                        .and("_id").as("category")
                        .andExclude("_id"),
                sort(Sort.Direction.DESC, "totalRevenue")
        );
        return mongoTemplate.aggregate(agg, "orders", CategorySales.class).getMappedResults();
    }

    /** Same shape as above but grouped by product, then $limit to the top N. */
    public List<ProductSales> topProducts(int limit) {
        Aggregation agg = newAggregation(
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

    /**
     * $lookup joins orders to customers - a "join" done inside the
     * database instead of N extra application-level queries. The tricky
     * part: Order.customerId is stored as a plain string, but
     * Customer._id is stored as an ObjectId (Spring Data auto-converts
     * @Id String fields to ObjectId). $lookup needs matching types on
     * both sides, so we convert with $toObjectId in an $addFields stage
     * first - a very common real-world gotcha the first time you write a
     * raw aggregation against Spring Data-managed collections.
     */
    public List<OrderWithCustomer> ordersWithCustomer() {
        Aggregation agg = newAggregation(
                addFields().addField("customerObjId")
                        .withValue(ConvertOperators.ToObjectId.toObjectId("$customerId"))
                        .build(),
                lookup("customers", "customerObjId", "_id", "customerInfo"),
                unwind("customerInfo", true),
                project("status", "createdAt", "items")
                        .and("_id").as("orderId")
                        .and("customerInfo.name").as("customerName")
                        .and("customerInfo.email").as("customerEmail")
        );
        return mongoTemplate.aggregate(agg, "orders", OrderWithCustomer.class).getMappedResults();
    }

    /** $bucket groups line items into price ranges - a histogram computed server-side. */
    public List<Document> priceBuckets() {
        Aggregation agg = newAggregation(
                unwind("items"),
                bucket("items.unitPrice")
                        .withBoundaries(0, 25, 50, 100, 250)
                        .withDefaultBucket("250+")
                        .andOutputCount().as("lineItemCount")
                        .andOutput(lineRevenue()).sum().as("totalRevenue")
        );
        return mongoTemplate.aggregate(agg, "orders", Document.class).getMappedResults();
    }

    /**
     * $facet runs several independent sub-pipelines against the same
     * input in a single aggregate() call - one dashboard, one round trip,
     * instead of three separate report requests.
     */
    public Document dashboard() {
        FacetOperation facet = facet(
                unwind("items"),
                group("items.category").sum("items.quantity").as("totalQuantity").sum(lineRevenue()).as("totalRevenue"),
                sort(Sort.Direction.DESC, "totalRevenue")
        ).as("byCategory")
                .and(count().as("totalOrders")).as("summary")
                .and(unwind("items"), sort(Sort.Direction.DESC, "items.unitPrice"), limit(3)).as("mostExpensiveLineItems");

        Aggregation agg = newAggregation(facet);
        return mongoTemplate.aggregate(agg, "orders", Document.class).getUniqueMappedResult();
    }
}
