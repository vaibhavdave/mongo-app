package com.mongoapp.replication.service;

import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReplicaSetService {

    private final MongoTemplate mongoTemplate;

    public ReplicaSetService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Spring Data repositories always read from whatever the driver
     * currently considers primary. To actually route a read to a
     * secondary, you drop to the driver's MongoDatabase/MongoCollection
     * API and set a ReadPreference explicitly - there's no per-repository-
     * query equivalent. secondaryPreferred here means "use a secondary if
     * one is available and healthy, otherwise fall back to primary".
     */
    public List<Document> findMessages(String readPreferenceName) {
        ReadPreference readPreference = parseReadPreference(readPreferenceName);
        MongoDatabase db = mongoTemplate.getMongoDatabaseFactory()
                .getMongoDatabase()
                .withReadPreference(readPreference);
        MongoCollection<Document> collection = db.getCollection("messages");
        List<Document> results = new ArrayList<>();
        collection.find().into(results);
        return results;
    }

    private ReadPreference parseReadPreference(String name) {
        return switch (name.toLowerCase()) {
            case "secondary" -> ReadPreference.secondary();
            case "secondarypreferred" -> ReadPreference.secondaryPreferred();
            case "nearest" -> ReadPreference.nearest();
            default -> ReadPreference.primary();
        };
    }

    /**
     * Write concern controls how many replica set members must
     * acknowledge a write before the driver considers it "done":
     * w=1 (default) only waits for the primary - fast, but if the primary
     * crashes before replicating, an already-acknowledged write can be
     * lost. w="majority" waits for a majority of the set to have it
     * durably applied - slower, but survives a single node failure
     * without data loss.
     */
    public Document insertMessage(String author, String text, String writeConcernName) {
        WriteConcern writeConcern = parseWriteConcern(writeConcernName);
        MongoCollection<Document> collection = mongoTemplate.getCollection("messages").withWriteConcern(writeConcern);

        Document doc = new Document()
                .append("author", author)
                .append("text", text)
                .append("createdAt", Instant.now());
        collection.insertOne(doc);
        return doc;
    }

    private WriteConcern parseWriteConcern(String name) {
        return switch (name.toLowerCase()) {
            case "majority" -> WriteConcern.MAJORITY;
            case "2" -> WriteConcern.W2;
            default -> WriteConcern.W1;
        };
    }

    /** Simplified replSetGetStatus - which member is currently primary/secondary, and their health. */
    public List<Map<String, Object>> replicaSetStatus() {
        Document status = mongoTemplate.executeCommand(new Document("replSetGetStatus", 1));
        List<Document> members = status.getList("members", Document.class);

        List<Map<String, Object>> summary = new ArrayList<>();
        for (Document member : members) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", member.getString("name"));
            row.put("stateStr", member.getString("stateStr"));
            row.put("health", member.getDouble("health"));
            summary.add(row);
        }
        return summary;
    }
}
