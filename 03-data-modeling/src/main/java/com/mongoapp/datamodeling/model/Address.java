package com.mongoapp.datamodeling.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * No @Document, no @Id: this is an embedded value object, not a
 * standalone entity. It only ever exists inside a Customer document -
 * there's no "addresses" collection. A good candidate for embedding: a
 * small, bounded list that's always read/written together with its
 * parent and has no independent lifecycle.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String type; // HOME, BILLING, SHIPPING
    private String street;
    private String city;
    private String state;
    private String zip;
}
