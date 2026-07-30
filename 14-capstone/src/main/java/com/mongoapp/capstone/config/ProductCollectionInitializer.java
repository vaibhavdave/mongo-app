package com.mongoapp.capstone.config;

import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.stereotype.Component;

/**
 * Same idea as module 07: enforce a minimum shape on "products" at the
 * database level, not just via this app's Bean Validation, so no client
 * (this app, a script, Mongo Express) can write a product missing its
 * core fields.
 */
@Component
public class ProductCollectionInitializer implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    public ProductCollectionInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (mongoTemplate.collectionExists("products")) {
            return;
        }

        MongoJsonSchema schema = MongoJsonSchema.builder()
                .required("sku", "name", "price", "category")
                .properties(
                        JsonSchemaProperty.string("sku"),
                        JsonSchemaProperty.string("name"),
                        JsonSchemaProperty.number("price"),
                        JsonSchemaProperty.string("category")
                )
                .build();

        CollectionOptions options = CollectionOptions.empty()
                .schema(schema)
                .schemaValidationAction(ValidationAction.ERROR)
                .schemaValidationLevel(ValidationLevel.STRICT);

        mongoTemplate.createCollection("products", options);
    }
}
