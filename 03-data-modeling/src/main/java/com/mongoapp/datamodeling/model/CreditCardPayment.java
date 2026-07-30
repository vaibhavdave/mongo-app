package com.mongoapp.datamodeling.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("credit_card")
public class CreditCardPayment extends Payment {

    private String brand;
    private String cardLast4;

    @JsonCreator
    public CreditCardPayment(@JsonProperty("amount") double amount,
                              @JsonProperty("brand") String brand,
                              @JsonProperty("cardLast4") String cardLast4) {
        super("credit_card");
        setAmount(amount);
        this.brand = brand;
        this.cardLast4 = cardLast4;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
    }
}
