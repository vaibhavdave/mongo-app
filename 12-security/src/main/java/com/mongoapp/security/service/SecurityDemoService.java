package com.mongoapp.security.service;

import com.mongodb.MongoCommandException;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SecurityDemoService {

    private final MongoTemplate mongoTemplate;

    public SecurityDemoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * connectionStatus is a command any authenticated user can run
     * against themselves - it reports back exactly what MongoDB thinks
     * this connection is authorized to do. Handy for literally showing
     * "here's what app_user's role actually grants", straight from the
     * server, rather than trusting documentation or memory.
     */
    public Map<String, Object> whoAmI() {
        Document status = mongoTemplate.executeCommand(new Document("connectionStatus", 1).append("showPrivileges", true));
        Document authInfo = (Document) status.get("authInfo");

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("authenticatedUsers", authInfo.get("authenticatedUserRoles"));
        summary.put("raw", authInfo.toJson());
        return summary;
    }

    /**
     * app_user's role (see mongo-init/init-app-user.js) only grants
     * readWrite on security_db - nothing at the admin/server level. This
     * deliberately runs listCollections against the "admin" database
     * (not security_db) to trigger an authorization failure and show
     * exactly what MongoDB says when a user tries something their role
     * doesn't cover, instead of just asserting it in prose.
     */
    public String attemptCrossDatabaseAccess() {
        try {
            List<Document> collections = mongoTemplate.getMongoDatabaseFactory()
                    .getMongoDatabase("admin")
                    .listCollections()
                    .into(new java.util.ArrayList<>());
            return "Unexpectedly succeeded - got " + collections.size() + " collections. "
                    + "Check that app_user's role in mongo-init really is scoped to security_db only.";
        } catch (MongoCommandException ex) {
            return "Access denied as expected: " + ex.getErrorMessage();
        }
    }
}
