package com.mongoapp.foundations.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String id) {
        super("No product found with id " + id);
    }
}
