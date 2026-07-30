package com.mongoapp.changestreams.controller;

import com.mongoapp.changestreams.dto.ChangeEventDto;
import com.mongoapp.changestreams.model.Product;
import org.bson.BsonValue;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChangeStreamController {

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public ChangeStreamController(ReactiveMongoTemplate reactiveMongoTemplate) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    /**
     * Opens a MongoDB change stream on the "products" collection and
     * republishes every change as a browser-side Server-Sent Event. No
     * polling involved on either side: MongoDB pushes the change to this
     * app the moment it happens (via the oplog), and this app pushes it
     * to every connected browser the moment it arrives. Try this in two
     * browser tabs and create a product from a third source (curl, Mongo
     * Express) - both tabs update immediately.
     */
    @GetMapping(path = "/api/products/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChangeEventDto>> streamProductChanges() {
        return reactiveMongoTemplate.changeStream(Product.class)
                .watchCollection("products")
                .listen()
                .map(this::toServerSentEvent);
    }

    private ServerSentEvent<ChangeEventDto> toServerSentEvent(ChangeStreamEvent<Product> event) {
        String operationType = event.getOperationType() != null ? event.getOperationType().getValue() : "unknown";
        Product product = event.getBody();

        String productId = product != null ? product.getId() : extractIdFromDocumentKey(event);

        return ServerSentEvent.builder(new ChangeEventDto(operationType, productId, product)).build();
    }

    /**
     * On a delete, getBody() is null (the document is already gone by
     * the time the event arrives) - the id has to come from the raw
     * change event's documentKey instead.
     */
    private String extractIdFromDocumentKey(ChangeStreamEvent<Product> event) {
        if (event.getRaw() == null || event.getRaw().getDocumentKey() == null) {
            return null;
        }
        BsonValue idValue = event.getRaw().getDocumentKey().get("_id");
        if (idValue == null) {
            return null;
        }
        return idValue.isObjectId() ? idValue.asObjectId().getValue().toHexString() : idValue.toString();
    }
}
