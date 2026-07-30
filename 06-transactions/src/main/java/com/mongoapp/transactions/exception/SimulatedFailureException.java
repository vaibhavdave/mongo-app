package com.mongoapp.transactions.exception;

/** Thrown deliberately mid-transfer to demonstrate rollback behavior. */
public class SimulatedFailureException extends RuntimeException {
    public SimulatedFailureException(String message) {
        super(message);
    }
}
