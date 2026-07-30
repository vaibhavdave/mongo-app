package com.mongoapp.capstone.exception;

/**
 * Thrown mid-order when an item's atomic conditional decrement (module
 * 02's findAndModify technique) fails because stock is too low. Thrown
 * from inside an @Transactional method (OrderService.placeOrder), so
 * Spring rolls back every stock decrement already applied earlier in the
 * same order too - see module 06 for why that matters.
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
