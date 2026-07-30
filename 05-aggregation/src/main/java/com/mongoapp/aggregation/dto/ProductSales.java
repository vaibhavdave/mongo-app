package com.mongoapp.aggregation.dto;

public record ProductSales(String productId, String productName, int totalQuantity, double totalRevenue) {
}
