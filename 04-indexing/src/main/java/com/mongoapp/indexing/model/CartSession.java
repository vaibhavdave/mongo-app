package com.mongoapp.indexing.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * A TTL (time-to-live) index automatically deletes documents once a date
 * field is in the past - useful for anything with a natural expiry: cart
 * sessions, OTP codes, temporary tokens. expireAfterSeconds = 0 combined
 * with storing the *actual future expiry timestamp* in the field (rather
 * than the creation time) is the flexible pattern: each document can have
 * its own expiry, not just a fixed age.
 *
 * MongoDB's TTL background monitor only runs about once a minute, so
 * expect deletion up to ~60s after expiresAt has passed - it's not
 * instantaneous.
 */
@Document(collection = "cart_sessions")
@Data
@NoArgsConstructor
public class CartSession {

    @Id
    private String id;

    private String owner;

    private List<String> productIds;

    @Indexed(name = "expiresAt_ttl", expireAfterSeconds = 0)
    private Instant expiresAt;

    private Instant createdAt;
}
