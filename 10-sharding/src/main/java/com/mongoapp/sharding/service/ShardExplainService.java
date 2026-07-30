package com.mongoapp.sharding.service;

import com.mongodb.ExplainVerbosity;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
public class ShardExplainService {

    private final MongoTemplate mongoTemplate;

    public ShardExplainService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Filtering on the shard key (deviceId) lets mongos calculate exactly
     * which single shard owns matching documents and route the query
     * there directly - a "targeted" query. Against docker-compose.yml
     * (plain mongod, not actually sharded) this still runs fine, it's
     * just that there's only ever one "shard" to talk to.
     */
    public Map<String, Object> explainTargetedQuery(String deviceId) {
        return explain(query(where("deviceId").is(deviceId)));
    }

    /**
     * Filtering on eventType - not the shard key - means mongos has no
     * way to know which shard(s) might have a match, so it must ask
     * *every* shard and merge the results: a "scatter-gather" query. On
     * a real sharded cluster this shows up in the explain output as
     * multiple shards being queried; scatter-gather queries get more
     * expensive as you add more shards, unlike targeted ones.
     */
    public Map<String, Object> explainScatterGatherQuery(String eventType) {
        return explain(query(where("eventType").is(eventType)));
    }

    private Map<String, Object> explain(Query mongoQuery) {
        MongoCollection<Document> collection = mongoTemplate.getCollection("events");
        Document explainResult = collection.find(mongoQuery.getQueryObject())
                .explain(ExplainVerbosity.EXECUTION_STATS);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("shardsQueried", extractShardNames(explainResult));
        summary.put("rawExplain", explainResult.toJson());
        return summary;
    }

    /**
     * Best-effort extraction: a sharded explain's shape (where per-shard
     * plan info lives) has shifted across MongoDB versions, and this
     * module's docker-compose.yml (plain mongod) won't have shard info at
     * all. Rather than risk a brittle, version-specific parser, this
     * pulls out shard names if it recognizes the shape and otherwise
     * says so plainly - the full rawExplain field above always has the
     * ground truth regardless.
     */
    @SuppressWarnings("unchecked")
    private List<String> extractShardNames(Document explainResult) {
        Object queryPlanner = explainResult.get("queryPlanner");
        if (queryPlanner instanceof Document qp) {
            Object winningPlan = qp.get("winningPlan");
            if (winningPlan instanceof Document wp && wp.get("shards") instanceof List<?> shards) {
                return shards.stream()
                        .filter(Document.class::isInstance)
                        .map(Document.class::cast)
                        .map(d -> d.getString("shardName"))
                        .filter(name -> name != null)
                        .toList();
            }
        }
        return List.of("(not running against a sharded cluster, or MongoDB version reports shard info differently - see rawExplain)");
    }
}
