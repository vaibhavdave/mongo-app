package com.mongoapp.capstone.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class OrderItemRequest {

    @NotBlank
    private String productId;

    @Positive
    private int quantity;
}
