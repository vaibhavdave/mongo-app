package com.mongoapp.datamodeling.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Polymorphic embedded document: Order.payment can hold either a
 * CreditCardPayment or a PaypalPayment. Two separate mechanisms are at
 * play here, doing similar jobs on two different layers:
 *
 *  - Jackson (@JsonTypeInfo/@JsonSubTypes below) picks the right subclass
 *    when deserializing an incoming REST request body, using the "type"
 *    field already present in the JSON.
 *
 *  - Spring Data MongoDB does the same thing independently for BSON: it
 *    writes a "_class" field into the stored document recording the
 *    concrete subtype, and uses it to pick the right subclass when
 *    reading the document back. @TypeAlias on each subclass (see
 *    CreditCardPayment/PaypalPayment) shortens that stored value from the
 *    full class name to something readable in Mongo Express.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreditCardPayment.class, name = "credit_card"),
        @JsonSubTypes.Type(value = PaypalPayment.class, name = "paypal")
})
public abstract class Payment {

    private double amount;
    private final String type;

    protected Payment(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }
}
