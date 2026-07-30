package com.mongoapp.datamodeling.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {

    @NotBlank
    private String customerId;

    @NotEmpty
    private List<OrderItem> items;

    private Payment payment;
}
