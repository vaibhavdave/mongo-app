package com.mongoapp.capstone.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Embedded snapshot (module 03) - productName/unitPrice captured at order time, not looked up live. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private String productId;
    private String productName;
    private double unitPrice;
    private int quantity;
}
