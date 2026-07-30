package com.mongoapp.testing;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared base for every test that needs a real MongoDB: Testcontainers
 * starts a throwaway "mongo:7.0" container (as a single-node replica set,
 * which MongoDBContainer sets up automatically - so even transaction/
 * change-stream code could be tested against this) once per test class,
 * and @DynamicPropertySource points the app's Mongo connection at it
 * before the Spring context starts. No docker-compose, no manually
 * running Mongo - `mvn test` alone is enough, as long as Docker itself is
 * reachable.
 */
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> MONGO_DB_CONTAINER.getReplicaSetUrl("testing_db"));
    }
}
