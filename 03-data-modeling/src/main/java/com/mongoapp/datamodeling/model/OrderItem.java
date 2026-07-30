package com.mongoapp.datamodeling.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embedded, like Address - an order's line items live and die with the
 * order. Note productName/unitPrice are *snapshots* copied in at order
 * time, deliberately duplicating data that also lives in a Product
 * document elsewhere. That's intentional: if the product's price or name
 * changes later, this historical order should still show what the
 * customer actually paid, not today's price. Denormalizing for read
 * correctness/performance is a normal MongoDB modeling choice, not a bug.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private String productId;
    private String productName;
    private double unitPrice;
    private int quantity;
}
