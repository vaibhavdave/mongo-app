package com.mongoapp.changestreams.dto;

import com.mongoapp.changestreams.model.Product;

/** operationType is one of "insert", "update", "replace", "delete" (the raw driver's OperationType values). */
public record ChangeEventDto(String operationType, String productId, Product product) {
}
