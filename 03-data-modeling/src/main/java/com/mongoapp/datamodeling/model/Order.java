package com.mongoapp.datamodeling.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "orders")
@Data
@NoArgsConstructor
public class Order {

    @Id
    private String id;

    /**
     * Manual reference: just the customer's id, stored as a plain string.
     * This is the pattern MongoDB's own docs recommend for most
     * one-to-many/many-to-one relationships. Resolving it is one explicit
     * query in your own code (see OrderController.getCustomer) - you
     * control exactly when it happens.
     */
    private String customerId;

    /**
     * @DBRef reference, kept side-by-side purely for comparison. It
     * stores {"$ref": "customers", "$id": ...} instead of a plain id, and
     * - by default - Spring Data resolves it *eagerly*: loading N orders
     * triggers N extra queries, one per order, to fetch each referenced
     * customer, even if you never touch the field. That's why manual
     * references (customerId above) are generally preferred: same
     * capability, but the extra query only happens when you explicitly
     * ask for it.
     */
    @DBRef
    private Customer customerRef;

    /** Embedded - see OrderItem for why. */
    private List<OrderItem> items;

    /** Embedded, polymorphic - see Payment for how. */
    private Payment payment;

    private String status;

    private Instant createdAt;
}
