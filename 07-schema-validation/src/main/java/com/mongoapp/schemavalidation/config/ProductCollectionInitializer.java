package com.mongoapp.schemavalidation.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.stereotype.Component;

import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;

/**
 * Creates the "products" collection up front with a $jsonSchema validator
 * attached, before any document is ever written to it. MongoDB then
 * enforces this schema on every insert/update against the collection -
 * from this app, from Mongo Express, from the shell, from anywhere -
 * which is exactly what application-level Bean Validation (see Product)
 * cannot do.
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
                        JsonSchemaProperty.object("price").properties(
                                JsonSchemaProperty.required(JsonSchemaProperty.decimal128("amount")),
                                JsonSchemaProperty.required(JsonSchemaProperty.string("currency")
                                        .possibleValues("USD", "EUR", "GBP"))
                        ),
                        // Restricting category to a fixed set is something the
                        // Java-side Bean Validation on Product deliberately does
                        // NOT do - a concrete example of the DB schema covering
                        // ground the application layer doesn't.
                        JsonSchemaProperty.string("category")
                                .possibleValues("electronics", "books", "home", "toys")
                )
                .build();

        CollectionOptions options = CollectionOptions.empty()
                .schema(schema)
                .schemaValidationAction(ValidationAction.ERROR) // reject invalid writes outright (vs WARN, which only logs)
                .schemaValidationLevel(ValidationLevel.STRICT);  // apply to inserts and updates alike

        mongoTemplate.createCollection("products", options);
    }
}
