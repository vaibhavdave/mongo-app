package com.mongoapp.schemavalidation.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Deliberately not a `double`. Money as a binary float rounds - 19.99
 * stored as a double and read back can come out as 19.990000000000002.
 * BigDecimal in Java, stored as BSON Decimal128 (a proper base-10
 * floating point type), avoids that entirely. See MoneyWritingConverter/
 * MoneyReadingConverter for how the BigDecimal <-> Decimal128 conversion
 * actually happens.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Money {

    @Positive
    private BigDecimal amount;

    @NotBlank
    private String currency;
}
