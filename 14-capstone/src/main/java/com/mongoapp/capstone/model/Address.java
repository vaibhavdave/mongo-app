package com.mongoapp.capstone.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Embedded value object (module 03) - no @Document, lives only inside Customer. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String type;
    private String street;
    private String city;
    private String state;
    private String zip;
}
