package com.mongoapp.capstone;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Same pattern as module 13: a throwaway MongoDB per test run, via
 * Testcontainers. MongoDBContainer sets up a single-node replica set
 * automatically, which is exactly what this app's transactions (order
 * placement) and change stream (live order feed) need to function.
 */
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        // getReplicaSetUrl() already returns a complete connection string
        // with the replicaSet parameter Testcontainers configured -
        // no need to (and don't) append one manually.
        registry.add("spring.data.mongodb.uri", () -> MONGO_DB_CONTAINER.getReplicaSetUrl("capstone_db"));
    }
}
