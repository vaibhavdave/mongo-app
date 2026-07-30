package com.mongoapp.aggregation.dto;

public record CategorySales(String category, int totalQuantity, double totalRevenue, int lineItemCount) {
}
