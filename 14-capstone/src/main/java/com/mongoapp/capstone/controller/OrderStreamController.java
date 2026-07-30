package com.mongoapp.capstone.controller;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongoapp.capstone.model.Order;
import jakarta.annotation.PreDestroy;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Change streams (module 11), adapted to this app's imperative (non-
 * WebFlux) stack: module 11 used ReactiveMongoTemplate's fluent
 * .changeStream() API, which needs a reactive Flux to work with. Here,
 * there's no Flux - so this drops to the sync MongoDB driver's own
 * MongoCollection.watch(), which returns a blocking cursor, and pumps it
 * into an SseEmitter from a dedicated background thread per subscriber.
 *
 * Simplified for learning purposes: if a client disconnects while the
 * cursor is blocked waiting for the *next* change (i.e. no new orders
 * are coming in), that thread and cursor won't be cleaned up until the
 * next change event does arrive. A production version would need an
 * explicit cancellation signal (e.g. closing the cursor from the
 * emitter's onCompletion/onTimeout callbacks) to avoid that.
 */
@RestController
public class OrderStreamController {

    private final MongoTemplate mongoTemplate;
    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "order-change-stream");
        thread.setDaemon(true);
        return thread;
    });

    public OrderStreamController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/api/orders/stream")
    public SseEmitter streamOrders() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        executor.submit(() -> watchOrders(emitter));
        return emitter;
    }

    private void watchOrders(SseEmitter emitter) {
        MongoCollection<Document> collection = mongoTemplate.getCollection("orders");
        try (MongoCursor<ChangeStreamDocument<Document>> cursor =
                     collection.watch().fullDocument(FullDocument.UPDATE_LOOKUP).iterator()) {
            while (cursor.hasNext()) {
                ChangeStreamDocument<Document> change = cursor.next();

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("operationType", change.getOperationType().getValue());

                Document fullDocument = change.getFullDocument();
                if (fullDocument != null) {
                    // Reuse Spring Data's own converter so the SSE payload is a
                    // normal, Jackson-friendly Order (String id, etc.) rather
                    // than a raw BSON Document full of ObjectId/Decimal128
                    // values Jackson doesn't know how to serialize by default.
                    Order order = mongoTemplate.getConverter().read(Order.class, fullDocument);
                    payload.put("order", order);
                }

                emitter.send(SseEmitter.event().data(payload));
            }
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
