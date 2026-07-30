package com.mongoapp.datamodeling.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("paypal")
public class PaypalPayment extends Payment {

    private String payerEmail;

    @JsonCreator
    public PaypalPayment(@JsonProperty("amount") double amount,
                          @JsonProperty("payerEmail") String payerEmail) {
        super("paypal");
        setAmount(amount);
        this.payerEmail = payerEmail;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }
}
