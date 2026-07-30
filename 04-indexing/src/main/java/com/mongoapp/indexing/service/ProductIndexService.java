package com.mongoapp.indexing.service;

import com.mongodb.ExplainVerbosity;
import com.mongodb.client.MongoCollection;
import com.mongoapp.indexing.model.Product;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
public class ProductIndexService {

    private final MongoTemplate mongoTemplate;

    public ProductIndexService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Filters on category and sorts by price - exactly what
     * category_price_idx (defined on Product) was built for. Expect the
     * winning plan to be an IXSCAN on that index with totalDocsExamined
     * close to nReturned.
     */
    public Map<String, Object> explainCategoryPriceQuery(String category) {
        Query mongoQuery = query(where("category").is(category)).with(Sort.by(Sort.Direction.DESC, "price"));
        return explain(mongoQuery);
    }

    /**
     * A regex search on a field with no index at all. Expect a COLLSCAN
     * (full collection scan) - totalDocsExamined will equal the entire
     * collection size regardless of how few documents actually match.
     * This is the comparison point for the query above.
     */
    public Map<String, Object> explainUnindexedDescriptionSearch(String snippet) {
        Query mongoQuery = query(where("description").regex(snippet, "i"));
        return explain(mongoQuery);
    }

    private Map<String, Object> explain(Query mongoQuery) {
        MongoCollection<Document> collection = mongoTemplate.getCollection("products");
        Document explainResult = collection.find(mongoQuery.getQueryObject())
                .sort(mongoQuery.getSortObject())
                .explain(ExplainVerbosity.EXECUTION_STATS);

        Document executionStats = (Document) explainResult.get("executionStats");
        Document queryPlanner = (Document) explainResult.get("queryPlanner");
        Document winningPlan = (Document) queryPlanner.get("winningPlan");

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("nReturned", executionStats.get("nReturned"));
        summary.put("totalDocsExamined", executionStats.get("totalDocsExamined"));
        summary.put("totalKeysExamined", executionStats.get("totalKeysExamined"));
        summary.put("executionTimeMillis", executionStats.get("executionTimeMillis"));
        summary.put("stages", describeStages(winningPlan));
        summary.put("rawExplain", explainResult.toJson());
        return summary;
    }

    private List<Map<String, Object>> describeStages(Document stage) {
        List<Map<String, Object>> stages = new ArrayList<>();
        Document current = stage;
        while (current != null) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("stage", current.getString("stage"));
            if (current.containsKey("indexName")) {
                s.put("indexName", current.getString("indexName"));
            }
            stages.add(s);
            Object input = current.get("inputStage");
            current = input instanceof Document ? (Document) input : null;
        }
        return stages;
    }

    /** $text search using the compound text index on name+description, sorted by relevance score. */
    public List<Product> textSearch(String search) {
        TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(search.split("\\s+"));
        TextQuery textQuery = TextQuery.queryText(criteria).sortByScore();
        return mongoTemplate.find(textQuery, Product.class);
    }
}
